package cc.modlabs.kpaper.world.area

import cc.modlabs.kpaper.world.area.model.Area
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import kotlin.math.ceil

object AreaVisualizer {
    private const val MAX_PARTICLES = 10_000

    fun show(
        area: Area,
        player: Player,
        color: Color = Color.LIME,
        spacing: Double = 0.5,
    ): Int {
        require(spacing > 0) { "Spacing must be positive" }
        if (player.world.name != area.point1.world) return 0

        val footprint = if (area.points.size == 2) {
            val first = area.point1
            val second = area.point2
            listOf(
                first.x to first.z,
                second.x to first.z,
                second.x to second.z,
                first.x to second.z,
            )
        } else {
            area.points.map { it.x to it.z }
        }
        val dust = Particle.DustOptions(color, 1f)
        var spawned = 0

        fun line(from: Location, to: Location) {
            val steps = ceil(from.distance(to) / spacing).toInt().coerceAtLeast(1)
            for (step in 0..steps) {
                if (spawned >= MAX_PARTICLES) return
                val progress = step.toDouble() / steps
                player.spawnParticle(
                    Particle.DUST,
                    from.clone().add(to.clone().subtract(from).toVector().multiply(progress)),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    dust,
                )
                spawned++
            }
        }

        for (index in footprint.indices) {
            val next = (index + 1) % footprint.size
            val from = footprint[index]
            val to = footprint[next]
            line(Location(player.world, from.first, area.minY, from.second), Location(player.world, to.first, area.minY, to.second))
            if (area.maxY != area.minY) {
                line(Location(player.world, from.first, area.maxY, from.second), Location(player.world, to.first, area.maxY, to.second))
            }
        }
        if (area.maxY != area.minY) {
            footprint.forEach { (x, z) ->
                line(Location(player.world, x, area.minY, z), Location(player.world, x, area.maxY, z))
            }
        }
        return spawned
    }
}