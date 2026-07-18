package cc.modlabs.kpaper.inventory.internal

import cc.modlabs.kpaper.event.EventHandler
import cc.modlabs.kpaper.event.listen
import cc.modlabs.kpaper.event.register
import cc.modlabs.kpaper.event.unregister
import cc.modlabs.kpaper.inventory.InventoryItem
import cc.modlabs.kpaper.inventory.ItemActions
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent
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
        itemClickEvents[item.clone()] = action
    }

    fun unregisterInventory(playerId: UUID) {
        inventoryMap.remove(playerId)
    }

    fun unregisterItemClickEvent(item: ItemStack) {
        itemClickEvents.remove(item)
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
        if (item.type.isAir) return@listen
        if (ItemActions.dispatch(item, it)) return@listen
        val action = itemClickEvents[item] ?: return@listen
        action(it)
    }

    private val inventoryClose = listen<InventoryCloseEvent> {
        val player = it.player as? Player ?: return@listen
        val registration = inventoryMap[player.uniqueId] ?: return@listen
        if (registration.inventory == it.inventory) inventoryMap.remove(player.uniqueId)
    }

    private val playerQuit = listen<PlayerQuitEvent> {
        inventoryMap.remove(it.player.uniqueId)
    }

    data class RegisteredInventory(
        val inventory: Inventory,
        val items: Map<Int, InventoryItem>
    )

    override fun unload() {
        itemClick.unregister()
        inventoryClick.unregister()
        inventoryClose.unregister()
        playerQuit.unregister()
        itemClickEvents.clear()
        inventoryMap.clear()
        ItemActions.clear()
    }

    override fun load() {
        itemClick.register()
        inventoryClick.register()
        inventoryClose.register()
        playerQuit.register()
    }
}
