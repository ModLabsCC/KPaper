package cc.modlabs.kpaper.world.area.event

import cc.modlabs.kpaper.world.area.model.Area
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class AreaLoadEvent(area: Area) : Event()  {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }
}