package cc.modlabs.kpaper.world.area

import cc.modlabs.klassicx.tools.minecraft.toStringLocation
import cc.modlabs.kpaper.file.config.WorldConfig
import cc.modlabs.kpaper.util.getLogger
import cc.modlabs.kpaper.world.area.event.AreaLoadEvent
import cc.modlabs.kpaper.world.area.event.AreasLoadedEvent
import cc.modlabs.kpaper.world.area.model.Area
import cc.modlabs.kpaper.world.area.model.AreaFlag
import cc.modlabs.kpaper.world.area.model.AreaFlags
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

object AreaCache {

    private val areas = mutableMapOf<String, Area>()
    private val cacheLock: ReadWriteLock = ReentrantReadWriteLock()

    private fun loadForWorld(world: World): List<Area> {
        val areasConfig = WorldConfig(world)
        val areaNames = areasConfig.getConfigurationSection("areas")?.getKeys(false) ?: return emptyList()
        val loaded = mutableListOf<Area>()
        for (area in areaNames) {
            getLogger().info("Loading area $area")
            val name = areasConfig.getString("areas.$area.name")
            if (name.isNullOrBlank()) {
                getLogger().warn("Skipping area '$area' in world '${world.name}': missing name")
                continue
            }
            val points = areasConfig.getStringList("areas.$area.points")
                .filter { it.isNotBlank() }
                .map { it.toStringLocation() }
                .ifEmpty {
                    listOfNotNull(
                        areasConfig.getString("areas.$area.p1")?.takeIf { it.isNotBlank() }?.toStringLocation(),
                        areasConfig.getString("areas.$area.p2")?.takeIf { it.isNotBlank() }?.toStringLocation(),
                    )
                }
            if (points.size < 2) {
                getLogger().warn(
                    "Skipping area '$area' in world '${world.name}': at least two points are required",
                )
                continue
            }

            val entrySoundName = areasConfig.getString("areas.$area.sound.entry.name")
            val entrySoundVolume = areasConfig.getDouble("areas.$area.sound.entry.volume", 1.0).toFloat()
            val entrySoundPitch = areasConfig.getDouble("areas.$area.sound.entry.pitch", 1.0).toFloat()

            val exitSoundName = areasConfig.getString("areas.$area.sound.exit.name")
            val exitSoundVolume = areasConfig.getDouble("areas.$area.sound.exit.volume", 1.0).toFloat()
            val exitSoundPitch = areasConfig.getDouble("areas.$area.sound.exit.pitch", 1.0).toFloat()

            val flags = loadFlags(areasConfig, area)

            val areaObj = Area(
                name = name,
                points = points,
                flags = flags,
                entrySound = entrySoundName,
                entryVolume = entrySoundVolume,
                entryPitch = entrySoundPitch,
                exitSound = exitSoundName,
                exitVolume = exitSoundVolume,
                exitPitch = exitSoundPitch,
            )
            loaded += areaObj
            getLogger().info("Loaded area $area")
        }
        return loaded
    }

    fun reloadAreas() {
        val loaded = mutableListOf<Area>()
        for (world in Bukkit.getWorlds()) {
            try {
                loaded += loadForWorld(world)
            } catch (e: Exception) {
                getLogger().warn("Failed to load areas for world '${world.name}': ${e.message}")
                e.printStackTrace()
            }
        }

        cacheLock.writeLock().lock()
        try {
            areas.clear()
            loaded.forEach { areas[areaKey(it)] = it }
        } finally {
            cacheLock.writeLock().unlock()
        }
        loaded.forEach { Bukkit.getPluginManager().callEvent(AreaLoadEvent(it)) }
        Bukkit.getPluginManager().callEvent(AreasLoadedEvent(loaded.toList()))
        getLogger().info("Loaded ${loaded.size} areas.")
    }

    fun addArea(area: Area) {
        cacheLock.writeLock().lock()
        try {
            areas[areaKey(area)] = area
        } finally {
            cacheLock.writeLock().unlock()
        }
    }

    fun removeArea(area: Area) {
        cacheLock.writeLock().lock()
        try {
            areas.remove(areaKey(area))
        } finally {
            cacheLock.writeLock().unlock()
        }
    }

    fun getArea(name: String): Area? {
        cacheLock.readLock().lock()
        try {
            areas[name]?.let { return it }
            val normalized = name.trim()
            return areas.values.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
                ?: areas.entries.firstOrNull { it.key.equals(normalized, ignoreCase = true) }?.value
        } finally {
            cacheLock.readLock().unlock()
        }
    }

    fun getArea(world: String, name: String): Area? {
        cacheLock.readLock().lock()
        try {
            areas["$world:$name"]?.let { return it }
            return areas.values.firstOrNull {
                it.point1.world.equals(world, ignoreCase = true) &&
                    it.name.equals(name, ignoreCase = true)
            }
        } finally {
            cacheLock.readLock().unlock()
        }
    }

    fun getAreas(world: String): List<Area> = getAreas().filter { it.point1.world == world }

    fun getAreas(): List<Area> {
        cacheLock.readLock().lock()
        try {
            return areas.values.toList()
        } finally {
            cacheLock.readLock().unlock()
        }
    }

    fun clear() {
        cacheLock.writeLock().lock()
        try {
            areas.clear()
        } finally {
            cacheLock.writeLock().unlock()
        }
    }

    private fun loadFlags(areasConfig: WorldConfig, areaName: String): Map<AreaFlag<*>, Any> {
        val flagsPath = "areas.$areaName.flags"
        val loadedFlags = mutableMapOf<AreaFlag<*>, Any>()

        areasConfig.getStringList(flagsPath).forEach { legacyFlag ->
            val resolvedFlag = AreaFlags.getOrCreateBoolean(legacyFlag)
            loadedFlags[resolvedFlag] = true
        }

        val typedSection = areasConfig.getConfigurationSection(flagsPath)
        typedSection?.getKeys(false)?.forEach { key ->
            val rawValue = typedSection.get(key)
            val knownFlag = AreaFlags.get(key)

            if (knownFlag != null) {
                val parsedValue = parseFlagValue(knownFlag, rawValue)
                if (parsedValue != null) {
                    loadedFlags[knownFlag] = parsedValue
                }
                return@forEach
            }

            val dynamicFlag = AreaFlags.getOrCreateBoolean(key)
            dynamicFlag.decodeValue(rawValue)?.let { decoded ->
                loadedFlags[dynamicFlag] = decoded
            }
        }

        return loadedFlags
    }

    private fun parseFlagValue(flag: AreaFlag<*>, rawValue: Any?): Any? {
        @Suppress("UNCHECKED_CAST")
        return (flag as AreaFlag<Any?>).decodeValue(rawValue)
    }

    fun clear(world: String) {
        cacheLock.writeLock().lock()
        try {
            areas.entries.removeIf { it.value.point1.world == world }
        } finally {
            cacheLock.writeLock().unlock()
        }
    }

    private fun areaKey(area: Area): String = "${area.point1.world}:${area.name}"

}
