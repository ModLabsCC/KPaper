package cc.modlabs.kpaper.inventory.mineskin

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UrlsTest : FunSpec({

    test("Urls data class should initialize with default empty skin URL") {
        val urls = Urls()
        
        urls.skin shouldBe ""
    }
    
    test("Urls data class should accept custom skin URL") {
        val skinUrl = "https://textures.minecraft.net/texture/abc123"
        val urls = Urls(skinUrl)
        
        urls.skin shouldBe skinUrl
    }
    
    test("Urls data class should handle empty string explicitly") {
        val urls = Urls("")
        
        urls.skin shouldBe ""
    }
    
    test("Urls data class should handle typical Minecraft texture URL") {
        val textureUrl = "https://textures.minecraft.net/texture/1234567890abcdef"
        val urls = Urls(textureUrl)
        
        urls.skin shouldBe textureUrl
    }
    
    test("Urls data class should be a data class with proper equality") {
        val urls1 = Urls("test-url")
        val urls2 = Urls("test-url")
        val urls3 = Urls("different-url")
        
        urls1 shouldBe urls2
        (urls1 == urls3) shouldBe false
    }
    
    test("Urls data class should handle URL with parameters") {
        val urlWithParams = "https://example.com/skin?id=123&format=png"
        val urls = Urls(urlWithParams)
        
        urls.skin shouldBe urlWithParams
    }
    
    test("Urls copy should work correctly") {
        val original = Urls("original-url")
        val copied = original.copy(skin = "new-url")
        
        original.skin shouldBe "original-url"
        copied.skin shouldBe "new-url"
    }
})