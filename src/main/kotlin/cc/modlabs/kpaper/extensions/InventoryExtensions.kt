package cc.modlabs.kpaper.extensions

import cc.modlabs.kpaper.coroutines.taskRunLater
import cc.modlabs.kpaper.inventory.ItemBuilder
import cc.modlabs.kpaper.inventory.toItemBuilder
import cc.modlabs.kpaper.main.PluginInstance
import dev.fruxz.ascend.extension.isNull
import dev.fruxz.stacked.text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.annotations.ApiStatus.Experimental
import java.util.UUID

// Constants for inventory namespaces
val NAMESPACE_GUI_IDENTIFIER = pluginKey("gui_identifier")
val NAMESPACE_ITEM_IDENTIFIER = pluginKey("item_identifier")


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

fun openWithIdentifier(player: Player, inv: Inventory, identifier: String? = null, vararg identifiers: Map<NamespacedKey, String>? = arrayOf(), identificationItem: ItemStack = Material.LIGHT_GRAY_STAINED_GLASS_PANE.asQuantity(1)) {
    if (identifier != null) inv.identify(identifier, *identifiers, identificationItem = identificationItem)
    player.openInventory(inv)
}

fun Inventory.identify(identifier: String, vararg identifiers: Map<NamespacedKey, String>? = arrayOf(), identificationItem: ItemStack = Material.LIGHT_GRAY_STAINED_GLASS_PANE.asQuantity(1)) {
    this.setItem(0, this.getItem(0)?.toItemBuilder {
        addPersistentData(NAMESPACE_GUI_IDENTIFIER, identifier)
        identifiers.forEach { map ->
            map?.forEach { (key, value) ->
                addPersistentData(key, value)
            }
        }
    }?.build() ?: identificationItem.toItemBuilder {
        addPersistentData(NAMESPACE_GUI_IDENTIFIER, identifier)
        identifiers.forEach { map ->
            map?.forEach { (key, value) ->
                addPersistentData(key, value)
            }
        }
    }.build())
}

fun Inventory.isIdentifiedAs(identifier: String): Boolean {
    return this.getItem(0)?.itemMeta?.persistentDataContainer?.get(
        NAMESPACE_GUI_IDENTIFIER,
        PersistentDataType.STRING
    ) == identifier
}

val Inventory.identifier: String?
    get() = this.getItem(0)?.itemMeta?.persistentDataContainer?.get(
        NAMESPACE_GUI_IDENTIFIER,
        PersistentDataType.STRING
    )

fun Inventory.identifier(namespacedKey: NamespacedKey): String? {
    return this.getItem(0)?.itemMeta?.persistentDataContainer?.get(
        namespacedKey,
        PersistentDataType.STRING
    )
}

fun ItemStack.isIdentifiedAs(identifier: String): Boolean {
    return this.itemMeta?.persistentDataContainer?.get(
        NAMESPACE_ITEM_IDENTIFIER,
        PersistentDataType.STRING
    ) == identifier
}

val ItemStack.identifier: String?
    get() = this.itemMeta?.persistentDataContainer?.get(
        NAMESPACE_ITEM_IDENTIFIER,
        PersistentDataType.STRING
    )

fun ItemStack.identifier(namespacedKey: NamespacedKey): String? {
    return this.itemMeta?.persistentDataContainer?.get(
        namespacedKey,
        PersistentDataType.STRING
    )
}

fun ItemStack.hasKey(namespacedKey: NamespacedKey): Boolean {
    return this.itemMeta?.persistentDataContainer?.has(
        namespacedKey,
        PersistentDataType.STRING
    ) == true
}

fun <T : Any> ItemStack.hasKey(namespacedKey: NamespacedKey, type: PersistentDataType<T, T>): Boolean {
    return this.itemMeta?.persistentDataContainer?.has(
        namespacedKey,
        type
    ) == true
}

fun ItemStack.getKey(namespacedKey: NamespacedKey): String? {
    return this.itemMeta?.persistentDataContainer?.get(
        namespacedKey,
        PersistentDataType.STRING
    )
}

fun <T : Any> ItemStack.getKey(namespacedKey: NamespacedKey, type: PersistentDataType<T, T>): T? {
    return this.itemMeta?.persistentDataContainer?.get(
        namespacedKey,
        type
    )
}

fun ItemStack.addCustomString(value: String): ItemStack {
    return this.toItemBuilder {
        addCustomString(value)
    }.build()
}

fun ItemStack.getCustomStrings(): List<String> {
    return this.itemMeta?.customModelDataComponent?.strings ?: emptyList()
}

