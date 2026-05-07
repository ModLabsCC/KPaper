package cc.modlabs.kpaper.world.area.event

import cc.modlabs.kpaper.world.area.model.Area
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class AreaEnterEvent(area: Area, val player: Player) : AreaEvent(area) {
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