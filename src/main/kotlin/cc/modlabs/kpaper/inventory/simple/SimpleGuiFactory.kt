package cc.modlabs.kpaper.inventory.simple

import dev.fruxz.stacked.text
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * DSL for quick, simple GUI creation.
 * Returns an [Inventory] with a [SimpleGUI] holder so clicks are handled by [SimpleGUIListener].
 */
fun simpleGUI(title: String, size: Int, block: SimpleGuiBuilder.() -> Unit): Inventory {
    val holder = SimpleGuiHolder(title, size)
    val builder = SimpleGuiBuilder(holder)
    builder.block()
    return holder.inventory
}

class SimpleGuiBuilder internal constructor(private val holder: SimpleGuiHolder) {
    /**
     * Place an item and attach an optional click handler without event parameter.
     */
    fun item(slot: Int, item: ItemStack, onClick: (() -> Unit)? = null) {
        holder.set(slot, item) { _ -> onClick?.invoke() }
    }

    /**
     * Place an item and attach a click handler with full event.
     */
    fun item(slot: Int, item: ItemStack, onClick: (InventoryClickEvent) -> Unit) {
        holder.set(slot, item, onClick)
    }

    /**
     * Fill border slots with the provided item (works for any 9xN inventory).
     */
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

internal class SimpleGuiHolder(title: String, size: Int) : SimpleGUI, InventoryHolder {
    private val actions = mutableMapOf<Int, (InventoryClickEvent) -> Unit>()
    override fun getInventory(): Inventory = inv
    private val inv: Inventory = Bukkit.createInventory(this, size, text(title))

    fun set(slot: Int, item: ItemStack, onClick: ((InventoryClickEvent) -> Unit)? = null) {
        inv.setItem(slot, item)
        if (onClick != null) actions[slot] = onClick
    }

    override fun open() { /* not used; returned Inventory is opened by caller */ }

    override fun handle(event: InventoryClickEvent) {
        // Only handle clicks inside this inventory
        if (event.clickedInventory?.holder !== this) return
        event.isCancelled = true
        actions[event.slot]?.invoke(event)
    }

    override fun onClose(event: InventoryCloseEvent) { /* no-op */ }

    override fun onDrag(event: InventoryDragEvent) { event.isCancelled = true }
}
