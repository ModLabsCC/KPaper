package cc.modlabs.kpaper.inventory

import cc.modlabs.kpaper.inventory._internal.ItemClickListener
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class InventoryBuilder(
    private val size: Int,
    private val title: Component
) {
    private val items = mutableMapOf<Int, InventoryItem>()

    fun setItem(slot: Int, item: InventoryItem): InventoryBuilder {
        items[slot] = item
        return this
    }

    fun setItem(slot: Int, item: ItemStack): InventoryBuilder {
        items[slot] = InventoryItem(item)
        return this
    }

    fun build(): Inventory {
        val inventory = Bukkit.createInventory(null, size, title)
        items.forEach { (slot, item) ->
            inventory.setItem(slot, item.itemStack)
        }
        return inventory
    }

    fun open(player: Player) {
        val inventory = build()
        player.openInventory(inventory)
        ItemClickListener.registerInventory(player, inventory, items)
    }
}

class InventoryItem(
    val itemStack: ItemStack,
    val onClick: (InventoryClickEvent) -> Unit = {}
)