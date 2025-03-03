package cc.modlabs.kpaper.inventory._internal

import cc.modlabs.kpaper.inventory.AnvilGUI
import cc.modlabs.kpaper.inventory.AnvilSlot
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory

object AnvilListener : Listener {
    private val activeGUIs = mutableMapOf<Inventory, Pair<Player, AnvilGUI>>()

    fun registerGUI(player: Player, inventory: Inventory, gui: AnvilGUI) {
        activeGUIs[inventory] = player to gui
    }

    fun unregisterGUI(inventory: Inventory) {
        activeGUIs.remove(inventory)
    }

    // Listen for clicks in the anvil GUI
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val pair = activeGUIs[event.inventory] ?: return
        val (player, gui) = pair

        // Ensure the click comes from the intended player
        if (event.whoClicked != player) return

        // Check if the clicked slot is one of our defined slots
        val clickedSlot = AnvilSlot.entries.find { it.index == event.slot }
        if (clickedSlot != null) {
            val slotConfig = gui.slotConfigs[clickedSlot]
            // Call the custom onClick handler if one is defined
            slotConfig?.onClick?.invoke(player, event)
            // For the output slot, if no custom onClick is provided, use the default onComplete callback
            if (clickedSlot == AnvilSlot.OUTPUT && slotConfig?.onClick == null) {
                val clickedItem = event.currentItem
                // Typically the "renamed" text is taken from the item's display name
                val inputText = clickedItem?.itemMeta?.displayName()
                gui.onComplete?.invoke(player, inputText)
                player.closeInventory()
                unregisterGUI(event.inventory)
                return
            }
            // Cancel the event if consumeItem is true (default) to prevent default item transfer
            if (slotConfig?.consumeItem != false) {
                event.isCancelled = true
            }
        } else {
            // Cancel clicks outside our defined slots
            event.isCancelled = true
        }
    }

    // Clean up when the anvil inventory is closed
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (activeGUIs.containsKey(event.inventory)) {
            val (player, gui) = activeGUIs[event.inventory]!!
            gui.onClose?.invoke(player)
            unregisterGUI(event.inventory)
        }
    }
}