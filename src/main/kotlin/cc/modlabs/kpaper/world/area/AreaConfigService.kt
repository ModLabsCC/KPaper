package cc.modlabs.kpaper.world.area

import cc.modlabs.kpaper.file.config.WorldConfig
import cc.modlabs.kpaper.world.area.model.AreaFlags

object AreaConfigService {
    private fun readBooleanFlags(worldConfig: WorldConfig, areaName: String): MutableSet<String> {
        val flagsPath = "areas.$areaName.flags"
        val flags = worldConfig.getStringList(flagsPath).map { it.uppercase() }.toMutableSet()

        worldConfig.getConfigurationSection(flagsPath)?.getKeys(false)?.forEach { key ->
            if (worldConfig.getBoolean("$flagsPath.$key", false)) {
                flags += key.uppercase()
            }
        }

        return flags
    }

    fun savePoint(worldName: String, areaName: String, point: String, location: String): Any? {
        val worldConfig = WorldConfig(worldName)
        val oldValue = worldConfig.get("areas.$areaName.$point")

        worldConfig.set("areas.$areaName.$point", location)
        worldConfig.set("areas.$areaName.name", areaName)
        worldConfig.saveConfig()
        AreaCache.reloadAreas()

        return oldValue
    }

    fun saveSound(
        worldName: String,
        areaName: String,
        type: String,
        sound: String,
        volume: Float,
        pitch: Float
    ): Any? {
        val worldConfig = WorldConfig(worldName)
        val oldValue = worldConfig.get("areas.$areaName.sound.$type.name")

        worldConfig.set("areas.$areaName.sound.$type.name", sound)
        worldConfig.set("areas.$areaName.sound.$type.volume", volume)
        worldConfig.set("areas.$areaName.sound.$type.pitch", pitch)
        worldConfig.saveConfig()
        AreaCache.reloadAreas()

        return oldValue
    }

    fun addFlag(worldName: String, areaName: String, flag: String): Boolean {
        val worldConfig = WorldConfig(worldName)
        val normalizedFlag = flag.uppercase()
        val flagsPath = "areas.$areaName.flags"
        val flags = readBooleanFlags(worldConfig, areaName)

        if (normalizedFlag in flags) return false

        flags += normalizedFlag
        worldConfig.set(flagsPath, null)
        flags.forEach { existingFlag ->
            worldConfig.set("$flagsPath.$existingFlag", true)
        }
        AreaFlags.getOrCreateBoolean(normalizedFlag)
        worldConfig.saveConfig()
        AreaCache.reloadAreas()
        return true
    }

    fun removeFlag(worldName: String, areaName: String, flag: String): Boolean {
        val worldConfig = WorldConfig(worldName)
        val normalizedFlag = flag.uppercase()
        val flagsPath = "areas.$areaName.flags"
        val flags = readBooleanFlags(worldConfig, areaName)

        if (!flags.remove(normalizedFlag)) return false

        worldConfig.set(flagsPath, null)
        flags.forEach { existingFlag ->
            worldConfig.set("$flagsPath.$existingFlag", true)
        }
        worldConfig.saveConfig()
        AreaCache.reloadAreas()
        return true
    }
}