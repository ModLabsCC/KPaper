package cc.modlabs.kpaper.extensions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TranslationExtensionTest {

    @Test
    fun `minecraftTranslated wraps key with lang tag`() {
        assertEquals("<lang:test.key>", "test.key".minecraftTranslated)
    }

    @Test
    fun `getUnknownTranslation formats clickable hoverable key`() {
        val key = "missing.key"
        val expected = "<hover:show_text:'Click to copy'><click:suggest_command:'$key'>$key</click></hover>"
        assertEquals(expected, getUnknownTranslation(key))
    }

    @Test
    fun `toMinecraftTranslated formats args with colons`() {
        val out = "item.count".toMinecraftTranslated(1, 2, 3)
        assertEquals("<lang:item.count:1:2:3>", out)
    }
}
