package cc.modlabs.kpaper.inventory.simple

import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.InventoryHolder

interface SimpleGUI : InventoryHolder {

    fun open(): Unit = Unit

    fun handle(event: InventoryClickEvent): Unit = Unit

    fun onClose(event: InventoryCloseEvent): Unit = Unit

    fun onDrag(event: InventoryDragEvent): Unit = Unit
}