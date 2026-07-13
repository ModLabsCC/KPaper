package cc.modlabs.kpaper.inventory

import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class InventorySerializer : ISerializer {
    override fun serialize(inventories: List<Inventory>, outputStream: OutputStream) {
        require(inventories.size <= MAX_INVENTORIES) { "Too many inventories" }
        DataOutputStream(outputStream).let { output ->
            output.writeInt(FORMAT_VERSION)
            output.writeInt(inventories.size)
            inventories.forEach { inventory -> writeInventory(output, inventory) }
            output.flush()
        }
    }

    override fun deserializeInventories(stream: InputStream): List<Inventory> {
        val input = DataInputStream(stream)
        val version = input.readInt()
        if (version != FORMAT_VERSION) throw IOException("Unsupported inventory format version: $version")
        val count = input.readInt()
        if (count !in 0..MAX_INVENTORIES) throw IOException("Invalid inventory count: $count")
        return List(count) { readInventory(input) }
    }

    companion object {
        private const val FORMAT_VERSION = 2
        private const val MAX_INVENTORIES = 256
        private const val MAX_SLOTS = 6 * 9
        private const val MAX_ITEM_BYTES = 4 * 1024 * 1024

        private fun writeInventory(output: DataOutputStream, inventory: Inventory) {
            require(inventory.size in 1..MAX_SLOTS) { "Invalid inventory size: ${inventory.size}" }
            output.writeInt(inventory.size)
            repeat(inventory.size) { slot -> writeItem(output, inventory.getItem(slot)) }
        }

        private fun writeItem(output: DataOutputStream, item: ItemStack?) {
            if (item == null || item.isEmpty) {
                output.writeInt(-1)
                return
            }
            val bytes = item.serializeAsBytes()
            require(bytes.size <= MAX_ITEM_BYTES) { "Serialized item is too large" }
            output.writeInt(bytes.size)
            output.write(bytes)
        }

        private fun readInventory(input: DataInputStream): Inventory {
            val size = input.readInt()
            if (size !in 1..MAX_SLOTS) throw IOException("Invalid inventory size: $size")
            val inventory = Bukkit.createInventory(null, ((size + 8) / 9) * 9)
            repeat(size) { slot -> inventory.setItem(slot, readItem(input)) }
            return inventory
        }

        private fun readItem(input: DataInputStream): ItemStack? {
            val length = input.readInt()
            if (length == -1) return null
            if (length !in 1..MAX_ITEM_BYTES) throw IOException("Invalid serialized item length: $length")
            return ItemStack.deserializeBytes(input.readNBytes(length).also {
                if (it.size != length) throw IOException("Unexpected end of serialized item")
            })
        }

        fun serializeToBytes(inventory: Inventory): ByteArray = Serializer.serializeToBytes(listOf(inventory))
        fun deserializeFromBytes(bytes: ByteArray): Inventory = Serializer.deserializeInventoriesFromBytes(bytes).first()
        fun serializeInventoriesToBytes(inventories: List<Inventory>): ByteArray = Serializer.serializeToBytes(inventories)
        fun deserializeInventoriesFromBytes(bytes: ByteArray): List<Inventory> = Serializer.deserializeInventoriesFromBytes(bytes)
        fun serialize(inventory: Inventory): String = Serializer.serialize(listOf(inventory))
        fun deserialize(serialized: String): Inventory = Serializer.deserializeInventories(serialized).first()
    }
}
