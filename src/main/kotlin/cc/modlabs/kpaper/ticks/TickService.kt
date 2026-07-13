package cc.modlabs.kpaper.ticks

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.concurrent.CopyOnWriteArraySet

object TickService : Listener {
    private val startListeners = CopyOnWriteArraySet<(Int) -> Unit>()
    private val endListeners = CopyOnWriteArraySet<(Int, Double, Long) -> Unit>()

    fun onStart(listener: (tick: Int) -> Unit): AutoCloseable {
        startListeners += listener
        return AutoCloseable { startListeners -= listener }
    }

    fun onEnd(listener: (tick: Int, tickDurationMillis: Double, remainingNanos: Long) -> Unit): AutoCloseable {
        endListeners += listener
        return AutoCloseable { endListeners -= listener }
    }

    @EventHandler
    fun handleStart(event: ServerTickStartEvent) {
        startListeners.forEach { it(event.tickNumber) }
    }

    @EventHandler
    fun handleEnd(event: ServerTickEndEvent) {
        endListeners.forEach { it(event.tickNumber, event.tickDuration, event.timeRemaining) }
    }

    internal fun load(plugin: Plugin) {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    internal fun unload() {
        HandlerList.unregisterAll(this)
        startListeners.clear()
        endListeners.clear()
    }
}
