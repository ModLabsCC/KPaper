package cc.modlabs.kpaper.inventory

import org.bukkit.inventory.Inventory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterOutputStream


object Serializer {
    // in case something breaking needs to be introduced
    private const val V1: Byte = 0x00
    private const val CURRENT_VERSION: Byte = V1
    private val serializers: MutableMap<Byte, ISerializer> = mutableMapOf(V1 to InventorySerializer())

    private const val COMPRESSION_LEVEL = 4

    @Throws(IOException::class)
    fun compress(data: ByteArray, outputStream: ByteArrayOutputStream) {
        val deflater = Deflater()
        deflater.setLevel(COMPRESSION_LEVEL)
        val deflateStream = DeflaterOutputStream(outputStream, deflater)
        deflateStream.write(data)
        deflateStream.flush()
        deflateStream.close()
    }

    @Throws(IOException::class)
    fun decompress(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val inflateStream = InflaterOutputStream(out)
        inflateStream.write(data)
        inflateStream.flush()
        inflateStream.close()
        return out.toByteArray()
    }

    fun serialize(inventories: List<Inventory>): String {
        val output = ByteArrayOutputStream()
        output.write(CURRENT_VERSION.toInt())
        serializers[CURRENT_VERSION]!!.serialize(inventories, output)
        val data = output.toByteArray()
        output.reset()
        compress(data, output)
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }

    @Throws(IOException::class)
    fun deserializeInventories(serialized: String): List<Inventory> {
        val compressed = Base64.getDecoder().decode(serialized)
        val uncompressed: ByteArray = decompress(compressed)
        val version = uncompressed[0]
        val serializer: ISerializer = serializers[version]
            ?: throw IOException("Invalid serializer version (" + version.toInt() + ") at position " + 0)
        val dataWithoutVersion = uncompressed.copyOfRange(1, uncompressed.size)
        return serializer.deserializeInventories(ByteArrayInputStream(dataWithoutVersion))
    }
}