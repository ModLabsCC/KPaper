package cc.modlabs.kpaper.event.custom

import cc.modlabs.kpaper.event.KEvent
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class PlayerDamageByPlayerEvent(
    val damager: Player,
    val victim: Player,
    var damage: Double
) : KEvent() {
    companion object {
        private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList(): HandlerList = handlerList
    }

    override fun getHandlers(): HandlerList = handlerList
}
