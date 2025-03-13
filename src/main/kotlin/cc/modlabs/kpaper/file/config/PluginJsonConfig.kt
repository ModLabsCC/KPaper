package cc.modlabs.kpaper.file.config

import cc.modlabs.kpaper.main.PluginInstance

/**
 * Represents a configuration file for storing settings in YAML format.
 *
 *
 * @property fileName The name of the configuration file.
 */
class PluginJsonConfig(fileName: String) : FileJsonConfig("plugins/${PluginInstance.dataFolder.name}/$fileName")