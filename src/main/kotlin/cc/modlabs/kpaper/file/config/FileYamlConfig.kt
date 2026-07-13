package cc.modlabs.kpaper.file.config

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

/**
 * Represents a configuration file for storing settings in YAML format.
 *
 * This class extends [org.bukkit.configuration.file.YamlConfiguration] and provides additional functionality for handling file operations and
 * loading/saving configuration data.
 *
 * @property fileName The name of the configuration file.
 */
abstract class FileYamlConfig(var path: String) : YamlConfiguration() {
    val file: File

    fun saveConfig() {
        try {
            save(path)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun deleteConfig() {
        file.delete()
    }

    init {
        file = File(path).canonicalFile
        path = file.path
        try {
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                file.createNewFile()
            }
            load(path)
        } catch (e: IOException) {
            throw IllegalStateException("Unable to initialize YAML configuration at $path", e)
        } catch (e: InvalidConfigurationException) {
            e.printStackTrace()
        }
    }
}
