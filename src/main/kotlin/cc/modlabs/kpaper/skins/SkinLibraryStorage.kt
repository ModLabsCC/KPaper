package cc.modlabs.kpaper.skins

import org.bukkit.plugin.java.JavaPlugin
import cc.modlabs.kpaper.file.readYaml
import cc.modlabs.kpaper.file.saveAtomically
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.Locale

internal class SkinLibraryStorage(private val plugin: JavaPlugin) {
    private val file = File(plugin.dataFolder, "skins.yml")
    private val legacyFile = File(plugin.dataFolder, "npc-skins.yml")

    fun load(entries: MutableMap<String, SkinEntry>, aliases: MutableMap<String, String>) {
        plugin.dataFolder.mkdirs()
        entries.clear()
        aliases.clear()
        plugin.getResource("npc-skins.yml")?.bufferedReader()?.use { reader ->
            loadLegacy(YamlConfiguration.loadConfiguration(reader), SkinSource.BUNDLED, entries)
        }
        if (legacyFile.isFile) loadLegacy(readYaml(legacyFile), SkinSource.LEGACY, entries)
        if (file.isFile) loadLibrary(readYaml(file), entries, aliases)
    }

    fun save(entries: Map<String, SkinEntry>, aliases: Map<String, String>) {
        val config = YamlConfiguration()
        entries.values.forEach { entry ->
            val path = "skins.${entry.id}"
            config.set("$path.name", entry.displayName)
            config.set("$path.texture", entry.texture)
            config.set("$path.signature", entry.signature)
            config.set("$path.source", entry.source.name.lowercase(Locale.ROOT))
            config.set("$path.mineskin-id", entry.mineSkinId)
            config.set("$path.player-name", entry.playerName)
            config.set("$path.created-at", entry.createdAt)
        }
        aliases.toSortedMap().forEach { (oldId, newId) -> config.set("aliases.$oldId", newId) }
        config.saveAtomically(file)
    }

    private fun loadLegacy(config: YamlConfiguration, source: SkinSource, entries: MutableMap<String, SkinEntry>) {
        require(!config.contains("skins") || config.isConfigurationSection("skins")) { "Invalid skin library section" }
        config.getConfigurationSection("skins")?.getKeys(false).orEmpty().forEach { rawKey ->
            val id = legacySkinId(rawKey)
            val path = "skins.$rawKey"
            val texture = config.getString("$path.texture").orEmpty()
            require(texture.isNotBlank()) { "Skin '$id' has no texture" }
            entries[id] = SkinEntry(
                id = id,
                displayName = config.getString("$path.name")?.takeIf(String::isNotBlank) ?: humanizeSkinId(id),
                texture = texture,
                signature = config.getString("$path.signature"),
                source = source,
                mineSkinId = id.removePrefix("mineskin_").takeIf { id.startsWith("mineskin_") },
                playerName = config.getString("$path.player-name"),
            )
        }
    }

    private fun loadLibrary(
        config: YamlConfiguration,
        entries: MutableMap<String, SkinEntry>,
        aliases: MutableMap<String, String>,
    ) {
        require(!config.contains("skins") || config.isConfigurationSection("skins")) { "Invalid skin library section" }
        require(!config.contains("aliases") || config.isConfigurationSection("aliases")) { "Invalid skin aliases section" }
        config.getConfigurationSection("aliases")?.getKeys(false).orEmpty().forEach { rawId ->
            aliases[validatedSkinId(rawId)] = validatedSkinId(config.getString("aliases.$rawId").orEmpty())
        }
        aliases.keys.forEach(entries::remove)
        config.getConfigurationSection("skins")?.getKeys(false).orEmpty().forEach { rawKey ->
            val id = legacySkinId(rawKey)
            if (id in aliases) return@forEach
            val path = "skins.$rawKey"
            val texture = config.getString("$path.texture").orEmpty()
            require(texture.isNotBlank()) { "Skin '$id' has no texture" }
            entries[id] = SkinEntry(
                id = id,
                displayName = config.getString("$path.name")?.let(::validatedSkinDisplayName) ?: humanizeSkinId(id),
                texture = texture,
                signature = config.getString("$path.signature"),
                source = SkinSource.from(config.getString("$path.source")),
                mineSkinId = config.getString("$path.mineskin-id"),
                playerName = config.getString("$path.player-name"),
                createdAt = config.getLong("$path.created-at").takeIf { it > 0 } ?: System.currentTimeMillis(),
            )
        }
    }
}
