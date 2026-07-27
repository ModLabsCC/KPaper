package cc.modlabs.kpaper.world.area

import cc.modlabs.kpaper.main.PluginInstance
import cc.modlabs.kpaper.world.area.model.Area
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

object AreaVisualizer {
    private const val MAX_PARTICLES = 10_000
    private data class Viewer(val playerId: UUID, val world: String, val area: String)
    private val tasks = ConcurrentHashMap<Viewer, ScheduledTask>()

    fun showFor(
        area: Area,
        player: Player,
        durationTicks: Long = 100,
        color: Color = Color.LIME,
        spacing: Double = 0.5,
        periodTicks: Long = 20,
    ): Boolean {
        require(durationTicks > 0) { "Duration must be positive" }
        require(periodTicks > 0) { "Period must be positive" }
        var remaining = ceil(durationTicks.toDouble() / periodTicks).toInt()
        return player.scheduler.runAtFixedRate(
            PluginInstance,
            { task ->
                show(area, player, color, spacing)
                if (--remaining == 0) task.cancel()
            },
            {},
            1,
            periodTicks,
        ) != null
    }

    fun toggle(
        area: Area,
        player: Player,
        color: Color = Color.LIME,
        spacing: Double = 0.5,
        periodTicks: Long = 20,
    ): Boolean {
        require(periodTicks > 0) { "Period must be positive" }
        val key = Viewer(player.uniqueId, area.point1.world, area.name.lowercase(Locale.ROOT))
        tasks.remove(key)?.let {
            it.cancel()
            return false
        }

        val task = player.scheduler.runAtFixedRate(
            PluginInstance,
            { show(area, player, color, spacing) },
            { tasks.remove(key) },
            1,
            periodTicks,
        ) ?: return false
        tasks[key] = task
        return true
    }

    internal fun stop(area: Area) {
        val areaName = area.name.lowercase(Locale.ROOT)
        tasks.keys.filter { it.world == area.point1.world && it.area == areaName }.forEach { key ->
            tasks.remove(key)?.cancel()
        }
    }

    internal fun clear() {
        tasks.values.forEach { it.cancel() }
        tasks.clear()
    }

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