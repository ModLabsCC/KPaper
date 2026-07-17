package cc.modlabs.kpaper.main

import cc.modlabs.klassicx.extensions.getLogger
import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import java.util.concurrent.ConcurrentHashMap

/**
 * Lifecycle and availability helpers for the optional PacketEvents integration.
 *
 * PacketEvents is disabled by default. Enable it via:
 * ```
 * featureConfig {
 *     enablePacketEventsFeatures = true
 * }
 * ```
 */
object PacketEventsSupport {
    @Volatile
    private var active: Boolean = false

    private val warnedCallers = ConcurrentHashMap.newKeySet<String>()

    /**
     * Whether PacketEvents was successfully initialized for this plugin.
     */
    fun isActive(): Boolean = active

    /**
     * Whether the [Feature.PACKET_EVENTS] flag is enabled in the current plugin config.
     */
    fun isFeatureEnabled(): Boolean = try {
        PluginInstance.isFeatureEnabled(Feature.PACKET_EVENTS)
    } catch (_: UninitializedPropertyAccessException) {
        false
    }

    /**
     * Initializes PacketEvents when [Feature.PACKET_EVENTS] is enabled.
     */
    fun loadAndInit(plugin: KPlugin) {
        if (!plugin.isFeatureEnabled(Feature.PACKET_EVENTS)) {
            active = false
            return
        }

        val packetEvents = SpigotPacketEventsBuilder.build(plugin)
        PacketEvents.setAPI(packetEvents)
        packetEvents.settings.checkForUpdates(false)
        packetEvents.load()
        packetEvents.init()
        active = true
        plugin.logger.info("PacketEvents enabled (display entities / packet-based NPC overrides).")
    }

    /**
     * Terminates PacketEvents if it was previously initialized by KPaper.
     */
    fun terminate() {
        if (!active) return
        try {
            PacketEvents.getAPI()?.terminate()
        } finally {
            active = false
            warnedCallers.clear()
        }
    }

    /**
     * Returns `true` when PacketEvents is usable; otherwise logs once per caller and returns `false`.
     */
    fun ensureAvailable(caller: String): Boolean {
        if (active) {
            val api = PacketEvents.getAPI()
            if (api != null) return true
        }

        if (warnedCallers.add(caller)) {
            getLogger().error(
                "[$caller] PacketEvents is not active. Enable it with " +
                    "`featureConfig { enablePacketEventsFeatures = true }` in your KPlugin."
            )
        }
        return false
    }
}
