package cc.modlabs.kpaper.main

import cc.modlabs.kpaper.event.CustomEventListener
import cc.modlabs.kpaper.inventory._internal.ItemClickListener
import org.bukkit.plugin.java.JavaPlugin

/**
 * The main plugin instance. Less complicated name for internal usage.
 */
lateinit var PluginInstance: KPlugin


abstract class KPlugin : JavaPlugin() {
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
        ItemClickListener.load()
        CustomEventListener.load()

        startup()
    }

    final override fun onDisable() {
        ItemClickListener.unload()
        CustomEventListener.unload()

        shutdown()
    }
}