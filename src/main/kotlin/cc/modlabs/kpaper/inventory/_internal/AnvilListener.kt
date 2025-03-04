package cc.modlabs.kpaper.inventory._internal

import cc.modlabs.kpaper.extensions.getLogger
import cc.modlabs.kpaper.inventory.AnvilGUI
import cc.modlabs.kpaper.inventory.AnvilSlot
import dev.fruxz.stacked.extension.asPlainString
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.Inventory

object AnvilListener : Listener {
    private val activeGUIs = mutableMapOf<Inventory, Pair<Player, AnvilGUI>>()

    fun registerGUI(player: Player, inventory: Inventory, gui: AnvilGUI) {
        activeGUIs[inventory] = player to gui
    }

    fun unregisterGUI(inventory: Inventory) {
        activeGUIs.remove(inventory)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.ANVIL) return
        if (!activeGUIs.containsKey(event.inventory)) return
        val pair = activeGUIs[event.inventory]!!
        val (player, gui) = pair

        // Ensure the click comes from the intended player
        if (event.whoClicked != player) return

        // Check if the clicked slot is one of our defined slots
        val clickedSlot = AnvilSlot.entries.find { it.index == event.slot }
        if (clickedSlot != null) {
            val slotConfig = gui.slotConfigs[clickedSlot]
            slotConfig?.onClick?.invoke(player, event)
            if (clickedSlot == AnvilSlot.OUTPUT && slotConfig?.onClick == null) {
                getLogger().info("Clicked Slot: ${clickedSlot.name}")
                val clickedItem = event.currentItem ?: throw Exception("Current item is null!")
                getLogger().info("clicked item: ${clickedItem.type}")
                val inputText: String = clickedItem.displayName().asPlainString
                gui.onComplete?.invoke(player, inputText)
                player.closeInventory()
                unregisterGUI(event.inventory)
                return
            }
            if (slotConfig?.consumeItem != false) {
                event.isCancelled = true
            }
        } else {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (activeGUIs.containsKey(event.inventory)) {
            val (player, gui) = activeGUIs[event.inventory]!!
            gui.onClose?.invoke(player)
            unregisterGUI(event.inventory)
        }
    }

    @EventHandler
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        if (!activeGUIs.containsKey(event.inventory)) return

        val (player, gui) = activeGUIs[event.inventory]!!

        val viewer = event.viewers.firstOrNull()
        if (viewer !== player) return

        val inv = event.inventory

        val anvilView = event.view

        if (gui.noCost) {
            anvilView.setRepairCost(0)

            if (inv.getItem(0) != null && inv.getItem(2) == null) {
                val inputItem = inv.getItem(0)?.clone() ?: return
                inv.setItem(2, inputItem)
            }
        }
    }
}