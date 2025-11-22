package cc.modlabs.kpaper.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextUtilsTest {

    @Test
    fun `plain component serializes to plain legacy`() {
        val c = Component.text("Hello")
        assertEquals("Hello", c.toLegacy())
    }

    @Test
    fun `colored component serializes with section codes`() {
        val c = Component.text("Hi").color(NamedTextColor.RED)
        // §c is the legacy code for red
        assertEquals("§cHi", c.toLegacy())
    }
}
