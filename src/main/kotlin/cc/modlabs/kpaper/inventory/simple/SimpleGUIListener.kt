package cc.modlabs.kpaper.inventory.simple

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

internal class SimpleGUIListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return
        val holder =  inventory.getHolder(false)
        if (holder is SimpleGUI) {
            holder.handle(event)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val inventory = event.inventory
        val holder =  inventory.getHolder(false)
        if (holder is SimpleGUI) {
            holder.onClose(event)
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val inventory = event.inventory
        val holder =  inventory.getHolder(false)
        if (holder is SimpleGUI) {
            holder.onDrag(event)
        }
    }

}