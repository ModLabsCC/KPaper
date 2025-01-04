package cc.modlabs.kpaper.coroutines

import cc.modlabs.kpaper.main.PluginInstance
import org.bukkit.Bukkit

/**
 * Executes the given [runnable] with the given [delay].
 * Either sync or async (specified by the [sync] parameter).
 */
fun taskRunLater(delay: Long, sync: Boolean = true, runnable: () -> Unit) {
    if (sync)
        Bukkit.getScheduler().runTaskLater(PluginInstance, runnable, delay)
    else
        Bukkit.getScheduler().runTaskLaterAsynchronously(PluginInstance, runnable, delay)
}

/**
 * Executes the given [runnable] either
 * sync or async (specified by the [sync] parameter).
 */
fun taskRun(sync: Boolean = true, runnable: () -> Unit) {
    if (sync) {
        sync(runnable)
    } else {
        async(runnable)
    }
}