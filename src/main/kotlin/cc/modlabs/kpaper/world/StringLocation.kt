package cc.modlabs.kpaper.world

import cc.modlabs.klassicx.tools.minecraft.StringLocation
import net.minecraft.core.BlockPos
import org.bukkit.Bukkit
import org.bukkit.Location

fun StringLocation.toBlockPos(): BlockPos {
    return BlockPos(x.toInt(), y.toInt(), z.toInt())
}


fun StringLocation.toBukkitLocation(): Location {
    return Location(
        Bukkit.getWorld(world),
        x,
        y,
        z,
        yaw,
        pitch
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