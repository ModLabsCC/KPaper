package cc.modlabs.kpaper.event.custom

import cc.modlabs.kpaper.event.KEvent
import org.bukkit.block.Block
import org.bukkit.entity.Player

class PlayerInteractAtBlockEvent(
    val player: Player,
    val block: Block,
) : KEvent()