package cc.modlabs.kpaper.file.config

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import dev.fruxz.ascend.json.readJson
import dev.fruxz.ascend.json.writeJson
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems

/**
 * Represents a configuration file for storing settings in JSON format.
 *
 * This abstract class provides functionality for handling file operations and
 * loading/saving configuration data in JSON format.
 *
 * @property path The path to the configuration file.
 */
abstract class FileJsonConfig(var path: String) {

    companion object {
        val gson = Gson()
    }

    // Use the correct system property for file separator.
    private val separator: String = FileSystems.getDefault().separator ?: "/"

    val file: File
    var json: JsonObject = JsonObject()

    init {
        path = path.replace("/", separator)
        file = File(path)
        try {
            if (!file.exists()) {
                file.createNewFile()
                json = JsonObject() // Initialize an empty configuration
            } else {
                // If file.readJson() returns null, use an empty JsonObject instead.
                json = file.readJson() ?: JsonObject()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
    }

    /**
     * Saves the current configuration to disk.
     */
    fun saveConfig() {
        try {
            file.writeJson(json)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes the configuration file from disk.
     */
    fun deleteConfig() {
        if (!file.delete()) {
            System.err.println("Failed to delete configuration file: $path")
        }
    }

    /**
     * Reloads the configuration from disk.
     */
    fun reloadConfig() {
        try {
            if (file.exists()) {
                json = file.readJson() ?: JsonObject()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if the configuration contains the specified key (dotted path).
     *
     * Example: "database.host" will check for json["database"]["host"].
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    fun containsKey(key: String): Boolean {
        return resolvePath(key) != null
    }

    /**
     * Removes the configuration value at the specified key (dotted path).
     *
     * Example: remove("database.host") removes the host entry from the database object.
     *
     * @param key The key to remove.
     */
    fun remove(key: String) {
        val keys = key.split(".")
        if (keys.isEmpty()) return

        // If there is only one key, remove it from the root object.
        if (keys.size == 1) {
            json.remove(keys[0])
        } else {
            // Navigate to the parent JsonObject.
            var current = json
            for (i in 0 until keys.size - 1) {
                if (current.has(keys[i]) && current.get(keys[i]).isJsonObject) {
                    current = current.getAsJsonObject(keys[i])
                } else {
                    // Key path does not exist, nothing to remove.
                    return
                }
            }
            current.remove(keys.last())
        }
    }

    /**
     * Clears the entire configuration by resetting it to an empty JsonObject.
     */
    fun clearConfig() {
        json = JsonObject()
    }

    /**
     * Resolves a JSON element by a dotted key path.
     * Returns null if the path does not exist.
     *
     * Example: For key "database.host", it traverses json["database"]["host"].
     */
    fun resolvePath(path: String): JsonElement? {
        val keys = path.split(".")
        var current: JsonElement = json
        for (key in keys) {
            if (current is JsonObject && current.has(key)) {
                current = current.get(key)
            } else {
                return null
            }
        }
        return current
    }

    /**
     * Sets a JSON element at the specified dotted key path.
     * Creates intermediate JsonObjects if they do not exist.
     *
     * Example: set("database.host", JsonPrimitive("localhost"))
     * will create/update json["database"]["host"].
     */
    fun set(key: String, value: JsonElement) {
        val keys = key.split(".")
        var current = json
        for (i in 0 until keys.size - 1) {
            val k = keys[i]
            if (!current.has(k) || !current.get(k).isJsonObject) {
                current.add(k, JsonObject())
            }
            current = current.getAsJsonObject(k)
        }
        current.add(keys.last(), value)
    }

    fun get(key: String): JsonElement? = resolvePath(key)

    fun getString(key: String): String? =
        resolvePath(key)?.takeIf { it.isJsonPrimitive }?.asString

    fun getInt(key: String): Int? =
        resolvePath(key)?.takeIf { it.isJsonPrimitive }?.asInt

    fun getLong(key: String): Long? =
        resolvePath(key)?.takeIf { it.isJsonPrimitive }?.asLong

    fun getDouble(key: String): Double? =
        resolvePath(key)?.takeIf { it.isJsonPrimitive }?.asDouble

    fun getBoolean(key: String): Boolean? =
        resolvePath(key)?.takeIf { it.isJsonPrimitive }?.asBoolean

    fun getFloat(key: String): Float? =
        resolvePath(key)?.takeIf { it.isJsonPrimitive }?.asFloat

    fun getJsonObject(key: String): JsonObject? =
        resolvePath(key)?.takeIf { it.isJsonObject }?.asJsonObject

    fun getJsonArray(key: String): JsonArray? =
        resolvePath(key)?.takeIf { it.isJsonArray }?.asJsonArray

    fun getStringList(key: String): List<String>? =
        getJsonArray(key)?.mapNotNull { if (it.isJsonPrimitive) it.asString else null }

    fun getIntList(key: String): List<Int>? =
        getJsonArray(key)?.mapNotNull { if (it.isJsonPrimitive) it.asInt else null }

    fun getLongList(key: String): List<Long>? =
        getJsonArray(key)?.mapNotNull { if (it.isJsonPrimitive) it.asLong else null }

    fun getDoubleList(key: String): List<Double>? =
        getJsonArray(key)?.mapNotNull { if (it.isJsonPrimitive) it.asDouble else null }

    fun getBooleanList(key: String): List<Boolean>? =
        getJsonArray(key)?.mapNotNull { if (it.isJsonPrimitive) it.asBoolean else null }

    fun getFloatList(key: String): List<Float>? =
        getJsonArray(key)?.mapNotNull { if (it.isJsonPrimitive) it.asFloat else null }

    fun setStringList(key: String, value: List<String>) {
        set(key, gson.toJsonTree(value))
    }
}