fun ItemStack.hasCustomString(value: String): Boolean {
    return this.getCustomStrings().contains(value)
}

val PLACEHOLDER_GRAY = ItemBuilder(Material.GRAY_STAINED_GLASS_PANE) {
    display(" ")
}.build()

fun Inventory.setItem(range: IntProgression, item: ItemStack) {
    range.forEach { this.setItem(it, item) }
}

fun fillEmptyAndOpenInventory(player: Player, inv: Inventory, identifier: String? = null, vararg identifiers: Map<NamespacedKey, String>? = arrayOf(), excludeSlots: List<Int> = emptyList()) {
    fillEmptyInventory(inv, PLACEHOLDER_GRAY, identifier, *identifiers, excludeSlots = excludeSlots)
    player.openInventory(inv)
}

fun fillEmptyAndOpenInventory(player: Player, inv: Inventory, spacer: ItemStack = PLACEHOLDER_GRAY, identifier: String? = null, vararg identifiers: Map<NamespacedKey, String>? = arrayOf(), excludeSlots: List<Int> = emptyList()) {
    fillEmptyInventory(inv, spacer, identifier, *identifiers, excludeSlots = excludeSlots)
    player.openInventory(inv)
}

fun fillEmptyInventory(inventory: Inventory, customSpacer: ItemStack, identifier: String? = null, vararg identifiers: Map<NamespacedKey, String>? = arrayOf(), excludeSlots: List<Int> = emptyList()) {
    for (i in 0 until inventory.size) {
        if ((inventory.getItem(i) == null || inventory.getItem(i)!!.type == Material.AIR) && i !in excludeSlots) {
            inventory.setItem(i, customSpacer)
        }
    }
    if (identifier != null) inventory.identify(identifier, *identifiers)
}

fun Inventory.fillEmpty(filler: ItemStack, identifier: String? = null, vararg identifiers: Map<NamespacedKey, String>? = arrayOf()) {
    fillEmptyInventory(this, filler, identifier, *identifiers)
}

fun Inventory.arrangeItemsAround(newItem: ItemStack) {
    val centerSlot = this.getMiddleSlot()
    val slots = arrayOf(centerSlot - 9, centerSlot - 1, centerSlot + 1, centerSlot + 9)

    for (slot in slots) {
        // Check if the slot is within the inventory bounds
        if (slot in this.contents.indices) {
            // Check if the slot is already occupied
            if (this.getItem(slot) == null || this.getItem(slot)!!.type == Material.AIR || this.getItem(slot)!!.hasKey("spacer")) {
                this.setItem(slot, newItem)
                break
            }
        }
    }
}

fun Inventory.swapItems(firstSlot: Int, secondSlot: Int, newItem: ItemStack) {
    // Swap the two items
    val tempItem = this.getItem(firstSlot)
    this.setItem(firstSlot, this.getItem(secondSlot))
    this.setItem(secondSlot, tempItem)

    // Place the new item in the empty slot
    if (this.getItem(firstSlot).isNull) {
        this.setItem(firstSlot, newItem)
    } else if (this.getItem(secondSlot).isNull) {
        this.setItem(secondSlot, newItem)
    }
}

fun Inventory.getEmptySlot(): Int {
    for (i in 0 until this.size) {
        if (this.getItem(i) == null || this.getItem(i)!!.type == Material.AIR) {
            return i
        }
    }
    return -1
}

fun Inventory.setItemInMiddle(item: ItemStack) {
    val roundedMiddle = this.size / 2
    val middle = if (roundedMiddle % 2 == 0) roundedMiddle else roundedMiddle
    this.setItem(middle, item)
}

fun Inventory.getMiddleSlot(): Int {
    val roundedMiddle = this.size / 2
    return if (roundedMiddle % 2 == 0) roundedMiddle else roundedMiddle
}

fun Inventory.setLeftUpperCorner(item: ItemStack) {
    this.setItem(0, item)
}

fun Inventory.setRightUpperCorner(item: ItemStack) {
    this.setItem(8, item)
}

fun Inventory.setMiddleLeft(item: ItemStack) {
    val rows = this.size / 9
    val middle = if (rows % 2 == 0) rows / 2 - 1 else rows / 2
    this.setItem(middle * 9, item)
}

fun Inventory.setMiddleRight(item: ItemStack) {
    val rows = this.size / 9
    val middle = if (rows % 2 == 0) rows / 2 - 1 else rows / 2
    this.setItem(middle * 9 + 8, item)
}

