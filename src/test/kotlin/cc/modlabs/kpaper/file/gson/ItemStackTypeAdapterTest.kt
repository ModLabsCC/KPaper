package cc.modlabs.kpaper.file.gson

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ItemStackTypeAdapterTest {

    private fun gson() = GsonBuilder()
        .registerTypeAdapter(ItemStack::class.java, ItemStackTypeAdapter())
        .create()

    @Test
    @Disabled("Requires Bukkit server bootstrap for ItemStack construction; keep as documentation test")
    fun `serialize then deserialize simple stack without meta`() {
        val original = ItemStack(Material.STONE, 3)
        val g = gson()

        val json = g.toJson(original, ItemStack::class.java)
        val parsed = g.fromJson(json, ItemStack::class.java)

        assertEquals(Material.STONE, parsed.type)
        assertEquals(3, parsed.amount)
    }

    @Test
    fun `invalid material throws JsonParseException`() {
        val g = gson()
        val badJson = """
            {"item":"minecraft:not_a_real_material","count":1}
        """.trimIndent()

        assertThrows(JsonParseException::class.java) {
            g.fromJson(badJson, ItemStack::class.java)
        }
    }
}
