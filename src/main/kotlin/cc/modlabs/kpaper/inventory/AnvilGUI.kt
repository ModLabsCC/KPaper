package cc.modlabs.kpaper.inventory

import cc.modlabs.kpaper.inventory._internal.AnvilListener
import dev.fruxz.stacked.text
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack

/**
 * Enum class representing the slots in an anvil inventory.
 * @param index The index of the slot in the inventory.
 */
enum class AnvilSlot(val index: Int) {
    INPUT_LEFT(0),
    INPUT_RIGHT(1),
    OUTPUT(2)
}

/**
 * Configuration for an anvil slot.
 * @param item The item to display in the slot.
 * @param onClick The action to perform when the slot is clicked.
 * @param consumeItem Whether to consume the item when the slot is clicked.
 */
data class SlotConfig(
    var item: ItemStack? = null,
    var onClick: ((player: Player, event: InventoryClickEvent) -> Unit)? = null,
    var consumeItem: Boolean = true
)

class AnvilGUI {
    var title: String = "Anvil"
    var noCost: Boolean = false
    val slotConfigs: MutableMap<AnvilSlot, SlotConfig> = mutableMapOf()
    var onComplete: ((player: Player, input: String) -> Unit)? = null
    var onClose: ((player: Player) -> Unit)? = null

    fun slot(slot: AnvilSlot, builder: SlotBuilder.() -> Unit) {
        val slotBuilder = SlotBuilder().apply(builder)
        slotConfigs[slot] = SlotConfig(
            item = slotBuilder.item,
            onClick = slotBuilder.onClick,
            consumeItem = slotBuilder.consumeItem
        )
    }
}

class SlotBuilder {
    var item: ItemStack? = null
    var onClick: ((player: Player, event: InventoryClickEvent) -> Unit)? = null
    var consumeItem: Boolean = true
}

fun Player.openAnvilGUI(builder: AnvilGUI.() -> Unit): InventoryView? {
    val gui = AnvilGUI().apply(builder)
    val inv: Inventory = Bukkit.createInventory(null, InventoryType.ANVIL, text(gui.title))

    gui.slotConfigs.forEach { (slot, config) ->
        config.item?.let { inv.setItem(slot.index, it) }
    }
    AnvilListener.registerGUI(this, inv, gui)
    val view = this.openInventory(inv)

    return view
}