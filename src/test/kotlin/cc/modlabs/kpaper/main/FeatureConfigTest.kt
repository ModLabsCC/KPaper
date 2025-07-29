package cc.modlabs.kpaper.main

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainAll

class FeatureConfigTest : FunSpec({

    test("Feature enum should contain expected values") {
        val features = Feature.values()
        
        features shouldContainAll arrayOf(Feature.ITEM_CLICK, Feature.CUSTOM_EVENTS)
    }
    
    test("FeatureConfig should check if feature is enabled") {
        val config = FeatureConfig(mapOf(Feature.ITEM_CLICK to true, Feature.CUSTOM_EVENTS to false))
        
        config.isEnabled(Feature.ITEM_CLICK) shouldBe true
        config.isEnabled(Feature.CUSTOM_EVENTS) shouldBe false
    }
    
    test("FeatureConfig should return false for unknown features") {
        val config = FeatureConfig(emptyMap())
        
        config.isEnabled(Feature.ITEM_CLICK) shouldBe false
        config.isEnabled(Feature.CUSTOM_EVENTS) shouldBe false
    }
    
    test("FeatureConfigBuilder should enable all features by default") {
        val config = FeatureConfigBuilder().build()
        
        config.isEnabled(Feature.ITEM_CLICK) shouldBe true
        config.isEnabled(Feature.CUSTOM_EVENTS) shouldBe true
    }
    
    test("FeatureConfigBuilder should allow feature override") {
        val builder = FeatureConfigBuilder()
        builder.feature(Feature.ITEM_CLICK, false)
        val config = builder.build()
        
        config.isEnabled(Feature.ITEM_CLICK) shouldBe false
        config.isEnabled(Feature.CUSTOM_EVENTS) shouldBe true // should remain default
    }
    
    test("FeatureConfigBuilder should handle multiple overrides") {
        val builder = FeatureConfigBuilder()
        builder.feature(Feature.ITEM_CLICK, false)
        builder.feature(Feature.CUSTOM_EVENTS, false)
        val config = builder.build()
        
        config.isEnabled(Feature.ITEM_CLICK) shouldBe false
        config.isEnabled(Feature.CUSTOM_EVENTS) shouldBe false
    }
    
    test("DSL featureConfig should work correctly") {
        val config = featureConfig {
            feature(Feature.ITEM_CLICK, false)
        }
        
        config.isEnabled(Feature.ITEM_CLICK) shouldBe false
        config.isEnabled(Feature.CUSTOM_EVENTS) shouldBe true
    }
    
    test("DSL featureConfig should handle empty configuration") {
        val config = featureConfig { }
        
        config.isEnabled(Feature.ITEM_CLICK) shouldBe true
        config.isEnabled(Feature.CUSTOM_EVENTS) shouldBe true
    }
    
    test("DSL featureConfig should handle all features disabled") {
        val config = featureConfig {
            feature(Feature.ITEM_CLICK, false)
            feature(Feature.CUSTOM_EVENTS, false)
        }
        
        config.isEnabled(Feature.ITEM_CLICK) shouldBe false
        config.isEnabled(Feature.CUSTOM_EVENTS) shouldBe false
    }
    
    test("FeatureConfig data class should have correct flags") {
        val flags = mapOf(Feature.ITEM_CLICK to true, Feature.CUSTOM_EVENTS to false)
        val config = FeatureConfig(flags)
        
        config.flags shouldBe flags
    }
})