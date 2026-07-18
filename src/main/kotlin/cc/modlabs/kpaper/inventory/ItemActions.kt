package cc.modlabs.kpaper.inventory

import org.bukkit.NamespacedKey
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ConcurrentHashMap

/** Stable PDC-backed item actions; avoids retaining mutable ItemStacks as callback keys. */
object ItemActions {
    val key: NamespacedKey = requireNotNull(NamespacedKey.fromString("kpaper:item_action"))
    private val handlers = ConcurrentHashMap<String, (InventoryClickEvent) -> Unit>()

    fun register(id: String, handler: (InventoryClickEvent) -> Unit) {
        require(id.matches(Regex("^[a-z0-9._/-]{1,128}$"))) { "Invalid item action id: $id" }
        handlers[id] = handler
    }

    fun unregister(id: String) {
        handlers.remove(id)
    }

    fun tag(item: ItemStack, id: String) {
        require(handlers.containsKey(id)) { "Item action is not registered: $id" }
        val meta = requireNotNull(item.itemMeta) {
            "Item ${item.type} does not support item metadata"
        }
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, id)
        item.itemMeta = meta
    }

    internal fun dispatch(item: ItemStack, event: InventoryClickEvent): Boolean {
        val meta = item.itemMeta ?: return false
        val id = meta.persistentDataContainer.get(key, PersistentDataType.STRING) ?: return false
        val handler = handlers[id] ?: return false
        handler(event)
        return true
    }

    fun clear() = handlers.clear()
}
