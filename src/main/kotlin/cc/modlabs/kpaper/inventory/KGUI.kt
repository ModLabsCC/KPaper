package cc.modlabs.kpaper.inventory

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
abstract class KGUI<T> {

    protected abstract val size: Int
    protected abstract val title: Component

    protected abstract fun buildInventory(player: Player, context: T?): InventoryBuilder

    fun open(player: Player, context: T?) {
        val builder = buildInventory(player, context)
        builder.open(player)
    }
}