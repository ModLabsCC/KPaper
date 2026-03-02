package cc.modlabs.kpaper.inventory

import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.*
import java.util.*

class InventorySerializer : ISerializer {

    @Throws(IOException::class)
    override fun serialize(inventories: List<Inventory>, outputStream: OutputStream) {
        val dataOut = DataOutputStream(outputStream)
        val inventoryCount = inventories.size
        dataOut.writeInt(inventoryCount)
        for (inventory in inventories) {
            serializeSingleInventory(inventory, outputStream)
        }
        dataOut.flush()
    }

    @Throws(IOException::class)
    override fun deserializeInventories(stream: InputStream): List<Inventory> {
        val dataIn = DataInputStream(stream)
        val inventoryCount = dataIn.readInt()
        val inventories: MutableList<Inventory> = ArrayList(inventoryCount)
        for (i in 0 until inventoryCount) {
            inventories.add(deserializeSingleInventory(dataIn))
        }
        return inventories
    }

    companion object {
        private const val NULL_STACK: Byte = 0x00
        private const val NORM_STACK: Byte = 0x01

        // -------------------------
        // New: ByteArray BLOB APIs
        // -------------------------

        /**
         * New: Serialize ONE inventory into compressed bytes (no Base64).
         * Suitable for storing in BLOB/MEDIUMBLOB.
         */
        fun serializeToBytes(inventory: Inventory): ByteArray {
            return Serializer.serializeToBytes(listOf(inventory))
        }

        /**
         * New: Deserialize ONE inventory from compressed bytes (no Base64).
         */
        @Throws(IOException::class)
        fun deserializeFromBytes(bytes: ByteArray): Inventory {
            return Serializer.deserializeInventoriesFromBytes(bytes).first()
        }

        /**
         * New: Serialize MANY inventories into compressed bytes (no Base64).
         */
        fun serializeInventoriesToBytes(inventories: List<Inventory>): ByteArray {
            return Serializer.serializeToBytes(inventories)
        }

        /**
         * New: Deserialize MANY inventories from compressed bytes (no Base64).
         */
        @Throws(IOException::class)
        fun deserializeInventoriesFromBytes(bytes: ByteArray): List<Inventory> {
            return Serializer.deserializeInventoriesFromBytes(bytes)
        }

        // -------------------------
        // Existing low-level format
        // (inventory -> OutputStream)
        // -------------------------

        @Throws(IOException::class)
        private fun serializeSingleInventory(inventory: Inventory, out: OutputStream) {
            val dataOut = DataOutputStream(out)
            val slots = inventory.size
            dataOut.writeInt(slots)
            for (i in 0 until slots) {
                val item = inventory.getItem(i)
                val isNull = item == null
                dataOut.writeByte((if (isNull) NULL_STACK else NORM_STACK).toInt())
                if (!isNull) {
                    serializeItemStack(item, dataOut)
                }
            }
            dataOut.flush()
        }

        private fun serializeItemStack(itemStack: ItemStack?, out: OutputStream) {
            BukkitObjectOutputStream(out).use { oos ->
                oos.writeObject(itemStack)
                oos.flush()
                // NOTE: use() closes the stream wrapper, but not the underlying stream in most JDK impls.
                // If you ever see issues, switch to manual flush without closing.
            }
        }

        @Throws(IOException::class, ClassNotFoundException::class)
        private fun deserializeItemStack(stream: InputStream): ItemStack {
            BukkitObjectInputStream(stream).use { ois ->
                return ois.readObject() as ItemStack
            }
        }

        @Throws(IOException::class)
        private fun deserializeSingleInventory(stream: InputStream): Inventory {
            val dais = DataInputStream(stream)
            val size = dais.readInt()
            val invSize = (1 + ((size - 1) / 9)) * 9 // ceil(size/9) * 9
            val inventory = Bukkit.createInventory(null, invSize)

            for (i in 0 until size) {
                val marker = dais.readByte()
                if (marker == NULL_STACK) continue
                val itemStack = deserializeItemStack(dais)
                inventory.setItem(i, itemStack)
            }
            return inventory
        }

        // -------------------------
        // Legacy String APIs (Base64)
        // Keep these so old data still works
        // -------------------------

        fun serialize(inventory: Inventory): String {
            return Serializer.serialize(listOf(inventory))
        }

        @Throws(IOException::class)
        fun deserialize(serialized: String): Inventory {
            return Serializer.deserializeInventories(serialized).first()
        }
    }
}