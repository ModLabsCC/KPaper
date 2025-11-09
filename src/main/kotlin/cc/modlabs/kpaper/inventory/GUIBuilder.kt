package cc.modlabs.kpaper.inventory

import cc.modlabs.kpaper.inventory.simple.SimpleGUI
import dev.fruxz.stacked.text
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * GUI DSL used in KGUI examples.
 * Returns an Inventory whose holder handles interactions.
 */
fun GUI(title: String, size: Int, block: GUIBuilder.() -> Unit): Inventory {
    val holder = GUIHolder(title, size)
    val builder = GUIBuilder(holder)
    builder.block()
    return holder.inventory
}

class GUIBuilder internal constructor(private val holder: GUIHolder) {
    fun item(slot: Int, item: ItemStack, onClick: (() -> Unit)? = null) {
        holder.set(slot, item) { _ -> onClick?.invoke() }
    }

    fun item(slot: Int, item: ItemStack, onClick: (InventoryClickEvent) -> Unit) {
        holder.set(slot, item, onClick)
    }

    fun fill(item: ItemStack) {
        val inv = holder.inventory
        for (i in 0 until inv.size) if (inv.getItem(i) == null) holder.set(i, item)
    }

    fun fillBorder(item: ItemStack) {
        val inv = holder.inventory
        val size = inv.size
        val width = 9
        val height = size / width
        for (x in 0 until width) {
            holder.set(x, item)
            holder.set((height - 1) * width + x, item)
        }
        for (y in 0 until height) {
            holder.set(y * width, item)
            holder.set(y * width + (width - 1), item)
        }
    }
}

internal class GUIHolder(title: String, size: Int) : SimpleGUI, InventoryHolder {
    private val actions = mutableMapOf<Int, (InventoryClickEvent) -> Unit>()
    override fun getInventory(): Inventory = inv
    private val inv: Inventory = Bukkit.createInventory(this, size, text(title))

    fun set(slot: Int, item: ItemStack, onClick: ((InventoryClickEvent) -> Unit)? = null) {
        inv.setItem(slot, item)
        if (onClick != null) actions[slot] = onClick
    }

    override fun open() { /* handled by caller via Player.openInventory */ }

    override fun handle(event: InventoryClickEvent) {
        if (event.clickedInventory?.holder !== this) return
        event.isCancelled = true
        actions[event.slot]?.invoke(event)
    }

    override fun onClose(event: InventoryCloseEvent) { /* no-op */ }

    override fun onDrag(event: InventoryDragEvent) { event.isCancelled = true }
}
