package cc.modlabs.kpaper.file.config

import org.bukkit.Bukkit

/**
 * Represents a configuration file for storing settings in YAML format.

 *
 * @property fileName The name of the configuration file.
 */
class WorldJsonConfig(worldName: String, fileName: String = "worldConfig.json") : FileJsonConfig(
    (Bukkit.getWorld(worldName)?.worldFolder?.path?.replace("\\", "/") ?: "worlds/$worldName") + "/$fileName"
)