package cc.modlabs.kpaper.inventory

import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus
import org.bukkit.inventory.Inventory

@ApiStatus.Experimental
abstract class KGUI {
    protected abstract val title: String
    protected abstract val size: Int

    protected abstract fun build(player: Player): Inventory

    fun open(player: Player) {
        val inv = build(player)
        player.openInventory(inv)
    }

    fun refresh(player: Player) {
        open(player)
    }
}
