package cc.modlabs.kpaper.inventory

import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.Base64

object ItemStackConverter {
    private const val MAX_ITEMS = 4096
    private const val MAX_ITEM_BYTES = 4 * 1024 * 1024

    fun playerInventoryToBase64(playerInventory: PlayerInventory): Array<String> =
        arrayOf(toBase64(playerInventory), itemStackArrayToBase64(playerInventory.armorContents))

    fun itemStackArrayToBase64(items: Array<ItemStack?>?): String =
        encodeItems(items ?: emptyArray())

    fun itemStackToBase64(item: ItemStack): String =
        Base64.getEncoder().encodeToString(item.serializeAsBytes())

    fun toBase64(inventory: Inventory): String =
        encodeItems(Array(inventory.size) { inventory.getItem(it) })

    @Throws(IOException::class)
    fun fromBase64(data: String?): Inventory? {
        if (data == null) return null
        val items = decodeItems(data)
        val size = ((items.size.coerceAtLeast(1) + 8) / 9) * 9
        if (size > 6 * 9) throw IOException("Inventory is too large: $size")
        return Bukkit.createInventory(null, size).also { it.contents = items }
    }

    @Throws(IOException::class)
    fun itemStackArrayFromBase64(data: String?): Array<ItemStack?>? = data?.let(::decodeItems)

    @Throws(IOException::class)
    fun itemStackFromBase64(data: String?): ItemStack {
        if (data == null) throw IOException("Item data cannot be null")
        val bytes = decodeBase64(data)
        if (bytes.size !in 1..MAX_ITEM_BYTES) throw IOException("Invalid serialized item length: ${bytes.size}")
        return ItemStack.deserializeBytes(bytes)
    }

    private fun encodeItems(items: Array<ItemStack?>): String {
        require(items.size <= MAX_ITEMS) { "Too many items" }
        val bytes = ByteArrayOutputStream().use { byteStream ->
            DataOutputStream(byteStream).use { output ->
                output.writeInt(items.size)
                items.forEach { item ->
                    if (item == null || item.isEmpty) {
                        output.writeInt(-1)
                    } else {
                        val itemBytes = item.serializeAsBytes()
                        require(itemBytes.size <= MAX_ITEM_BYTES) { "Serialized item is too large" }
                        output.writeInt(itemBytes.size)
                        output.write(itemBytes)
                    }
                }
            }
            byteStream.toByteArray()
        }
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun decodeItems(data: String): Array<ItemStack?> {
        val bytes = decodeBase64(data)
        return DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val count = input.readInt()
            if (count !in 0..MAX_ITEMS) throw IOException("Invalid item count: $count")
            arrayOfNulls<ItemStack>(count).also { items ->
                repeat(count) { index ->
                    val length = input.readInt()
                    if (length == -1) return@repeat
                    if (length !in 1..MAX_ITEM_BYTES) throw IOException("Invalid serialized item length: $length")
                    val itemBytes = input.readNBytes(length)
                    if (itemBytes.size != length) throw IOException("Unexpected end of serialized item")
                    items[index] = ItemStack.deserializeBytes(itemBytes)
                }
            }
        }
    }

    private fun decodeBase64(data: String): ByteArray = try {
        Base64.getMimeDecoder().decode(data)
    } catch (error: IllegalArgumentException) {
        throw IOException("Invalid Base64 item data", error)
    }
}
