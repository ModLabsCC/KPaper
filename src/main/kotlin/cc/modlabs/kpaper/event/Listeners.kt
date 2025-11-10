package cc.modlabs.kpaper.event

import cc.modlabs.kpaper.main.PluginInstance
import cc.modlabs.kpaper.extensions.pluginManager
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.player.PlayerEvent

/**
 * Shortcut for unregistering all events in this listener.
 */
fun Listener.unregister() = HandlerList.unregisterAll(this)

/**
 * Registers the event with a custom event [executor].
 *
 * @param T the type of event
 * @param priority the priority when multiple listeners handle this event
 * @param ignoreCancelled if manual cancellation should be ignored
 * @param executor handles incoming events
 */
inline fun <reified T : Event> Listener.register(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    noinline executor: (Listener, Event) -> Unit,
) {
    pluginManager.registerEvent(T::class.java, this, priority, executor, PluginInstance, ignoreCancelled)
}

/**
 * This class represents a [Listener] with
 * only one event to listen to.
 */
abstract class SingleListener<T : Event>(
    val priority: EventPriority,
    val ignoreCancelled: Boolean,
) : Listener {
    abstract fun onEvent(event: T)
}

/**
 * Registers the [SingleListener] with its
 * event listener.
 *
 * @param priority the priority when multiple listeners handle this event
 * @param ignoreCancelled if manual cancellation should be ignored
 */
inline fun <reified T : Event> SingleListener<T>.register() {
    pluginManager.registerEvent(
        T::class.java,
        this,
        priority,
        { _, event -> (event as? T)?.let { this.onEvent(it) } },
        PluginInstance,
        ignoreCancelled
    )
}

/**
 * @param T the type of event to listen to
 * @param priority the priority when multiple listeners handle this event
 * @param ignoreCancelled if manual cancellation should be ignored
 * @param register if the event should be registered immediately
 * @param onEvent the event callback
 */
inline fun <reified T : Event> listen(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    register: Boolean = true,
    crossinline onEvent: (event: T) -> Unit,
): SingleListener<T> {
    val listener = object : SingleListener<T>(priority, ignoreCancelled) {
        override fun onEvent(event: T) = onEvent.invoke(event)
    }
    if (register) listener.register()
    return listener
}

inline fun <reified T : PlayerEvent> Player.on(priority: EventPriority = EventPriority.NORMAL, ignoreCancelled: Boolean = false, register: Boolean = true, crossinline onEvent: (event: T) -> Unit) {
    listen<T>(priority, ignoreCancelled, register) { event ->
        if (event.player != this@on) return@listen
        onEvent.invoke(event)
    }
}

inline fun <reified T : BlockEvent> Block.on(priority: EventPriority = EventPriority.NORMAL, ignoreCancelled: Boolean = false, register: Boolean = true, crossinline onEvent: (event: T) -> Unit) {
    listen<T>(priority, ignoreCancelled, register) { event ->
        if (event.block != this@on) return@listen
        onEvent.invoke(event)
    }
}

inline fun <reified T : EntityEvent> Entity.on(priority: EventPriority = EventPriority.NORMAL, ignoreCancelled: Boolean = false, register: Boolean = true, crossinline onEvent: (event: T) -> Unit) {
    listen<T>(priority, ignoreCancelled, register) { event ->
        if (event.entity != this@on) return@listen
        onEvent.invoke(event)
    }
}