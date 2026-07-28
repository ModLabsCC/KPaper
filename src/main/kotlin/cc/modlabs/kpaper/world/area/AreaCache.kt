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
import kotlin.math.floor

object AreaCache {

    private const val MAX_INDEXED_CHUNKS_PER_AREA = 4_096L
    private val areas = mutableMapOf<String, Area>()
    private val chunkIndex = mutableMapOf<ChunkKey, MutableList<Area>>()
    private val broadAreas = mutableMapOf<String, MutableList<Area>>()
    private val areaOrder = mutableMapOf<Area, Int>()
    private val cacheLock: ReadWriteLock = ReentrantReadWriteLock()
    private data class ChunkKey(val world: String, val x: Int, val z: Int)

    private fun loadForWorld(world: World): List<Area> {
        val areasConfig = WorldConfig(world)
        val areaNames = areasConfig.getConfigurationSection("areas")?.getKeys(false) ?: return emptyList()
        val loaded = mutableListOf<Area>()
        var skipped = 0
        var migrated = 0
        for (area in areaNames) {
            try {
                val name = areasConfig.getString("areas.$area.name")
                if (name.isNullOrBlank()) {
                    skipped++
                    continue
                }
                val pointsPath = "areas.$area.points"
                val configuredPoints = areasConfig.getStringList(pointsPath).filter { it.isNotBlank() }
                val legacyPoints = if (configuredPoints.isEmpty()) {
                    listOfNotNull(
                        areasConfig.getString("areas.$area.p1")?.takeIf { it.isNotBlank() },
                        areasConfig.getString("areas.$area.p2")?.takeIf { it.isNotBlank() },
                    )
                } else {
                    emptyList()
                }
                val pointStrings = configuredPoints.ifEmpty { legacyPoints }
                if (pointStrings.size < 2) {
                    skipped++
                    continue
                }
                val points = pointStrings.map { it.toStringLocation() }

                loaded += Area(
                    name = name,
                    points = points,
                    flags = loadFlags(areasConfig, area),
                    entrySound = areasConfig.getString("areas.$area.sound.entry.name"),
                    entryVolume = areasConfig.getDouble("areas.$area.sound.entry.volume", 1.0).toFloat(),
                    entryPitch = areasConfig.getDouble("areas.$area.sound.entry.pitch", 1.0).toFloat(),
                    exitSound = areasConfig.getString("areas.$area.sound.exit.name"),
                    exitVolume = areasConfig.getDouble("areas.$area.sound.exit.volume", 1.0).toFloat(),
                    exitPitch = areasConfig.getDouble("areas.$area.sound.exit.pitch", 1.0).toFloat(),
                )
                if (legacyPoints.isNotEmpty()) {
                    areasConfig.set(pointsPath, legacyPoints)
                    areasConfig.set("areas.$area.p1", null)
                    areasConfig.set("areas.$area.p2", null)
                    migrated++
                }
            } catch (_: Exception) {
                skipped++
            }
        }
        if (migrated > 0) areasConfig.saveConfig()
        if (skipped > 0) getLogger().warn("Skipped $skipped invalid area definitions in world '${world.name}'.")
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
            rebuildIndex()
        } finally {
            cacheLock.writeLock().unlock()
        }
        fireLoadEvents(loaded)
        getLogger().info("Loaded ${loaded.size} areas.")
    }

    internal fun reloadAreas(worldName: String) {
        val world = Bukkit.getWorld(worldName) ?: return
        val loaded = try {
            loadForWorld(world)
        } catch (e: Exception) {
            getLogger().warn("Failed to load areas for world '${world.name}': ${e.message}")
            e.printStackTrace()
            return
        }
        cacheLock.writeLock().lock()
        try {
            areas.entries.removeIf { it.value.point1.world == worldName }
            loaded.forEach { areas[areaKey(it)] = it }
            rebuildIndex()
        } finally {
            cacheLock.writeLock().unlock()
        }
        fireLoadEvents(loaded)
        getLogger().info("Loaded ${loaded.size} areas for world '$worldName'.")
    }

    fun addArea(area: Area) {
        cacheLock.writeLock().lock()
        try {
            val replaced = areas.put(areaKey(area), area)
            if (replaced == null) {
                areaOrder[area] = areas.size - 1
                index(area)
            } else {
                rebuildIndex()
            }
        } finally {
            cacheLock.writeLock().unlock()
        }
    }

    fun removeArea(area: Area) {
        cacheLock.writeLock().lock()
        try {
            areas.remove(areaKey(area))
            rebuildIndex()
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

    internal fun candidates(world: String, x: Double, z: Double): List<Area> {
        cacheLock.readLock().lock()
        try {
            val local = chunkIndex[ChunkKey(world, chunk(x), chunk(z))].orEmpty()
            val broad = broadAreas[world].orEmpty()
            if (broad.isEmpty()) return local.toList()
            return (local + broad).sortedBy { areaOrder[it] }
        } finally {
            cacheLock.readLock().unlock()
        }
    }

    fun clear() {
        cacheLock.writeLock().lock()
        try {
            areas.clear()
            rebuildIndex()
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
                if (parsedValue != null) loadedFlags[knownFlag] = parsedValue
                return@forEach
            }

            val dynamicFlag = AreaFlags.getOrCreateBoolean(key)
            dynamicFlag.decodeValue(rawValue)?.let { decoded -> loadedFlags[dynamicFlag] = decoded }
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
            rebuildIndex()
        } finally {
            cacheLock.writeLock().unlock()
        }
    }

    private fun fireLoadEvents(loaded: List<Area>) {
        loaded.forEach { Bukkit.getPluginManager().callEvent(AreaLoadEvent(it)) }
        Bukkit.getPluginManager().callEvent(AreasLoadedEvent(loaded.toList()))
    }

    private fun areaKey(area: Area): String = "${area.point1.world}:${area.name}"

    private fun rebuildIndex() {
        chunkIndex.clear()
        broadAreas.clear()
        areaOrder.clear()
        areas.values.forEachIndexed { order, area ->
            areaOrder[area] = order
            index(area)
        }
    }

    private fun index(area: Area) {
        val minChunkX = chunk(area.points.minOf { it.x })
        val maxChunkX = chunk(area.points.maxOf { it.x })
        val minChunkZ = chunk(area.points.minOf { it.z })
        val maxChunkZ = chunk(area.points.maxOf { it.z })
        val width = maxChunkX.toLong() - minChunkX + 1
        val depth = maxChunkZ.toLong() - minChunkZ + 1
        // ponytail: broad fallback avoids index explosion; use an R-tree if many huge areas become hot.
        if (width > MAX_INDEXED_CHUNKS_PER_AREA || depth > MAX_INDEXED_CHUNKS_PER_AREA ||
            width > MAX_INDEXED_CHUNKS_PER_AREA / depth
        ) {
            broadAreas.getOrPut(area.point1.world) { mutableListOf() } += area
            return
        }
        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                chunkIndex.getOrPut(ChunkKey(area.point1.world, chunkX, chunkZ)) { mutableListOf() } += area
            }
        }
    }

    private fun chunk(coordinate: Double): Int = floor(coordinate).toInt() shr 4
}