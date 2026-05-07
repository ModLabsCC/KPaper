package cc.modlabs.kpaper.world.area

import cc.modlabs.kpaper.main.KPlugin
import cc.modlabs.kpaper.world.area.listener.AreaListeners
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

object AreaSystem {
    private var listeners: AreaListeners? = null
    private var commandHandlerRegistered = false

    fun registerCommands(plugin: KPlugin) {
        if (commandHandlerRegistered) return
        commandHandlerRegistered = true

        val command = AreaCommand()
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(command.register(), command.description, command.aliases)
        }
    }

    fun load(plugin: KPlugin) {
        if (listeners != null) return
        val areaListeners = AreaListeners()
        listeners = areaListeners

        Bukkit.getPluginManager().registerEvents(areaListeners, plugin)
        AreaCache.reloadAreas()
    }

    fun unload() {
        listeners?.let { HandlerList.unregisterAll(it) }
        listeners = null
        AreaCache.clear()
    }
}
