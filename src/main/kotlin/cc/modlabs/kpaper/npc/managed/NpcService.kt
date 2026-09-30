package cc.modlabs.kpaper.npc.managed

import cc.modlabs.kpaper.main.PacketEventsSupport
import cc.modlabs.kpaper.skins.SkinLibraryService
import cc.modlabs.kpaper.world.toStringLocation
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/** Persistent Bedrockia-style packet NPCs. All NPC operations run on the server thread. */
class NpcService(
    private val plugin: JavaPlugin,
    val skins: SkinLibraryService,
    val hooks: NpcHooks = NpcHooks(),
    /** False when the consuming plugin owns persistence (for example a database). */
    private val persistDefinitions: Boolean,
) : Listener {
    constructor(plugin: JavaPlugin, skins: SkinLibraryService, hooks: NpcHooks = NpcHooks()) :
        this(plugin, skins, hooks, true)

    private val storage = NpcStorage(plugin)
    private var definitions = linkedMapOf<String, NpcDefinition>()
    private val actions = linkedMapOf<String, NpcAction>()
    private val traitKeys = linkedSetOf<String>()
    private var running = false
    private val renderer: PacketNpcRenderer = PacketNpcRenderer(plugin,
        { player, id -> interactions.interact(player, id, NpcDialogTrigger.CLICK) },
        { player, id -> interactions.interact(player, id, NpcDialogTrigger.PROXIMITY) }, hooks)
    private val movement = NpcMovementController(plugin, renderer, { definitions[it] }, hooks)
    private val interactions: NpcInteractionRuntime = NpcInteractionRuntime(plugin, renderer, { definitions[it] }, { actions[it] })

    fun start() {
        serverThread()
        check(PacketEventsSupport.isActive()) { "Managed NPCs require enablePacketEventsFeatures = true" }
        if (running) return
        val loaded = loadDefinitions()
        val prepared = prepare(loaded.values)
        definitions = loaded
        running = true
        skins.onChange = ::refreshSkins
        Bukkit.getPluginManager().registerEvents(this, plugin)
        renderer.start()
        prepared.forEach(renderer::put)
        movement.start(loaded.values.filter { it.location.world != null })
    }

    fun stop() {
        serverThread()
        running = false
        skins.onChange = {}
        interactions.clear()
        movement.stop()
        renderer.stop()
        HandlerList.unregisterAll(this)
        definitions.clear()
        actions.clear()
        traitKeys.clear()
    }

    fun reload() {
        active()
        check(persistDefinitions) { "Replace definitions from the consuming plugin's storage" }
        replaceAll(loadDefinitions().values)
    }

    /** Validate and prepare the entire replacement before changing the active registry. */
    fun replaceAll(npcs: Collection<NpcDefinition>) {
        active()
        val loaded = LinkedHashMap(npcs.map { it.snapshot() }.associateBy { it.id })
        require(loaded.size == npcs.size) { "Duplicate NPC IDs" }
        loaded.values.forEach { require(it.skin == null || skins.resolve(it.skin) != null) { "Unknown skin '${it.skin}'" } }
        val prepared = prepare(loaded.values)
        save(loaded.values)
        interactions.clear()
        movement.stop()
        renderer.stop()
        definitions = loaded
        renderer.start()
        prepared.forEach(renderer::put)
        movement.start(loaded.values.filter { it.location.world != null })
    }

    fun all(): List<NpcDefinition> { serverThread(); return definitions.values.map { it.snapshot() } }
    fun ids(): List<String> { serverThread(); return definitions.keys.sorted() }
    fun get(id: String): NpcDefinition? { serverThread(); return definitions[normalizedNpcId(id)]?.snapshot() }
    fun location(id: String): Location? { serverThread(); return renderer.location(normalizedNpcId(id)) }

    fun create(id: String, location: Location): NpcDefinition {
        active()
        val key = normalizedNpcId(id)
        require(key !in definitions) { "NPC '$key' already exists" }
        return put(NpcDefinition(key, location.toStringLocation()))
    }

    /** Import/upsert a generic definition; no project schema is interpreted automatically. */
    fun put(definition: NpcDefinition): NpcDefinition = put(definition, false)

    fun put(definition: NpcDefinition, resetMovementPosition: Boolean): NpcDefinition {
        active()
        val next = definition.snapshot()
        require(next.skin == null || skins.resolve(next.skin) != null) { "Unknown skin '${next.skin}'" }
        val previous = definitions[next.id]
        val resetPosition = resetMovementPosition || previous?.position != next.position || previous?.movement?.roamingRegionId != next.movement.roamingRegionId
        val renderedDefinition = if (resetPosition) next else renderer.location(next.id)?.let {
            next.copy(position = it.toStringLocation())
        } ?: next
        val packetNpc = prepare(listOf(renderedDefinition))[next.id]
        val updated = LinkedHashMap(definitions + (next.id to next))
        save(updated.values)
        definitions = updated
        interactions.remove(next.id)
        if (packetNpc == null) { renderer.remove(next.id); movement.remove(next.id) }
        else {
            renderer.put(next.id, packetNpc)
            if (resetPosition || previous?.movement != next.movement) movement.update(next, resetPosition)
        }
        return next.snapshot()
    }

    fun update(id: String, transform: (NpcDefinition) -> NpcDefinition): NpcDefinition {
        val current = requireNotNull(get(id)) { "NPC '$id' not found" }
        val next = transform(current)
        require(next.id == current.id) { "NPC ID cannot change during update" }
        return put(next)
    }

    fun delete(id: String): Boolean {
        active()
        val key = normalizedNpcId(id)
        if (key !in definitions) return false
        val updated = LinkedHashMap(definitions - key)
        save(updated.values)
        definitions = updated
        interactions.remove(key)
        movement.remove(key)
        renderer.remove(key)
        return true
    }

    fun setName(id: String, name: String) = update(id) { it.copy(name = validatedNpcName(name)) }
    fun setSkin(id: String, skin: String?) = update(id) { it.copy(skin = skin?.let { key ->
        requireNotNull(skins.resolve(key)) { "Skin '$key' not found" }.id
    }) }
    fun setEquipment(id: String, slot: EquipmentSlot, item: ItemStack?) = update(id) {
        it.copy(equipment = if (item == null || item.type.isAir) it.equipment - slot else it.equipment + (slot to item.clone()))
    }
    fun setSkinLayer(id: String, layer: String, enabled: Boolean) = update(id) {
        it.copy(skinLayers = updatedNpcSkinLayers(it.skinLayers, layer, enabled))
    }
    fun addDialogLine(id: String, line: String) = update(id) { it.copy(dialogLines = appendedNpcDialogLines(it.dialogLines, line)) }
    fun setHome(id: String, location: Location) =
        put(requireNotNull(get(id)) { "NPC '$id' not found" }.copy(position = location.toStringLocation()), true)
    fun addWaypoint(id: String, name: String, location: Location, weight: Int = 1) = update(id) {
        val point = NpcWaypoint(validatedNpcWaypointName(name), location.toStringLocation(), weight)
        it.copy(movement = it.movement.copy(waypoints = it.movement.waypoints.filterNot { old -> old.name == point.name } + point))
    }

    fun registerAction(key: String, action: NpcAction) {
        serverThread()
        validatedNpcKey(key)
        require(key !in actions) { "Action '$key' already registered" }
        actions[key] = action
    }
    fun unregisterAction(key: String) { serverThread(); actions.remove(key) }
    fun actionKeys(): List<String> { serverThread(); return actions.keys.sorted() }
    fun registerTrait(key: String) { serverThread(); traitKeys += validatedNpcKey(key) }
    fun registeredTraits(): List<String> { serverThread(); return (traitKeys + definitions.values.flatMap { it.traits.keys }).sorted() }

    fun refreshSkins() {
        serverThread()
        if (!running) return
        val prepared = prepare(definitions.values.map { npc ->
            renderer.location(npc.id)?.let { npc.copy(position = it.toStringLocation()) } ?: npc
        })
        interactions.clear()
        prepared.forEach(renderer::put)
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        definitions.values.filter { it.position.world == event.world.name }.forEach { npc ->
            prepare(listOf(npc))[npc.id]?.let { renderer.put(npc.id, it); movement.update(npc, true) }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onWorldUnload(event: WorldUnloadEvent) {
        definitions.values.filter { it.position.world == event.world.name }.forEach {
            interactions.remove(it.id); movement.remove(it.id); renderer.remove(it.id)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) = interactions.quit(event.player.uniqueId)

    private fun loadDefinitions(): LinkedHashMap<String, NpcDefinition> {
        val loaded = if (persistDefinitions) storage.load() else emptyList()
        require(loaded.map { it.id }.distinct().size == loaded.size) { "Duplicate NPC IDs" }
        return LinkedHashMap(loaded.associateBy { it.id })
    }

    private fun save(npcs: Collection<NpcDefinition>) {
        if (persistDefinitions) storage.save(npcs)
    }

    private fun prepare(npcs: Collection<NpcDefinition>): Map<String, PacketNpc> = npcs.mapNotNull { npc ->
        if (Bukkit.getWorld(npc.position.world) == null) null
        else npc.id to renderer.prepare(npc, npc.skin?.let(skins::resolve))
    }.toMap()

    private fun active() { serverThread(); check(running) { "NPC service is not running" } }
    private fun serverThread() { check(Bukkit.isPrimaryThread()) { "NPC operations require the server thread" } }
}
