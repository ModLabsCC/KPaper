package cc.modlabs.kpaper.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey

class IdentityTest : FunSpec({

    class TestIdentity(override val identityKey: Key) : Identity<String>

    test("Identity should return correct namespace from identityKey") {
        val key = Key.key("minecraft", "stone")
        val identity = TestIdentity(key)
        
        identity.namespace() shouldBe "minecraft"
    }
    
    test("Identity should return correct key from identityKey") {
        val key = Key.key("minecraft", "stone")
        val identity = TestIdentity(key)
        
        identity.key() shouldBe key
    }
    
    test("Identity should return correct value from identityKey") {
        val key = Key.key("minecraft", "stone")
        val identity = TestIdentity(key)
        
        identity.value() shouldBe "stone"
    }
    
    test("Identity should return correct asString from identityKey") {
        val key = Key.key("minecraft", "stone")
        val identity = TestIdentity(key)
        
        identity.asString() shouldBe "minecraft:stone"
    }
    
    test("Identity should return correct NamespacedKey from getKey") {
        val key = Key.key("modlabs", "testkey")
        val identity = TestIdentity(key)
        val expectedNamespacedKey = NamespacedKey.fromString("modlabs:testkey")
        
        identity.getKey() shouldBe expectedNamespacedKey
    }
    
    test("Identity should handle custom namespace correctly") {
        val key = Key.key("custom_plugin", "custom_item")
        val identity = TestIdentity(key)
        
        identity.namespace() shouldBe "custom_plugin"
        identity.value() shouldBe "custom_item"
        identity.asString() shouldBe "custom_plugin:custom_item"
    }
    
    test("Identity should work with mocked Key") {
        val mockKey = mockk<Key>()
        every { mockKey.namespace() } returns "test"
        every { mockKey.value() } returns "value"
        every { mockKey.asString() } returns "test:value"
        every { mockKey.key() } returns mockKey
        
        val identity = TestIdentity(mockKey)
        
        identity.namespace() shouldBe "test"
        identity.value() shouldBe "value"
        identity.asString() shouldBe "test:value"
        identity.key() shouldBe mockKey
    }
})