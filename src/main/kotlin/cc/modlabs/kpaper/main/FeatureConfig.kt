package cc.modlabs.kpaper.main

/**
 * The different features that can be toggled. New features can be added here.
 */
enum class Feature {
    ITEM_CLICK,
    CUSTOM_EVENTS,
    INVENTORY,
    COMMANDS,
    COROUTINES,
    FILES,
    WORLD,
    MESSAGE,
    VISUALS,
    GAME,
    UTIL,
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
            Feature.INVENTORY to true,
            Feature.COMMANDS to true,
            Feature.COROUTINES to true,
            Feature.FILES to true,
            Feature.WORLD to true,
            Feature.MESSAGE to true,
            Feature.VISUALS to true,
            Feature.GAME to true,
            Feature.UTIL to true,
        ).forEach { (feature, enabled) -> put(feature, enabled) }
    }

    // Allows developers to override the default for a particular feature.
    fun feature(feature: Feature, enabled: Boolean) {
        flags[feature] = enabled
    }

    // Docs-compatible toggles
    var enableEventFeatures: Boolean
        get() = flags[Feature.CUSTOM_EVENTS] == true
        set(value) { flags[Feature.CUSTOM_EVENTS] = value }

    var enableCommandFeatures: Boolean
        get() = flags[Feature.COMMANDS] == true
        set(value) { flags[Feature.COMMANDS] = value }

    var enableInventoryFeatures: Boolean
        get() = flags[Feature.INVENTORY] == true
        set(value) { flags[Feature.INVENTORY] = value }

    var enableCoroutineFeatures: Boolean
        get() = flags[Feature.COROUTINES] == true
        set(value) { flags[Feature.COROUTINES] = value }

    var enableFileFeatures: Boolean
        get() = flags[Feature.FILES] == true
        set(value) { flags[Feature.FILES] = value }

    var enableWorldFeatures: Boolean
        get() = flags[Feature.WORLD] == true
        set(value) { flags[Feature.WORLD] = value }

    var enableMessageFeatures: Boolean
        get() = flags[Feature.MESSAGE] == true
        set(value) { flags[Feature.MESSAGE] = value }

    var enableVisualFeatures: Boolean
        get() = flags[Feature.VISUALS] == true
        set(value) { flags[Feature.VISUALS] = value }

    var enableGameFeatures: Boolean
        get() = flags[Feature.GAME] == true
        set(value) { flags[Feature.GAME] = value }

    var enableUtilFeatures: Boolean
        get() = flags[Feature.UTIL] == true
        set(value) { flags[Feature.UTIL] = value }

    fun build() = FeatureConfig(flags)
}

/**
 * DSL entry point to build a FeatureConfig.
 */
fun featureConfig(block: FeatureConfigBuilder.() -> Unit): FeatureConfig =
    FeatureConfigBuilder().apply(block).build()