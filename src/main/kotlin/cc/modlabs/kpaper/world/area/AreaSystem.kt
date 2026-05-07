package cc.modlabs.kpaper.world.area

import cc.modlabs.kpaper.main.KPlugin
import cc.modlabs.kpaper.world.area.listener.AreaListeners
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList

object AreaSystem {
    private var listeners: AreaListeners? = null

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
