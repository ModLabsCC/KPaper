package cc.modlabs.kpaper.main

import cc.modlabs.kpaper.event.CustomEventListener
import cc.modlabs.kpaper.coroutines.initializeCoroutines
import cc.modlabs.kpaper.coroutines.shutdownCoroutines
import cc.modlabs.kpaper.inventory.internal.AnvilListener
import cc.modlabs.kpaper.inventory.internal.ItemClickListener
import cc.modlabs.kpaper.inventory.simple.SimpleGUIListener
import cc.modlabs.kpaper.inventory.mineskin.MineSkinFetcher
import cc.modlabs.kpaper.ticks.TickService
import cc.modlabs.kpaper.scheduling.KPaperScheduler
import cc.modlabs.kpaper.messages.LocalMessageCooldown
import cc.modlabs.kpaper.npc.NPCEventListener
import cc.modlabs.kpaper.party.Party
import cc.modlabs.kpaper.visuals.impl.BossBarVisuals
import cc.modlabs.kpaper.world.area.AreaSystem
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * The main plugin instance. Less complicated name for internal usage.
 */
lateinit var PluginInstance: KPlugin

/**
 * Preferred lower-camel accessor for [PluginInstance].
 */
fun pluginInstance(): KPlugin = PluginInstance


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
        if (isFeatureEnabled(Feature.AREAS)) {
            AreaSystem.registerCommands(this)
        }
        load()
    }

    final override fun onEnable() {
        initializeCoroutines()
        TickService.load(this)
        if (isFeatureEnabled(Feature.ITEM_CLICK)) {
            Bukkit.getPluginManager().registerEvents(AnvilListener, this)
            ItemClickListener.load()
        }
        if (isFeatureEnabled(Feature.CUSTOM_EVENTS)) {
            CustomEventListener.load()
        }
        if (isFeatureEnabled(Feature.INVENTORY)) {
            Bukkit.getPluginManager().registerEvents(SimpleGUIListener(), this)
        }
        if (isFeatureEnabled(Feature.AREAS)) {
            AreaSystem.load(this)
        }

        PacketEventsSupport.loadAndInit(this)

        startup()
    }

    final override fun onDisable() {
        try {
            shutdown()
        } finally {
            if (isFeatureEnabled(Feature.ITEM_CLICK)) ItemClickListener.unload()
            if (isFeatureEnabled(Feature.CUSTOM_EVENTS)) CustomEventListener.unload()
            if (isFeatureEnabled(Feature.AREAS)) AreaSystem.unload()
            TickService.unload()
            KPaperScheduler.cancel(this)
            AnvilListener.clear()
            NPCEventListener.shutdown()
            BossBarVisuals.clear()
            LocalMessageCooldown.clear()
            Party.close()
            MineSkinFetcher.clearCache()
            shutdownCoroutines()
            PacketEventsSupport.terminate()
        }
    }
}
