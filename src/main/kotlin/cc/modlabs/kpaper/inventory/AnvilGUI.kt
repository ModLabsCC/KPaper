package cc.modlabs.kpaper.inventory

import cc.modlabs.kpaper.inventory.internal.AnvilListener
import dev.fruxz.stacked.text
import dev.fruxz.stacked.extension.asPlainString
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
    companion object {
        fun builder(): AnvilGUIBuilder = AnvilGUIBuilder()
        enum class Slot { LEFT, RIGHT, OUTPUT }
        sealed class ResponseAction {
            class ReplaceInputText(val text: String) : ResponseAction()
            object Close : ResponseAction()
            companion object {
                fun replaceInputText(text: String): ResponseAction = ReplaceInputText(text)
                fun close(): ResponseAction = Close
            }
        }
        class ClickSnapshot(val text: String)
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

@Suppress("unused")
class AnvilTextSnapshot(val text: String)

class AnvilGUIBuilder() {
    private var _title: String = "Anvil"
    private var _noCost: Boolean = false
    private var leftItem: ItemStack? = null
    private var rightItem: ItemStack? = null
    private var clickHandler: ((AnvilGUI.Companion.Slot, AnvilTextSnapshot) -> List<AnvilGUI.Companion.ResponseAction>)? = null

    fun title(title: String) = apply { _title = title }
    fun noCost(value: Boolean) = apply { _noCost = value }
    fun itemLeft(item: ItemStack) = apply { leftItem = item }
    fun itemRight(item: ItemStack) = apply { rightItem = item }
    fun onClick(handler: (AnvilGUI.Companion.Slot, AnvilTextSnapshot) -> List<AnvilGUI.Companion.ResponseAction>) = apply { clickHandler = handler }
    fun plugin(@Suppress("UNUSED_PARAMETER") plugin: org.bukkit.plugin.Plugin) = apply { /* kept for API parity */ }

    fun open(player: Player): InventoryView? {
        return player.openAnvilGUI {
            title = _title
            noCost = _noCost
            slot(AnvilSlot.INPUT_LEFT) {
                item = leftItem
                onClick = { p, event ->
                    val snapshot = AnvilTextSnapshot(event.currentItem?.itemMeta?.displayName()?.asPlainString ?: "")
                    val response = clickHandler?.invoke(AnvilGUI.Companion.Slot.LEFT, snapshot) ?: emptyList()
                    applyResponses(p, event, response)
                }
            }
            slot(AnvilSlot.INPUT_RIGHT) {
                item = rightItem
                onClick = { p, event ->
                    val snapshot = AnvilTextSnapshot(event.currentItem?.itemMeta?.displayName()?.asPlainString ?: "")
                    val response = clickHandler?.invoke(AnvilGUI.Companion.Slot.RIGHT, snapshot) ?: emptyList()
                    applyResponses(p, event, response)
                }
            }
            slot(AnvilSlot.OUTPUT) {
                onClick = { p, event ->
                    val name = event.currentItem?.itemMeta?.displayName()?.asPlainString ?: ""
                    val snapshot = AnvilTextSnapshot(name)
                    val response = clickHandler?.invoke(AnvilGUI.Companion.Slot.OUTPUT, snapshot) ?: emptyList()
                    applyResponses(p, event, response)
                }
                consumeItem = false
            }
        }
    }

    private fun applyResponses(player: Player, event: InventoryClickEvent, responses: List<AnvilGUI.Companion.ResponseAction>) {
        for (r in responses) {
            when (r) {
                is AnvilGUI.Companion.ResponseAction.ReplaceInputText -> {
                    val inv = event.inventory
                    val left = inv.getItem(0)?.clone()
                    if (left != null) {
                        val meta = left.itemMeta
                        meta.displayName(text(r.text))
                        left.itemMeta = meta
                        inv.setItem(0, left)
                    }
                }
                is AnvilGUI.Companion.ResponseAction.Close -> {
                    player.closeInventory()
                }
            }
        }
    }

    companion object
}
