package cc.modlabs.kpaper.main

import cc.modlabs.kpaper.event.CustomEventListener
import cc.modlabs.kpaper.inventory._internal.AnvilListener
import cc.modlabs.kpaper.inventory._internal.ItemClickListener
import cc.modlabs.kpaper.inventory.simple.SimpleGUIListener
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * The main plugin instance. Less complicated name for internal usage.
 */
lateinit var PluginInstance: KPlugin


abstract class KPlugin : JavaPlugin() {
    /**
     * The feature configuration for this plugin.
     */
    open val featureConfig: FeatureConfig = featureConfig { }

    /**
     * Checks whether a particular feature is enabled.
     */
    fun isFeatureEnabled(feature: Feature): Boolean = featureConfig.isEnabled(feature)

    /**
     * Called when the plugin was loaded
     */
    open fun load() {}

    /**
     * Called when the plugin was enabled
     */
    open fun startup() {}

    /**
     * Called when the plugin gets disabled
     */
    open fun shutdown() {}

    final override fun onLoad() {
        if (::PluginInstance.isInitialized) {
            logger.warning("The main instance has been modified, even though it has already been set by another plugin!")
        }
        PluginInstance = this
        load()
    }

    final override fun onEnable() {
        if (isFeatureEnabled(Feature.ITEM_CLICK)) {
            Bukkit.getPluginManager().registerEvents(AnvilListener, this)
            ItemClickListener.load()
        }
        if (isFeatureEnabled(Feature.CUSTOM_EVENTS)) {
            CustomEventListener.load()
        }
        Bukkit.getPluginManager().registerEvents(SimpleGUIListener(), this)

        startup()
    }

    final override fun onDisable() {
        if (isFeatureEnabled(Feature.ITEM_CLICK)) {
            ItemClickListener.unload()
        }
        if (isFeatureEnabled(Feature.CUSTOM_EVENTS)) {
            CustomEventListener.unload()
        }

        shutdown()
    }
}