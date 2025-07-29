package cc.modlabs.kpaper.inventory

import cc.modlabs.kpaper.inventory.internal.ItemClickListener
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class InventoryBuilder(
    private val size: Int,
    private val title: Component,
    private val owner: Player? = null,
    private var items: MutableMap<Int, InventoryItem> = mutableMapOf()
) {
    val inventory = Bukkit.createInventory(owner, size, title)

    fun setItem(slot: Int, item: InventoryItem): InventoryBuilder {
        items[slot] = item
        return this
    }

    fun setItem(slot: Int, item: ItemStack): InventoryBuilder {
        items[slot] = InventoryItem(item)
        return this
    }

    fun build(): Inventory {
        items.forEach { (slot, item) ->
            inventory.setItem(slot, item.itemStack)
        }
        return inventory
    }

    fun open(player: Player) {
        val registerableInvetory = build()
        player.openInventory(registerableInvetory)
        ItemClickListener.registerInventory(player, registerableInvetory, items)
    }

    fun fill(item: InventoryItem): InventoryBuilder {
        for (i in 0 until size) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item.itemStack)
            }
        }
        return this
    }
}

class InventoryItem(
    val itemStack: ItemStack,
    val onClick: (InventoryClickEvent) -> Unit = {}
)