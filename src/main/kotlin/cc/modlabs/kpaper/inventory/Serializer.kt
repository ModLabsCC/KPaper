package cc.modlabs.kpaper.inventory

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.inventory.Inventory
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

object Serializer {
    // in case something breaking needs to be introduced
    private const val CONTAINER_VERSION: Byte = 0x01
    private const val CURRENT_VERSION: Byte = CONTAINER_VERSION
    private const val MAX_CONTAINER_JSON_BYTES: Int = 16 * 1024 * 1024
    private val serializers: MutableMap<Byte, ISerializer> = mutableMapOf(CONTAINER_VERSION to InventorySerializer())

    private const val COMPRESSION_LEVEL = 4

    // Safety cap against corrupted/hostile compressed blobs expanding huge.
    // Tune this to your realistic max inventory payload size.
    private const val MAX_DECOMPRESSED_BYTES: Int = 8 * 1024 * 1024 // 8 MB

    /**
     * New: Serialize inventories -> compressed binary payload (no Base64).
     * Format: [1 byte version] + serializer payload, then deflated.
     */
    fun serializeToBytes(inventories: List<Inventory>): ByteArray {
        val raw = ByteArrayOutputStream().use { out ->
            out.write(CURRENT_VERSION.toInt())
            serializers[CURRENT_VERSION]!!.serialize(inventories, out)
            out.toByteArray()
        }
        return deflate(raw)
    }

    /**
     * New: Deserialize inventories from compressed binary payload (no Base64).
     */
    @Throws(IOException::class)
    fun deserializeInventoriesFromBytes(compressed: ByteArray): List<Inventory> {
        val uncompressed = inflateWithLimit(compressed, MAX_DECOMPRESSED_BYTES)
        if (uncompressed.isEmpty()) throw IOException("Empty payload")

        val version: Byte = uncompressed[0]
        val serializer = serializers[version] ?: throw IOException("Invalid serializer version (${version.toInt()}) at position 0")
        val dataWithoutVersion = uncompressed.copyOfRange(1, uncompressed.size)
        return serializer.deserializeInventories(ByteArrayInputStream(dataWithoutVersion))
    }

    /**
     * Export JsonObject -> compressed bytes for BLOB storage.
     * Format: [1 byte version] + UTF-8 JSON bytes, then deflated.
     */
    fun exportDataContainerToBlob(container: JsonObject): ByteArray {
        val jsonBytes = container.toString().toByteArray(StandardCharsets.UTF_8)

        val raw = ByteArrayOutputStream().use { out ->
            out.write(CONTAINER_VERSION.toInt())
            out.write(jsonBytes)
            out.toByteArray()
        }

        return deflate(raw)
    }

    /**
     * Import compressed bytes from BLOB -> JsonObject.
     * Returns empty JsonObject if null/empty.
     */
    @Throws(IOException::class)
    fun importDataContainerFromBlob(blob: ByteArray?): JsonObject {
        if (blob == null || blob.isEmpty()) return JsonObject()

        val uncompressed = inflateWithLimit(blob, MAX_CONTAINER_JSON_BYTES)
        if (uncompressed.isEmpty()) throw IOException("Empty container payload")

        val version = uncompressed[0]
        if (version != CURRENT_VERSION) {
            throw IOException("Unsupported container version: ${version.toInt()}")
        }

        val jsonBytes = uncompressed.copyOfRange(1, uncompressed.size)
        val json = String(jsonBytes, StandardCharsets.UTF_8)

        val parsed = JsonParser.parseString(json)
        return if (parsed.isJsonObject) parsed.asJsonObject else JsonObject()
    }

    /**
     * Legacy compatibility: keep your old Base64 API but implement it via the new functions.
     */
    fun serialize(inventories: List<Inventory>): String {
        val bytes = serializeToBytes(inventories)
        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * Legacy compatibility: Base64 -> bytes -> new deserializer.
     */
    @Throws(IOException::class)
    fun deserializeInventories(serialized: String): List<Inventory> {
        val bytes = Base64.getDecoder().decode(serialized)
        return deserializeInventoriesFromBytes(bytes)
    }

    private fun deflate(data: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        val deflater = Deflater(COMPRESSION_LEVEL, /* nowrap = */ false)
        DeflaterOutputStream(baos, deflater).use { it.write(data) }
        return baos.toByteArray()
    }

    private fun inflateWithLimit(compressed: ByteArray, maxBytes: Int): ByteArray {
        InflaterInputStream(ByteArrayInputStream(compressed)).use { inflater ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            var total = 0

            while (true) {
                val read = inflater.read(buf)
                if (read <= 0) break
                total += read
                if (total > maxBytes) {
                    throw IOException("Decompressed data exceeds limit ($total > $maxBytes)")
                }
                out.write(buf, 0, read)
            }
            return out.toByteArray()
        }
    }
}