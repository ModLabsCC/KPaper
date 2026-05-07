package cc.modlabs.kpaper.file.config

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Represents a configuration file for storing settings in YAML format.
 *
 * This class extends [org.bukkit.configuration.file.YamlConfiguration] and provides additional functionality for handling file operations and
 * loading/saving configuration data.
 *
 * @property fileName The name of the configuration file.
 */
class WorldConfig private constructor(path: String) : FileYamlConfig(path) {
    constructor(worldName: String, fileName: String = "worldConfig.yml") : this(
        resolveWorldConfigPath(worldName, fileName)
    )

    constructor(world: World, fileName: String = "worldConfig.yml") : this(
        resolveWorldConfigPath(world, fileName)
    )

    companion object {
        private const val DIMENSIONS_FOLDER = "/dimensions/"

        private fun resolveWorldConfigPath(world: World, fileName: String): String {
            val worldFolder = world.worldFolder
            val resolvedPath = worldFolder.path.replace("\\", "/") + "/$fileName"
            migrateLegacyWorldConfig(world.name, worldFolder, resolvedPath, fileName)
            return resolvedPath
        }

        private fun resolveWorldConfigPath(worldName: String, fileName: String): String {
            val world = Bukkit.getWorld(worldName)
            if (world != null) return resolveWorldConfigPath(world, fileName)
            return Bukkit.getWorldContainer().path.replace("\\", "/") + "/$worldName/$fileName"
        }

        private fun migrateLegacyWorldConfig(worldName: String, worldFolder: File, newConfigPath: String, fileName: String) {
            val normalizedWorldFolderPath = worldFolder.path.replace("\\", "/")
            if (!normalizedWorldFolderPath.contains(DIMENSIONS_FOLDER)) return

            val rootWorldFolderPath = normalizedWorldFolderPath.substringBefore(DIMENSIONS_FOLDER)
            if (rootWorldFolderPath.isBlank()) return

            val legacyConfigFile = File(rootWorldFolderPath, fileName)
            if (!legacyConfigFile.exists() || !legacyConfigFile.isFile) return

            val newConfigFile = File(newConfigPath)
            if (legacyConfigFile.absolutePath == newConfigFile.absolutePath) return

            val oldConfig = YamlConfiguration.loadConfiguration(legacyConfigFile)
            val newConfig = YamlConfiguration.loadConfiguration(newConfigFile)

            val migratedKeys = mutableListOf<String>()
            val unresolvedKeys = mutableListOf<String>()

            oldConfig.getKeys(true)
                .filter { !oldConfig.isConfigurationSection(it) }
                .forEach { key ->
                    if (!newConfig.contains(key)) {
                        newConfig.set(key, oldConfig.get(key))
                        migratedKeys += key
                    } else {
                        val newValue = newConfig.get(key)
                        val oldValue = oldConfig.get(key)
                        if (newValue != oldValue) {
                            unresolvedKeys += key
                        }
                    }
                }

            if (migratedKeys.isNotEmpty()) {
                newConfig.save(newConfigFile)
            }

            if (unresolvedKeys.isEmpty()) {
                if (legacyConfigFile.delete()) {
                    Bukkit.getLogger().info("[KPaper] WorldConfig migration for world '$worldName' complete.")
                } else {
                    Bukkit.getLogger().warning(
                        "[KPaper] WorldConfig migration for world '$worldName' completed, but legacy file could not be deleted: ${legacyConfigFile.path}"
                    )
                }
                return
            }

            migratedKeys.forEach { key -> oldConfig.set(key, null) }
            oldConfig.save(legacyConfigFile)

            Bukkit.getLogger().warning(
                "[KPaper] WorldConfig migration for world '$worldName' is partial. Kept legacy keys in ${legacyConfigFile.path}: ${unresolvedKeys.joinToString(", ")}"
            )
        }
    }
}