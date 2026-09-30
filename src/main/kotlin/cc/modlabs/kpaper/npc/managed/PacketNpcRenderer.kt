package cc.modlabs.kpaper.npc.managed

import org.bukkit.plugin.java.JavaPlugin
import cc.modlabs.kpaper.skins.SkinEntry
import cc.modlabs.kpaper.packets.PacketInterceptor
import cc.modlabs.kpaper.packets.injectPacketInterceptor
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.InteractionHand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap

class PacketNpcRenderer(
    private val plugin: JavaPlugin,
    private val interact: (Player, String) -> Unit,
    approach: (Player, String) -> Unit,
    private val hooks: NpcHooks,
) : Listener {
    private val rendered = ConcurrentHashMap<String, PacketNpc>()
    private val entityIds = ConcurrentHashMap<Int, String>()
    private val factory = PacketNpcFactory(plugin, hooks)
    private val packets = NpcPacketSender(plugin, hooks)
    private val visibility = NpcVisibilityRuntime(hooks, rendered, approach, packets)
    private var callbackId: Int? = null
    private var task: BukkitTask? = null

    fun start() {
        if (task != null) return
        callbackId = PacketInterceptor.registerPacketCallback(ServerboundInteractPacket::class.java) { player, packet ->
            if (packet.hand() != InteractionHand.MAIN_HAND) return@registerPacketCallback
            val id = entityIds[packet.entityId()]
                ?: return@registerPacketCallback
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (player.isOnline && visibility.isVisible(player, id)) interact(player, id)
            })
        }
        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getOnlinePlayers().forEach { it.injectPacketInterceptor() }
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable(visibility::sync), 1L, 10L)
    }

    fun stop() {
        callbackId?.let(PacketInterceptor::unregisterPacketCallback)
        callbackId = null
        HandlerList.unregisterAll(this)
        task?.cancel()
        task = null
        visibility.clear()
        packets.clear()
        rendered.clear()
        entityIds.clear()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) = event.player.injectPacketInterceptor()

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) = visibility.reset(event.player)

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) = visibility.reset(event.player)

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) = visibility.reset(event.player)

    fun put(definition: NpcDefinition, skin: SkinEntry?) {
        put(definition.id, prepare(definition, skin))
    }

    internal fun prepare(definition: NpcDefinition, skin: SkinEntry?) = factory.create(definition, skin)

    internal fun put(id: String, npc: PacketNpc) {
        remove(id)
        rendered[id] = npc
        entityIds[npc.entityId] = id
    }

    fun remove(id: String) {
        val npc = rendered.remove(id) ?: return
        entityIds.remove(npc.entityId)
        visibility.remove(id, npc)
    }

    @Suppress("UNUSED_PARAMETER")
    fun move(id: String, location: Location, moving: Boolean) {
        rendered[id]?.let { visibility.move(id, it, location) }
    }

    fun location(id: String) = visibility.location(id)
    fun swing(id: String, offHand: Boolean) = visibility.swing(id, offHand)
    fun headLook(id: String, yaw: Float) = visibility.headLook(id, yaw)
    fun headPitch(id: String, offset: Float) = visibility.headPitch(id, offset)
    fun pose(id: String, pose: NpcPose) = visibility.pose(id, pose)
}
