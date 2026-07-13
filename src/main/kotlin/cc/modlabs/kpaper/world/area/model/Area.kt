package cc.modlabs.kpaper.world.area.model

import cc.modlabs.klassicx.tools.minecraft.StringLocation
import cc.modlabs.kpaper.world.area.AreaCache
import cc.modlabs.kpaper.world.area.event.AreaEnterEvent
import cc.modlabs.kpaper.world.area.event.AreaLeaveEvent
import cc.modlabs.kpaper.world.toBukkitLocation
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import kotlin.math.max
import kotlin.math.min

/**
 * Represents an area defined by two points in pixel coordinates.
 *
 * @property name The name of the area.
 * @property point1 The first pixel location defining the area.
 * @property point2 The second pixel location defining the area.
 */
class Area(
    val name: String,
    val point1: StringLocation,
    val point2: StringLocation,
    val flags: Map<AreaFlag<*>, Any> = emptyMap(),
    val entrySound: String? = null,
    val entryVolume: Float = 1f,
    val entryPitch: Float = 1f,
    val exitSound: String? = null,
    val exitVolume: Float = 1f,
    val exitPitch: Float = 1f,
) {

    fun contains(location: StringLocation): Boolean {
        return contains(location.toBukkitLocation())
    }

    fun contains(location: Location): Boolean {
        val minX = min(point1.x, point2.x)
        val maxX = max(point1.x, point2.x)
        val minY = min(point1.y, point2.y)
        val maxY = max(point1.y, point2.y)
        val minZ = min(point1.z, point2.z)
        val maxZ = max(point1.z, point2.z)

        return location.world?.name == point1.world &&
                location.x >= minX && location.x <= maxX &&
                location.y >= minY && location.y <= maxY &&
                location.z >= minZ && location.z <= maxZ
    }

    fun <T> getFlag(flag: AreaFlag<T>): T? {
        val value = flags[flag] ?: return null
        @Suppress("UNCHECKED_CAST")
        return value as? T
    }

    fun hasFlag(flag: AreaFlag<Boolean>): Boolean {
        return getFlag(flag) == true
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
 * Returns the area that contains this location.
 *
 * Uses the static method `getAreas()` of the `AreaCache` class to retrieve a list of all available areas.
 * Iterates over the list of areas and checks if the current location is contained within each area.
 * Returns the first area that contains the location, or `null` if no area contains the location.
 *
 * @return The area that contains this location, or `null` if no area contains the location.
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
 * Gets the area that contains the pixel location.
 *
 * @return the Area object that contains the pixel location, or null if no area contains the location.
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
