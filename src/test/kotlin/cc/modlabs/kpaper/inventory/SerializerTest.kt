package cc.modlabs.kpaper.inventory

import com.google.gson.JsonObject
import io.mockk.*
import org.bukkit.inventory.Inventory
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.*
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

class SerializerTest {

    private val version: Byte = 0x01

    @BeforeEach
    fun setUp() {
        unmockkAll()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun serializeToBytes() {
        val inv1 = mockk<Inventory>(relaxed = true)
        val inv2 = mockk<Inventory>(relaxed = true)
        val inventories = listOf(inv1, inv2)

        val serializerMock = mockk<ISerializer>()
        every { serializerMock.serialize(any(), any()) } answers {
            val out = secondArg<OutputStream>()
            DataOutputStream(out).use { dos ->
                dos.writeInt(0xC0FFEE)
                dos.writeInt(firstArg<List<Inventory>>().size)
                dos.flush()
            }
        }

        withSerializerMock(serializerMock) {
            val compressed = Serializer.serializeToBytes(inventories)
            val uncompressed = inflateRaw(compressed)

            assertTrue(uncompressed.isNotEmpty())
            assertEquals(version, uncompressed[0])

            val din = DataInputStream(ByteArrayInputStream(uncompressed, 1, uncompressed.size - 1))
            assertEquals(0xC0FFEE, din.readInt())
            assertEquals(2, din.readInt())

            verify(exactly = 1) { serializerMock.serialize(inventories, any()) }
        }
    }

    @Test
    fun deserializeInventoriesFromBytes() {
        val expected = listOf(mockk<Inventory>(), mockk<Inventory>())

        val serializerMock = mockk<ISerializer>()
        every { serializerMock.deserializeInventories(any()) } returns expected

        withSerializerMock(serializerMock) {
            // raw = [version] + payload that gets passed into deserializeInventories()
            val raw = byteArrayOf(version, 0x11, 0x22, 0x33, 0x44)
            val compressed = deflateRaw(raw)

            val result = Serializer.deserializeInventoriesFromBytes(compressed)
            assertSame(expected, result)

            verify(exactly = 1) { serializerMock.deserializeInventories(any()) }
        }
    }

    @Test
    fun exportDataContainerToBlob() {
        val container = JsonObject().apply {
            addProperty("realm_player_health", 20.0)
            addProperty("realm_player_hunger", 20)
            addProperty("realm_player_primary_inventory", "eF6...==")
        }

        val blob = Serializer.exportDataContainerToBlob(container)
        val uncompressed = inflateRaw(blob)

        assertTrue(uncompressed.isNotEmpty())
        assertEquals(version, uncompressed[0])

        val jsonBytes = uncompressed.copyOfRange(1, uncompressed.size)
        val jsonString = String(jsonBytes, Charsets.UTF_8)

        val parsed = com.google.gson.JsonParser.parseString(jsonString).asJsonObject
        assertEquals(container, parsed)
    }

    @Test
    fun importDataContainerFromBlob() {
        val container = JsonObject().apply {
            addProperty("a", 1)
            addProperty("b", "x")
        }
        val blob = Serializer.exportDataContainerToBlob(container)

        val result = Serializer.importDataContainerFromBlob(blob)
        assertEquals(container, result)
    }

    @Test
    fun serialize() {
        val inv = mockk<Inventory>(relaxed = true)
        val inventories = listOf(inv)

        val serializerMock = mockk<ISerializer>()
        every { serializerMock.serialize(any(), any()) } answers {
            val out = secondArg<OutputStream>()
            val dos = DataOutputStream(out)
            dos.writeInt(0xABCD)
            dos.flush()
        }

        withSerializerMock(serializerMock) {
            val encoded = Serializer.serialize(inventories)

            val compressed = Base64.getDecoder().decode(encoded)
            val uncompressed = inflateRaw(compressed)

            assertEquals(version, uncompressed[0])

            val din = DataInputStream(ByteArrayInputStream(uncompressed, 1, uncompressed.size - 1))
            assertEquals(0xABCD, din.readInt())

            verify(exactly = 1) { serializerMock.serialize(inventories, any()) }
        }
    }

    @Test
    fun deserializeInventories() {
        val expected = listOf(mockk<Inventory>())

        val serializerMock = mockk<ISerializer>()
        every { serializerMock.deserializeInventories(any()) } returns expected

        withSerializerMock(serializerMock) {
            val raw = byteArrayOf(version, 0x55, 0x66)
            val compressed = deflateRaw(raw)
            val encoded = Base64.getEncoder().encodeToString(compressed)

            val result = Serializer.deserializeInventories(encoded)
            assertSame(expected, result)

            verify(exactly = 1) { serializerMock.deserializeInventories(any()) }
        }
    }

    @Test
    fun importDataContainerFromBlob_nullOrEmpty_returnsEmpty() {
        assertEquals(JsonObject(), Serializer.importDataContainerFromBlob(null))
        assertEquals(JsonObject(), Serializer.importDataContainerFromBlob(byteArrayOf()))
    }

    @Test
    fun importDataContainerFromBlob_wrongVersion_throws() {
        val raw = byteArrayOf(0x02, '{'.code.toByte(), '}'.code.toByte())
        val blob = deflateRaw(raw)

        val ex = assertThrows<IOException> { Serializer.importDataContainerFromBlob(blob) }
        assertTrue(ex.message!!.contains("Unsupported container version"))
    }

    @Test
    fun deserializeInventoriesFromBytes_emptyPayload_throws() {
        val blob = deflateRaw(byteArrayOf())
        val ex = assertThrows<IOException> { Serializer.deserializeInventoriesFromBytes(blob) }
        assertTrue(ex.message!!.contains("Empty payload"))
    }

    // -----------------------------
    // Reflection helper: swap serializer implementation for tests
    // -----------------------------

    private fun withSerializerMock(mock: ISerializer, block: () -> Unit) {
        val map = getPrivateSerializersMap()
        val old = map[version]
        map[version] = mock
        try {
            block()
        } finally {
            if (old != null) map[version] = old else map.remove(version)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getPrivateSerializersMap(): MutableMap<Byte, ISerializer> {
        val field = Serializer::class.java.getDeclaredField("serializers")
        field.isAccessible = true
        return field.get(Serializer) as MutableMap<Byte, ISerializer>
    }

    // -----------------------------
    // Local inflate/deflate helpers
    // -----------------------------

    private fun deflateRaw(data: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        val deflater = Deflater(4, false)
        DeflaterOutputStream(baos, deflater).use { it.write(data) }
        return baos.toByteArray()
    }

    private fun inflateRaw(compressed: ByteArray): ByteArray {
        InflaterInputStream(ByteArrayInputStream(compressed)).use { inflater ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            while (true) {
                val r = inflater.read(buf)
                if (r <= 0) break
                out.write(buf, 0, r)
            }
            return out.toByteArray()
        }
    }
}