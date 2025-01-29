package cc.modlabs.kpaper.world

import org.bukkit.Bukkit
import org.bukkit.Location

data class StringLocation(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val world: String,
) {

    override fun toString(): String {
        return "${x},${y},${z},${yaw},${pitch},${world}"
    }

    fun toBukkitLocation(): Location {
        return Location(
            Bukkit.getWorld(world),
            x,
            y,
            z,
            yaw,
            pitch
        )
    }
}


fun String.toStringLocation(): StringLocation {
    val split = split(",")
    return StringLocation(
        split[0].toDoubleOrNull() ?: 0.0,
        split[1].toDoubleOrNull() ?: 0.0,
        split[2].toDoubleOrNull() ?: 0.0,
        split[3].toFloatOrNull() ?: 0.0f,
        split[4].toFloatOrNull() ?: 0.0f,
        split.getOrNull(5) ?: "world",
    )
}

fun Location.toStringLocation(): StringLocation {
    return StringLocation(
        x,
        y,
        z,
        yaw,
        pitch,
        world.name,
    )
}