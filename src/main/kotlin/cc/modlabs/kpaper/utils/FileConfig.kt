package cc.modlabs.kpaper.utils

import cc.modlabs.kpaper.main.PluginInstance
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException


/**
 * Represents a configuration file for storing settings in YAML format.
 *
 * This class extends [YamlConfiguration] and provides additional functionality for handling file operations and
 * loading/saving configuration data.
 *
 * @property fileName The name of the configuration file.
 */
class FileConfig(fileName: String) : YamlConfiguration() {
    private var seperator: String?
    private val path: String
    fun saveConfig() {
        try {
            save(path)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    init {
        seperator = System.getProperty("file.seperator")
        if (seperator == null) {
            seperator = "/"
        }
        path = "plugins${seperator}${PluginInstance.dataFolder.name}$seperator$fileName"
        val file = File(path)
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            load(path)
        } catch (_: IOException) {
            // Do nothing
        } catch (e: InvalidConfigurationException) {
            e.printStackTrace()
        }
    }
}