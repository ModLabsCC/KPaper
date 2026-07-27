package cc.modlabs.kpaper.world.area.model

import cc.modlabs.klassicx.tools.minecraft.StringLocation
import cc.modlabs.kpaper.world.area.AreaCache
import cc.modlabs.kpaper.world.area.event.AreaEnterEvent
import cc.modlabs.kpaper.world.area.event.AreaLeaveEvent
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a polygonal prism. Two points remain supported as a cuboid.
 *
 * @property name The name of the area.
 * @property points The X/Z polygon corners. Their lowest and highest Y define the vertical bounds.
 */
class Area(
    val name: String,
    val points: List<StringLocation>,
    val flags: Map<AreaFlag<*>, Any> = emptyMap(),
    val entrySound: String? = null,
    val entryVolume: Float = 1f,
    val entryPitch: Float = 1f,
    val exitSound: String? = null,
    val exitVolume: Float = 1f,
    val exitPitch: Float = 1f,
) {
    init {
        require(points.size >= 2) { "An area requires at least two points" }
        require(points.all { it.world == points.first().world }) { "All area points must be in the same world" }
    }

    constructor(
        name: String,
        point1: StringLocation,
        point2: StringLocation,
        flags: Map<AreaFlag<*>, Any> = emptyMap(),
        entrySound: String? = null,
        entryVolume: Float = 1f,
        entryPitch: Float = 1f,
        exitSound: String? = null,
        exitVolume: Float = 1f,
        exitPitch: Float = 1f,
    ) : this(
        name,
        listOf(point1, point2),
        flags,
        entrySound,
        entryVolume,
        entryPitch,
        exitSound,
        exitVolume,
        exitPitch,
    )

    val point1: StringLocation get() = points.first()
    val point2: StringLocation get() = points[1]
    val minY: Double = points.minOf { it.y }
    val maxY: Double = points.maxOf { it.y }

    fun contains(location: StringLocation): Boolean =
        contains(location.world, location.x, location.y, location.z)

    fun contains(location: Location): Boolean =
        contains(location.world?.name, location.x, location.y, location.z)

    fun contains(world: String?, x: Double, y: Double, z: Double): Boolean {
        if (world != point1.world || y < minY || y > maxY) return false
        if (points.size == 2) {
            return x >= min(point1.x, point2.x) && x <= max(point1.x, point2.x) &&
                z >= min(point1.z, point2.z) && z <= max(point1.z, point2.z)
        }

        var inside = false
        for (index in points.indices) {
            val a = points[index]
            val b = points[(index + 1) % points.size]
            if (isOnEdge(x, z, a.x, a.z, b.x, b.z)) return true
            if ((a.z > z) != (b.z > z) &&
                x < (b.x - a.x) * (z - a.z) / (b.z - a.z) + a.x
            ) {
                inside = !inside
            }
        }
        return inside
    }

    fun <T> getFlag(flag: AreaFlag<T>): T? {
        val value = flags[flag] ?: return null
        @Suppress("UNCHECKED_CAST")
        return value as? T
    }

    fun hasFlag(flag: AreaFlag<Boolean>): Boolean {
        return getFlag(flag) == true
    }

    private fun isOnEdge(
        x: Double,
        z: Double,
        ax: Double,
        az: Double,
        bx: Double,
        bz: Double,
    ): Boolean {
        val cross = (x - ax) * (bz - az) - (z - az) * (bx - ax)
        return kotlin.math.abs(cross) < 1e-9 &&
            x >= min(ax, bx) && x <= max(ax, bx) &&
            z >= min(az, bz) && z <= max(az, bz)
    }
}

/**
 * Returns the name of the chat room associated with the area.
 *
 * The chat room name is derived from the area name by excluding the prefix "chatroom_" and replacing
 * any underscores with spaces using title case format.
 *
 * @return the chat room name
 *
 * @see Area.name
 */
val Area.chatRoomName: String
    get() = name.replace("chatroom_", "").replace("_", " ")

/**
 * Determines whether a pixel location is within the given area.
 *
 * @param area The area to check against.
 * @return true if the pixel location is within the area, false otherwise.
 */
fun StringLocation.isInArea(area: Area): Boolean {
    return area.contains(this)
}

/**
 * Checks if the given location is within the specified area.
 *
 * @param area The area to check against.
 * @return `true` if the location is inside the area, `false` otherwise.
 */
fun Location.isInArea(area: Area): Boolean {
    return area.contains(this)
}

/**
 * Checks if the current pixel location is within any of the areas in the cache.
 *
 * @return true if the pixel location is within an area, false otherwise.
 */
fun StringLocation.isInArea(): Boolean {
    for (area in AreaCache.getAreas()) {
        if (area.contains(this)) {
            return true
        }
    }
    return false
}

/**
 * Checks if the current location is inside any area defined in the area cache.
 *
 * @return true if the current location is in any area, false otherwise.
 */
fun Location.isInArea(): Boolean {
    for (area in AreaCache.getAreas()) {
        if (area.contains(this)) {
            return true
        }
    }
    return false
}

/**
 * Returns the first area that contains this location, or `null` if none match.
 *
 * Prefer [areas] when overlapping areas matter (stairs through floors, nested regions, etc.).
 */
fun Location.getArea(): Area? {
    for (area in AreaCache.getAreas()) {
        if (area.contains(this)) {
            return area
        }
    }
    return null
}

/**
 * Returns every loaded area that contains this location.
 *
 * Overlapping areas are all included; order matches [AreaCache.getAreas].
 */
fun Location.areas(): List<Area> {
    val matches = ArrayList<Area>()
    for (area in AreaCache.getAreas()) {
        if (area.contains(this)) {
            matches += area
        }
    }
    return matches
}

/**
 * Returns the first area that contains this pixel location, or `null` if none match.
 *
 * Prefer [areas] when overlapping areas matter.
 */
fun StringLocation.getArea(): Area? {
    for (area in AreaCache.getAreas()) {
        if (area.contains(this)) {
            return area
        }
    }
    return null
}

/**
 * Returns every loaded area that contains this pixel location.
 */
fun StringLocation.areas(): List<Area> {
    val matches = ArrayList<Area>()
    for (area in AreaCache.getAreas()) {
        if (area.contains(this)) {
            matches += area
        }
    }
    return matches
}

/**
 * Executes when a player enters the area.
 *
 * @param player The player who entered the area.
 */
fun Area.onEnter(player: Player) {
    val event = AreaEnterEvent(this, player)
    event.callEvent()
    if (event.isCancelled) return
    entrySound?.let { player.playSound(player, it, SoundCategory.VOICE, entryVolume, entryPitch) }

    if (hasFlag(AreaFlags.SURVIVAL) && player.gameMode == GameMode.ADVENTURE) {
        player.gameMode = GameMode.SURVIVAL
    }

}

/**
 * Triggers when a player leaves an area.
 *
 * @param player The player who left the area.
 */
fun Area.onLeave(player: Player) {
    val event = AreaLeaveEvent(this, player)
    event.callEvent()
    if (event.isCancelled) return
    exitSound?.let { player.playSound(player, it, SoundCategory.VOICE, exitVolume, exitPitch) }

    if (hasFlag(AreaFlags.SURVIVAL) && player.gameMode == GameMode.SURVIVAL) {
        player.gameMode = GameMode.ADVENTURE
    }

}
