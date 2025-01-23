package cc.modlabs.kpaper.inventory

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

interface GUI {

    fun open(player: Player)

    fun handle(player: Player, event: InventoryClickEvent)
}