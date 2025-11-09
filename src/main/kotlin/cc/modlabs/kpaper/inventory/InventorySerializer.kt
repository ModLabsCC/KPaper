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
            serialize(inventory, outputStream)
        }
        dataOut.flush()
    }

    @Throws(IOException::class)
    override fun deserializeInventories(stream: InputStream): List<Inventory> {
        val dataIn = DataInputStream(stream)
        val inventoryCount = dataIn.readInt()
        val inventories: MutableList<Inventory> = ArrayList(inventoryCount)
        for (i in 0 until inventoryCount) {
            val inventory = deserializeInventory(dataIn)
            inventories.add(inventory)
        }
        return inventories
    }

    companion object {
        private const val NULL_STACK: Byte = 0x00
        private const val NORM_STACK: Byte = 0x01

        @Throws(IOException::class)
        fun serialize(inventory: Inventory, out: OutputStream) {
            val dataOut = DataOutputStream(out)
            val slots = inventory.size
            dataOut.writeInt(slots)
            for (i in 0 until slots) {
                val item = inventory.getItem(i)
                val isNull = item == null
                dataOut.writeByte((if (isNull) NULL_STACK else NORM_STACK).toInt())
                if (isNull) continue
                serialize(item, dataOut)
            }
            dataOut.flush()
        }

        fun serialize(itemStack: ItemStack?, out: OutputStream) {
            val dataOutput = BukkitObjectOutputStream(out)
            dataOutput.writeObject(itemStack)
            dataOutput.flush()
        }

        fun deserializeItemStack(stream: InputStream): ItemStack {
            val dataInput = BukkitObjectInputStream(stream)
            return dataInput.readObject() as ItemStack
        }

        fun deserializeInventory(stream: InputStream): Inventory {
            val dais = DataInputStream(stream)
            val size = dais.readInt()
            val invSize = (1 + ((size - 1) / 9)) * 9 // invSize = ceil(size/9) * 9
            val inventory = Bukkit.createInventory(null, invSize)
            for (i in 0 until size) {
                if (dais.readByte() == NULL_STACK) continue
                val itemStack = deserializeItemStack(dais)
                inventory.setItem(i, itemStack)
            }
            return inventory
        }

        fun serialize(inventory: Inventory): String {
            return Serializer.serialize(listOf(inventory))
        }

        fun deserialize(serialized: String): Inventory {
            return Serializer.deserializeInventories(serialized).first()
        }
    }
}
