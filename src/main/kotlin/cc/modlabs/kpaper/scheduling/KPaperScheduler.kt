package cc.modlabs.kpaper.scheduling

import cc.modlabs.kpaper.main.PluginInstance
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit

/** Region-safe scheduling facade for Paper and Folia-compatible consumers. */
object KPaperScheduler {
    fun global(plugin: Plugin = PluginInstance, task: () -> Unit): ScheduledTask =
        Bukkit.getGlobalRegionScheduler().run(plugin) { task() }

    fun globalLater(delayTicks: Long, plugin: Plugin = PluginInstance, task: () -> Unit): ScheduledTask =
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { task() }, delayTicks)

    fun globalRepeating(
        initialDelayTicks: Long,
        periodTicks: Long,
        plugin: Plugin = PluginInstance,
        task: (ScheduledTask) -> Unit,
    ): ScheduledTask = Bukkit.getGlobalRegionScheduler()
        .runAtFixedRate(plugin, task, initialDelayTicks, periodTicks)

    fun region(location: Location, plugin: Plugin = PluginInstance, task: () -> Unit): ScheduledTask =
        Bukkit.getRegionScheduler().run(plugin, location) { task() }

    fun regionLater(location: Location, delayTicks: Long, plugin: Plugin = PluginInstance, task: () -> Unit): ScheduledTask =
        Bukkit.getRegionScheduler().runDelayed(plugin, location, { task() }, delayTicks)

    fun entity(
        entity: Entity,
        plugin: Plugin = PluginInstance,
        retired: () -> Unit = {},
        task: () -> Unit,
    ): ScheduledTask? = entity.scheduler.run(plugin, { task() }, retired)

    fun entityLater(
        entity: Entity,
        delayTicks: Long,
        plugin: Plugin = PluginInstance,
        retired: () -> Unit = {},
        task: () -> Unit,
    ): ScheduledTask? = entity.scheduler.runDelayed(plugin, { task() }, retired, delayTicks)

    fun async(plugin: Plugin = PluginInstance, task: () -> Unit): ScheduledTask =
        Bukkit.getAsyncScheduler().runNow(plugin) { task() }

    fun asyncLater(
        delay: Long,
        unit: TimeUnit = TimeUnit.MILLISECONDS,
        plugin: Plugin = PluginInstance,
        task: () -> Unit,
    ): ScheduledTask = Bukkit.getAsyncScheduler().runDelayed(plugin, { task() }, delay, unit)

    fun cancel(plugin: Plugin = PluginInstance) {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin)
        Bukkit.getAsyncScheduler().cancelTasks(plugin)
    }
}