fun Inventory.setUpperMiddle(item: ItemStack) {
    this.setItem(4, item)
}

fun Inventory.setLowerMiddle(item: ItemStack) {
    this.setItem(this.size - 5, item)
}

fun Inventory.setLeftLowerCorner(item: ItemStack) {
    this.setItem(this.size - 9, item)
}

fun Inventory.setRightLowerCorner(item: ItemStack) {
    this.setItem(this.size - 1, item)
}

fun ItemStack.hasKey(namespacedKey: String): Boolean {
    return this.itemMeta?.persistentDataContainer?.has(
        pluginKey(namespacedKey),
        PersistentDataType.STRING
    ) ?: false
}

fun Inventory.clone(inventoryHolder: InventoryHolder? = null, shuffeld: Boolean = false): Inventory {
    val newInventory = Bukkit.createInventory(inventoryHolder, size)
    newInventory.contents = contents

    if (shuffeld) {
        newInventory.contents = newInventory.contents.shuffled()
    }

    return newInventory
}

private inline fun <reified T> Array<T>.shuffled(): Array<T> {
    val list = toMutableList()
    list.shuffle()
    return list.toTypedArray()
}

fun Inventory.containsExact(itemStack: ItemStack): Boolean {
    return contents.any { it?.toItemBuilder()?.isExact(itemStack) == true }
}

/**
 * Sets the owner of the item to the specified UUID and schedules a task to remove the owner after a given time.
 *
 * @param uuid The UUID of the owner.
 * @param time The time (in ticks) after which the owner will be removed. If set to 0 or less, no task will be scheduled.
 * @return The modified item.
 */
fun Item.ownedBy(uuid: UUID, time: Long = 20 * 10L): Item {
    owner = uuid
    if (time <= 0) return this // No need to schedule a task

    taskRunLater(time) {
        if (!isValid) return@taskRunLater
        owner = null
    }
    return this
}

fun canItemFitInInventory(inventory: Inventory, itemToAdd: ItemStack): Boolean {
    // Clone the item to avoid modifying the original
    val itemToCheck = itemToAdd.clone()

    if (itemToCheck.amount <= 0) {
        return true // Empty stack always fits
    }

    // First pass: try to stack with existing similar items
    for (i in 0 until inventory.size) {
        val slot = inventory.getItem(i)

        // Check if the slot has an item that can be stacked with our item
        if (slot != null && slot.isSimilar(itemToCheck)) {
            val maxStackSize = slot.type.maxStackSize
            val availableSpace = maxStackSize - slot.amount

            if (availableSpace > 0) {
                itemToCheck.amount -= availableSpace

                if (itemToCheck.amount <= 0) {
                    return true // Item fits completely
                }
            }
        }
    }

    // Second pass: check for empty slots
    var remainingAmount = itemToCheck.amount
    val maxStackSize = itemToCheck.type.maxStackSize

    for (i in 0 until inventory.size) {
        val slot = inventory.getItem(i)

        if (slot == null) {
            // This is an empty slot
            remainingAmount -= maxStackSize

            if (remainingAmount <= 0) {
                return true // Item fits completely using empty slots
            }
        }
    }

    return false // Cannot fit the item
}

/**
 * Alternative version that simulates adding the item to the inventory by creating a clone,
 * useful when you need to know exactly how the item will be distributed.
 *
 * @param inventory The Bukkit/PaperMC inventory to check
 * @param itemToAdd The ItemStack that needs to be added to the inventory
 * @return Boolean indicating whether the item can fit into the inventory
 */
@Experimental
fun canItemFitInInventorySimulated(inventory: PlayerInventory, itemToAdd: ItemStack): Boolean {
    // Clone the inventory to avoid modifying the original
    val tempInventory = inventory.holder?.server?.createInventory(null, inventory.size)
        ?: return false

    // Copy all items to the temporary inventory
    for (i in 0 until inventory.size) {
        val item = inventory.getItem(i)
        if (item != null) {
            tempInventory.setItem(i, item.clone())
        }
    }

    // Try to add the item to the temporary inventory
    val leftover = tempInventory.addItem(itemToAdd.clone())

    // If there are no leftover items, it means everything fit
    return leftover.isEmpty()
}

/**
 * Extension function for PlayerInventory that checks if an ItemStack can fit
 *
 * @param itemToAdd The ItemStack to check
 * @return Boolean indicating whether the item fits
 */
fun PlayerInventory.canFit(itemToAdd: ItemStack): Boolean {
    return canItemFitInInventory(this, itemToAdd)
}
