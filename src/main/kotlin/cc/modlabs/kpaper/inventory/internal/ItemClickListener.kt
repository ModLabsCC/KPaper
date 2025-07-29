package cc.modlabs.kpaper.inventory.internal

import cc.modlabs.kpaper.event.EventHandler
import cc.modlabs.kpaper.event.listen
import cc.modlabs.kpaper.event.register
import cc.modlabs.kpaper.event.unregister
import cc.modlabs.kpaper.inventory.InventoryItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object ItemClickListener: EventHandler() {
    private val itemClickEvents: MutableMap<ItemStack, (event: InventoryClickEvent) -> Unit> = mutableMapOf()
    private val inventoryMap = mutableMapOf<UUID, RegisteredInventory>()

    fun registerInventory(player: Player, inventory: Inventory, items: Map<Int, InventoryItem>) {
        inventoryMap[player.uniqueId] = RegisteredInventory(inventory, items)
    }

    fun registerItemClickEvent(item: ItemStack, action: (event: InventoryClickEvent) -> Unit) {
        itemClickEvents[item] = action
    }

    private val inventoryClick = listen<InventoryClickEvent> {
        val player = it.whoClicked as? Player ?: return@listen
        val registeredInventory = inventoryMap[player.uniqueId] ?: return@listen

        if (it.clickedInventory == registeredInventory.inventory) {
            it.isCancelled = true
            val clickedItem = registeredInventory.items[it.slot]
            clickedItem?.onClick?.invoke(it)
        }
    }

    private val itemClick = listen<InventoryClickEvent> {
        val item = it.currentItem ?: return@listen
        val action = itemClickEvents[item] ?: return@listen
        action(it)
    }

    data class RegisteredInventory(
        val inventory: Inventory,
        val items: Map<Int, InventoryItem>
    )

    override fun unload() {
        itemClick.unregister()
        inventoryClick.unregister()
    }

    override fun load() {
        itemClick.register()
        inventoryClick.register()
    }
}