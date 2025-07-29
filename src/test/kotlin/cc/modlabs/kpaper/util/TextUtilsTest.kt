package cc.modlabs.kpaper.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class TextUtilsTest : FunSpec({

    test("Component.toLegacy should convert plain text component to legacy string") {
        val component = Component.text("Hello World")
        val legacy = component.toLegacy()
        
        legacy shouldBe "Hello World"
    }
    
    test("Component.toLegacy should convert colored text component to legacy string with color codes") {
        val component = Component.text("Red Text", NamedTextColor.RED)
        val legacy = component.toLegacy()
        
        legacy shouldBe "§cRed Text"
    }
    
    test("Component.toLegacy should handle bold formatting") {
        val component = Component.text("Bold Text").decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
        val legacy = component.toLegacy()
        
        legacy shouldBe "§lBold Text"
    }
    
    test("Component.toLegacy should handle italic formatting") {
        val component = Component.text("Italic Text").decorate(net.kyori.adventure.text.format.TextDecoration.ITALIC)
        val legacy = component.toLegacy()
        
        legacy shouldBe "§oItalic Text"
    }
    
    test("Component.toLegacy should handle underlined formatting") {
        val component = Component.text("Underlined Text").decorate(net.kyori.adventure.text.format.TextDecoration.UNDERLINED)
        val legacy = component.toLegacy()
        
        legacy shouldBe "§nUnderlined Text"
    }
    
    test("Component.toLegacy should handle strikethrough formatting") {
        val component = Component.text("Strikethrough Text").decorate(net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH)
        val legacy = component.toLegacy()
        
        legacy shouldBe "§mStrikethrough Text"
    }
    
    test("Component.toLegacy should handle obfuscated formatting") {
        val component = Component.text("Obfuscated Text").decorate(net.kyori.adventure.text.format.TextDecoration.OBFUSCATED)
        val legacy = component.toLegacy()
        
        legacy shouldBe "§kObfuscated Text"
    }
    
    test("Component.toLegacy should handle combined color and formatting") {
        val component = Component.text("Blue Bold Text", NamedTextColor.BLUE)
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
        val legacy = component.toLegacy()
        
        legacy shouldBe "§9§lBlue Bold Text"
    }
    
    test("Component.toLegacy should handle empty component") {
        val component = Component.empty()
        val legacy = component.toLegacy()
        
        legacy shouldBe ""
    }
    
    test("Component.toLegacy should handle complex nested components") {
        val component = Component.text()
            .append(Component.text("Hello ", NamedTextColor.GREEN))
            .append(Component.text("World", NamedTextColor.RED))
            .build()
        val legacy = component.toLegacy()
        
        legacy shouldBe "§aHello §cWorld"
    }
})