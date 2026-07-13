package cc.modlabs.kpaper.event.custom

import cc.modlabs.kpaper.event.KEvent
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class PlayerInteractAtBlockEvent(
    val player: Player,
    val block: Block,
) : KEvent() {
    companion object {
        private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList(): HandlerList = handlerList
    }

    override fun getHandlers(): HandlerList = handlerList
}
