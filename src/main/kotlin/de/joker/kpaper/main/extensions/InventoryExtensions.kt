package de.joker.kpaper.main.extensions

import dev.fruxz.stacked.text
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta


/**
 * Each gridIndex is a 3x3 grid inside the inventory
 * There are a total of 6 grids
 * Each grid has a size of 9 slots
 */
fun Inventory.setItemToGridSlot(gridIndex: Int, item: ItemStack) {
    val gridSlots = getGridSlots(gridIndex)

    gridSlots.forEach {
        setItem(it, item)
    }
}

/**
 * Each gridIndex is a 3x3 grid inside the inventory
 * There are a total of 6 grids
 * Each grid has a size of 9 slots
 * Slots are orders from left to right, top to bottom
 */
fun Inventory.getGridSlots(gridIndex: Int): Set<Int> {
    val gridSlots = mutableSetOf<Int>()

    val yStart = if (gridIndex < 3) 0 else 3
    val xStart = if (gridIndex < 3) gridIndex * 3 else (gridIndex - 3) * 3

    for (y in yStart until yStart + 3) {
        for (x in xStart until xStart + 3) {
            gridSlots.add(getInventorySlot(y, x))
        }
    }

    return gridSlots
}

fun getInventorySlot(y: Int, x: Int): Int {
    return x + y * 9
}


inline fun item(
    material: Material,
    amount: Int = 1,
    meta: ItemMeta.() -> Unit = {},
): ItemStack = ItemStack(material, amount).meta(meta)

inline fun <reified T : ItemMeta> ItemStack.meta(
    block: T.() -> Unit,
): ItemStack = apply {
    itemMeta = (itemMeta as? T)?.apply(block) ?: itemMeta
}

fun ItemStack.displayName(displayName: String): ItemStack = meta<ItemMeta> {
    displayName(text(displayName))
}

fun ItemStack.lore(lore: String): ItemStack = meta<ItemMeta> {
    this.lore(listOf(text(lore)))
}
fun ItemStack.lore(lore: List<String>): ItemStack = meta<ItemMeta> {
    this.lore(lore.map { text(it) })
}