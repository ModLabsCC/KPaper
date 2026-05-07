package cc.modlabs.kpaper.file.config

import org.bukkit.Bukkit
import org.bukkit.World

/**
 * Represents a configuration file for storing settings in YAML format.

 *
 * @property fileName The name of the configuration file.
 */
class WorldJsonConfig private constructor(path: String) : FileJsonConfig(path) {
    constructor(worldName: String, fileName: String = "worldConfig.json") : this(
        resolveWorldConfigPath(worldName, fileName)
    )

    constructor(world: World, fileName: String = "worldConfig.json") : this(
        resolveWorldConfigPath(world, fileName)
    )

    companion object {
        private fun resolveWorldConfigPath(world: World, fileName: String): String {
            return world.worldFolder.path.replace("\\", "/") + "/$fileName"
        }

        private fun resolveWorldConfigPath(worldName: String, fileName: String): String {
            val world = Bukkit.getWorld(worldName)
            if (world != null) return resolveWorldConfigPath(world, fileName)
            return Bukkit.getWorldContainer().path.replace("\\", "/") + "/$worldName/$fileName"
        }
    }
}