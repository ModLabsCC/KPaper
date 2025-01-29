package cc.modlabs.kpaper.event.custom

import cc.modlabs.kpaper.event.KEvent
import org.bukkit.entity.Player

class PlayerDamageByPlayerEvent(
    val damager: Player,
    val victim: Player,
    var damage: Double
) : KEvent()