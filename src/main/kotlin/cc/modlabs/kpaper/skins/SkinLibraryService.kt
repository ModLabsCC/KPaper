package cc.modlabs.kpaper.skins

import cc.modlabs.kpaper.inventory.mineskin.MineSkinFetcher
import com.destroystokyo.paper.profile.PlayerProfile
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.CompletableFuture

/** Plugin-owned library. Network imports are asynchronous; a failed save never changes the registry. */
class SkinLibraryService(private val plugin: JavaPlugin) {
    // ponytail: one lock serializes library saves; split writes only if import throughput warrants it.
    private val storage = SkinLibraryStorage(plugin)
    private var entries = linkedMapOf<String, SkinEntry>()
    private var aliases = linkedMapOf<String, String>()
    private var accepting = false
    internal var onChange: () -> Unit = {}

    @Synchronized
    fun start() {
        val loaded = linkedMapOf<String, SkinEntry>()
        val loadedAliases = linkedMapOf<String, String>()
        storage.load(loaded, loadedAliases)
        loadedAliases.keys.forEach { alias ->
            require(canonicalSkinId(alias, loadedAliases) in loaded) { "Invalid or cyclic skin alias '$alias'" }
        }
        entries = loaded
        aliases = loadedAliases
        accepting = true
        changed()
    }

    fun reload() = start()

    @Synchronized
    fun close() { accepting = false }

    @Synchronized
    fun all(): List<SkinEntry> = entries.values.sortedBy { it.id }

    @Synchronized
    fun keys(): List<String> = (entries.keys + aliases.keys).sorted()

    @Synchronized
    fun get(id: String): SkinEntry? = entries[canonicalSkinId(id, aliases)]

    @Synchronized
    fun resolve(idOrName: String): SkinEntry? = get(idOrName) ?: entries.values.firstOrNull {
        it.displayName.equals(idOrName.trim(), true) || it.playerName.equals(idOrName.trim(), true)
    }

    @Synchronized
    fun findMineSkin(id: String): SkinEntry? = entries.values.firstOrNull { it.mineSkinId.equals(id.trim(), true) }

    @Synchronized
    fun put(entry: SkinEntry): SkinEntry {
        require(entry.id !in aliases) { "This ID is a skin alias" }
        require(entries.values.none { it.id != entry.id && it.displayName.equals(entry.displayName, true) }) {
            "A skin with that name already exists"
        }
        commit(entries + (entry.id to entry))
        return entry
    }

    @Synchronized
    fun rename(id: String, displayName: String): SkinEntry =
        put(requireNotNull(resolve(id)) { "Skin not found" }.copy(displayName = validatedSkinDisplayName(displayName)))

    /** Old IDs remain aliases, so saved NPC references continue to resolve. */
    @Synchronized
    fun renameId(id: String, newId: String): SkinEntry {
        val current = requireNotNull(resolve(id)) { "Skin not found" }
        val nextId = validatedSkinId(newId)
        require(nextId !in entries && nextId !in aliases) { "Skin ID already exists" }
        val nextAliases = aliases.mapValues { if (it.value == current.id) nextId else it.value } + (current.id to nextId)
        val renamed = current.copy(id = nextId)
        commit(entries - current.id + (nextId to renamed), nextAliases)
        return renamed
    }

    fun importMineSkin(displayName: String, mineSkinId: String): CompletableFuture<SkinEntry> {
        val name = validatedSkinDisplayName(displayName)
        val id = validatedMineSkinId(mineSkinId)
        synchronized(this) { check(accepting); require(resolve(name) == null) { "Skin already exists" } }
        return CompletableFuture.supplyAsync {
            val fetched = requireNotNull(MineSkinFetcher.fetchSkinSignature(id)) { "MineSkin not found" }
            importMineSkinData(name, id, fetched.texture.data.value, fetched.texture.data.signature)
        }
    }

    @Synchronized
    fun importMineSkinData(displayName: String, mineSkinId: String, texture: String, signature: String): SkinEntry {
        val name = validatedSkinDisplayName(displayName)
        val sourceId = validatedMineSkinId(mineSkinId)
        findMineSkin(sourceId)?.let { return it }
        require(signature.isNotBlank()) { "MineSkin requires a signed texture" }
        return put(SkinEntry(uniqueId(name), texture, signature, name, SkinSource.MINESKIN, sourceId))
    }

    fun getOrImportMineSkin(mineSkinId: String, displayName: String = "MineSkin $mineSkinId"): CompletableFuture<SkinEntry> =
        findMineSkin(mineSkinId)?.let { CompletableFuture.completedFuture(it) } ?: importMineSkin(displayName, mineSkinId)

    fun importPlayerSkin(playerName: String): CompletableFuture<SkinEntry> {
        val name = validatedPlayerName(playerName)
        synchronized(this) { check(accepting) }
        return Bukkit.createProfile(name).update().thenApply { rememberPlayerProfile(name, it) }
    }

    @Synchronized
    fun rememberPlayerProfile(playerName: String, profile: PlayerProfile): SkinEntry {
        val name = validatedPlayerName(playerName)
        val property = requireNotNull(profile.properties.firstOrNull { it.name == "textures" }) { "Profile has no texture" }
        val previous = entries.values.firstOrNull { it.playerName.equals(name, true) }
        val id = previous?.id ?: uniqueId("player_$name")
        return put(SkinEntry(id, property.value, property.signature, previous?.displayName ?: "Player $name",
            SkinSource.PLAYER, playerName = name, createdAt = previous?.createdAt ?: System.currentTimeMillis()))
    }

    @Synchronized
    fun rememberTexture(texture: String, displayName: String = "Custom Head", signature: String? = null): SkinEntry {
        entries.values.firstOrNull { it.texture == texture && it.signature == signature }?.let { return it }
        val baseName = validatedSkinDisplayName(displayName)
        var name = baseName
        var suffix = 2
        while (entries.values.any { it.displayName.equals(name, true) }) name = "${baseName.take(80)} ${suffix++}"
        return put(SkinEntry(uniqueId("texture_${sha256(texture).take(16)}"), texture, signature,
            name, SkinSource.TEXTURE))
    }

    private fun uniqueId(name: String): String {
        val base = skinId(name)
        return generateSequence(base) { previous ->
            val index = previous.removePrefix("${base}_").toIntOrNull() ?: 1
            "${base}_${index + 1}"
        }.first { it !in entries && it !in aliases }
    }

    private fun commit(next: Map<String, SkinEntry>, nextAliases: Map<String, String> = aliases) {
        check(accepting) { "Skin library is closed" }
        storage.save(next, nextAliases)
        entries = LinkedHashMap(next)
        aliases = LinkedHashMap(nextAliases)
        changed()
    }

    private fun changed() {
        if (Bukkit.isPrimaryThread()) notifyChanged()
        else if (plugin.isEnabled) Bukkit.getScheduler().runTask(plugin, Runnable {
            if (accepting) notifyChanged()
        })
    }

    private fun notifyChanged() {
        try { onChange() } catch (failure: Exception) {
            plugin.logger.log(java.util.logging.Level.WARNING, "Skin library saved, but NPC refresh failed", failure)
        }
    }
}
