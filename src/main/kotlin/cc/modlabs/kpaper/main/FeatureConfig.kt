package cc.modlabs.kpaper.main

/**
 * The different features that can be toggled. New features can be added here.
 */
enum class Feature {
    ITEM_CLICK,
    CUSTOM_EVENTS,
}

/**
 * Holds the configuration for enabling/disabling features.
 * It maps each feature to a Boolean indicating whether the feature is active.
 */
data class FeatureConfig internal constructor(val flags: Map<Feature, Boolean>) {
    fun isEnabled(feature: Feature): Boolean = flags[feature] ?: false
}

/**
 * DSL builder to construct a FeatureConfig.
 * We start with all features enabled to ensure backward compatibility.
 */
class FeatureConfigBuilder {
    private val flags = mutableMapOf<Feature, Boolean>().apply {
        mapOf(
            Feature.ITEM_CLICK to true,
            Feature.CUSTOM_EVENTS to true,
        ).forEach { (feature, enabled) -> put(feature, enabled) }
    }

    // Allows developers to override the default for a particular feature.
    fun feature(feature: Feature, enabled: Boolean) {
        flags[feature] = enabled
    }

    fun build() = FeatureConfig(flags)
}

/**
 * DSL entry point to build a FeatureConfig.
 */
fun featureConfig(block: FeatureConfigBuilder.() -> Unit): FeatureConfig =
    FeatureConfigBuilder().apply(block).build()