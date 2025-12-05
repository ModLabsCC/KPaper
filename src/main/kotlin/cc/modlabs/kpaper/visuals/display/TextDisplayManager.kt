package cc.modlabs.kpaper.visuals.display

import cc.modlabs.kpaper.util.getLogger
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Quaternion4f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import dev.fruxz.stacked.text
import org.bukkit.Location
import org.bukkit.entity.Player
import org.joml.Quaternionf
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A manager class for creating and managing packet-based Text Display entities in Minecraft.
 *
 * Text Displays are client-side entities that can display formatted text at specific locations.
 * This manager handles the lifecycle of these displays, including creation, updates, and removal.
 *
 * @example
 * ```kotlin
 * val manager = TextDisplayManager()
 * val display = manager.createTextDisplay(
 *     text = "Hello World!",
 *     location = player.location,
 *     viewer = player,
 *     billboard = TextDisplayManager.TextDisplayBillboard.CENTER,
 *     glow = false,
 *     opacity = 255,
 *     displayFlags = listOf(TextDisplayFlags.HAS_SHADOW),
 *     backgroundColor = 0x40000000
 * )
 * ```
 */
class TextDisplayManager {

    /**
     * Stores all active Text Displays, mapped by Entity ID.
     */
    private val activeDisplays = ConcurrentHashMap<Int, TextDisplay>()

    /**
     * Counter for generating unique Entity IDs.
     * Starts at 1000000 to avoid conflicts with regular entities.
     */
    private val nextEntityId = AtomicInteger(1000000)

    /**
     * Creates a Text Display for a specific player.
     *
     * @param text The text to display (supports color codes and Adventure components)
     * @param location The position of the Text Display
     * @param viewer The player who should see the Text Display
     * @param billboard The billboard mode for the display
     * @param glow Whether the display should glow (outline effect)
     * @param opacity The opacity of the text (0-255, -1 for default)
     * @param displayFlags The display flags to apply (shadow, transparency, alignment, etc.)
     * @param backgroundColor The background color in ARGB format (0xAARRGGBB)
     * @return The created TextDisplay object
     */
    fun createTextDisplay(
        text: String,
        location: Location,
        viewer: Player,
        billboard: TextDisplayBillboard,
        glow: Boolean = false,
        opacity: Int,
        displayFlags: List<TextDisplayFlags>,
        backgroundColor: Int,
    ): TextDisplay {
        val entityId = nextEntityId.getAndIncrement()
        val textDisplay = TextDisplay(
            entityId, text, location.clone(), mutableSetOf(viewer), billboard, glow, opacity, displayFlags, backgroundColor
        )

        spawnTextDisplay(textDisplay, viewer)
        activeDisplays[entityId] = textDisplay

        return textDisplay
    }

    /**
     * Creates a Text Display for multiple players.
     *
     * @param text The text to display (supports color codes and Adventure components)
     * @param location The position of the Text Display
     * @param viewers The players who should see the Text Display
     * @param billboard The billboard mode for the display
     * @param glow Whether the display should glow (outline effect)
     * @param opacity The opacity of the text (0-255, -1 for default)
     * @param displayFlags The display flags to apply (shadow, transparency, alignment, etc.)
     * @param backgroundColor The background color in ARGB format (0xAARRGGBB)
     * @return The created TextDisplay object
     */
    fun createTextDisplay(
        text: String,
        location: Location,
        viewers: Collection<Player>,
        billboard: TextDisplayBillboard,
        glow: Boolean = false,
        opacity: Int,
        displayFlags: List<TextDisplayFlags>,
        backgroundColor: Int,
    ): TextDisplay {
        val entityId = nextEntityId.getAndIncrement()
        val textDisplay = TextDisplay(
            entityId, text, location.clone(), viewers.toMutableSet(), billboard, glow, opacity, displayFlags, backgroundColor
        )

        for (viewer in viewers) {
            spawnTextDisplay(textDisplay, viewer)
        }

        activeDisplays[entityId] = textDisplay
        return textDisplay
    }

    /**
     * Shows an existing Text Display to an additional player.
     *
     * @param textDisplay The TextDisplay to show
     * @param viewer The player who should see the Text Display
     * @return `true` if the viewer was added, `false` if they could already see the display
     */
    fun addViewer(textDisplay: TextDisplay, viewer: Player): Boolean {
        if (textDisplay.viewers.add(viewer)) {
            spawnTextDisplay(textDisplay, viewer)
            return true
        }
        return false
    }

    /**
     * Hides a Text Display for a specific player.
     *
     * @param textDisplay The TextDisplay to hide
     * @param viewer The player for whom the display should be hidden
     * @return `true` if the viewer was removed, `false` if they couldn't see the display
     */
    fun removeViewer(textDisplay: TextDisplay, viewer: Player): Boolean {
        if (textDisplay.viewers.remove(viewer)) {
            destroyEntity(viewer, textDisplay.entityId)

            // If no viewers remain, remove the display completely
            if (textDisplay.viewers.isEmpty()) {
                activeDisplays.remove(textDisplay.entityId)
            }

            return true
        }
        return false
    }

    /**
     * Updates the text of an existing Text Display.
     *
     * @param textDisplay The TextDisplay to update
     * @param newText The new text to display
     */
    fun updateText(textDisplay: TextDisplay, newText: String) {
        textDisplay.text = newText

        // Send metadata update for all viewers
        val metadataPacket = createMetadataPacket(textDisplay)
        for (viewer in textDisplay.viewers) {
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadataPacket)
        }
    }

    /**
     * Updates the position of an existing Text Display.
     *
     * Note: This requires respawning the entity for all viewers, which may cause a brief flicker.
     *
     * @param textDisplay The TextDisplay to update
     * @param newLocation The new position
     */
    fun updateLocation(textDisplay: TextDisplay, newLocation: Location) {
        textDisplay.location = newLocation.clone()

        // For all viewers, respawn the display (there's no simple packet for position updates)
        for (viewer in textDisplay.viewers.toSet()) { // Create copy to avoid ConcurrentModification
            destroyEntity(viewer, textDisplay.entityId)
            spawnTextDisplay(textDisplay, viewer)
        }
    }

    /**
     * Removes a Text Display completely.
     *
     * @param textDisplay The TextDisplay to remove
     */
    fun removeTextDisplay(textDisplay: TextDisplay) {
        activeDisplays.remove(textDisplay.entityId)

        for (viewer in textDisplay.viewers.toSet()) {
            destroyEntity(viewer, textDisplay.entityId)
        }

        textDisplay.viewers.clear()
    }

    /**
     * Removes all Text Displays.
     */
    fun removeAllTextDisplays() {
        for (textDisplay in activeDisplays.values.toList()) {
            removeTextDisplay(textDisplay)
        }
    }

    /**
     * Gets a TextDisplay by its entity ID.
     *
     * @param entityId The entity ID of the TextDisplay
     * @return The TextDisplay or `null` if it doesn't exist
     */
    operator fun get(entityId: Int): TextDisplay? {
        return activeDisplays[entityId]
    }

    /**
     * Gets all active TextDisplays.
     *
     * @return A collection of all active TextDisplays
     */
    fun getAllTextDisplays(): Collection<TextDisplay> {
        return activeDisplays.values
    }

    /**
     * Checks if a specific entity ID belongs to a TextDisplay.
     *
     * @param entityId The entity ID to check
     * @return `true` if a TextDisplay with this ID exists
     */
    fun isTextDisplay(entityId: Int): Boolean {
        return activeDisplays.containsKey(entityId)
    }

    /**
     * Sends the packets to spawn a Text Display for a player.
     *
     * @param textDisplay The TextDisplay to spawn
     * @param viewer The player who should see the display
     */
    private fun spawnTextDisplay(textDisplay: TextDisplay, viewer: Player) {
        try {
            // Check if PacketEvents is initialized
            val api = PacketEvents.getAPI()
            if (api == null) {
                getLogger().error("[TextDisplayManager] PacketEvents API is not initialized. Make sure PacketEvents is loaded before creating text displays.")
                return
            }

            val playerManager = api.playerManager
            if (playerManager == null) {
                getLogger().error("[TextDisplayManager] PacketEvents player manager is not available for player ${viewer.name}")
                return
            }

            // Check if player is online
            if (!viewer.isOnline) {
                getLogger().warn("[TextDisplayManager] Attempted to spawn text display for offline player ${viewer.name}")
                return
            }

            // 1. Spawn Entity Packet
            val location = textDisplay.location
            val spawnPacket = WrapperPlayServerSpawnEntity(
                textDisplay.entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.TEXT_DISPLAY,
                Vector3d(location.x, location.y + textDisplay.yOffset, location.z),
                location.pitch,
                location.yaw,
                location.yaw,
                0,
                Optional.of(Vector3d(0.0, 0.0, 0.0))
            )

            // 2. Entity Metadata Packet
            val metadataPacket = createMetadataPacket(textDisplay)

            // Send packets
            playerManager.sendPacket(viewer, spawnPacket)
            playerManager.sendPacket(viewer, metadataPacket)

            getLogger().debug("[TextDisplayManager] Spawned text display ${textDisplay.entityId} for player ${viewer.name} at ${location.x}, ${location.y}, ${location.z}")
        } catch (e: Exception) {
            getLogger().error("[TextDisplayManager] Failed to spawn text display ${textDisplay.entityId} for player ${viewer.name}: ${e.message}", e)
        }
    }

    /**
     * Creates a metadata packet for a Text Display.
     *
     * @param textDisplay The TextDisplay
     * @return The created EntityMetadata packet
     */
    private fun createMetadataPacket(textDisplay: TextDisplay): WrapperPlayServerEntityMetadata {
        val metadataList = ArrayList<EntityData<*>>()

        // translation
        metadataList.add(EntityData(11, EntityDataTypes.VECTOR3F, textDisplay.translation))

        // scale
        metadataList.add(EntityData(12, EntityDataTypes.VECTOR3F, textDisplay.scale))

        // rotation left
        metadataList.add(EntityData(13, EntityDataTypes.QUATERNION, textDisplay.leftRotation))

        // rotation right
        metadataList.add(EntityData(14, EntityDataTypes.QUATERNION, textDisplay.rightRotation))

        // Billboard mode (index 15)
        metadataList.add(EntityData(15, EntityDataTypes.BYTE, textDisplay.billboard.billboardValue.toByte()))

        // view range
        metadataList.add(EntityData(17, EntityDataTypes.FLOAT, textDisplay.viewRange))

        // Text content (index 23) - Must be sent before other text-specific metadata
        metadataList.add(EntityData(23, EntityDataTypes.ADV_COMPONENT, text(textDisplay.text)))

        // Text width (index 24)
        metadataList.add(EntityData(24, EntityDataTypes.INT, textDisplay.width.toInt()))

        // Background color (ARGB) (index 25)
        metadataList.add(EntityData(25, EntityDataTypes.INT, textDisplay.backgroundColor))

        // Opacity (index 26) - Clamp to valid range
        val opacityValue = when {
            textDisplay.opacity < 0 -> -1.toByte()
            textDisplay.opacity > 255 -> 255.toByte()
            else -> textDisplay.opacity.toByte()
        }
        metadataList.add(EntityData(26, EntityDataTypes.BYTE, opacityValue.toByte()))

        // Display flags (index 27)
        metadataList.add(EntityData(27, EntityDataTypes.BYTE, TextDisplayFlags.calculateBitMask(textDisplay.displayFlags).toByte()))

        // Glow effect (index 22) - Note: This is a general entity metadata field, not display-specific
        // Only add if glowing is enabled
        if (textDisplay.glowing) {
            metadataList.add(EntityData(22, EntityDataTypes.INT, textDisplay.glowingInt()))
        }

        return WrapperPlayServerEntityMetadata(textDisplay.entityId, metadataList)
    }

    /**
     * Sends a Destroy-Entity packet to a player.
     *
     * @param player The player
     * @param entityId The entity ID to destroy
     */
    internal fun destroyEntity(player: Player, entityId: Int) {
        // Create packet with the entity ID
        val destroyPacket = WrapperPlayServerDestroyEntities(entityId)

        // Send packet to player
        PacketEvents.getAPI().playerManager.sendPacket(player, destroyPacket)
    }

    /**
     * Billboard modes for Text Displays.
     * Controls how the text display rotates relative to the viewer.
     */
    enum class TextDisplayBillboard(val billboardValue: Int) {
        /**
         * Fixed orientation - doesn't rotate, always faces the same direction.
         */
        FIXED(0),

        /**
         * Vertical rotation - rotates around Y axis to face the player horizontally.
         */
        VERTICAL(1),

        /**
         * Horizontal rotation - rotates around X axis to face the player vertically.
         */
        HORIZONTAL(2),

        /**
         * Center rotation - rotates to fully face the player (both axes).
         */
        CENTER(3)
    }

    /**
     * Represents a Text Display entity.
     *
     * @property entityId The unique entity ID assigned to this display
     * @property text The text content to display
     * @property location The current location of the display
     * @property viewers The set of players who can currently see this display
     * @property billboard The billboard mode for rotation behavior
     * @property glowing Whether the display has a glow effect
     * @property opacity The opacity of the text (0-255, -1 for default)
     * @property displayFlags The display flags applied to this display
     * @property backgroundColor The background color in ARGB format
     */
    class TextDisplay(
        val entityId: Int,
        var text: String,
        var location: Location,
        val viewers: MutableSet<Player>,
        val billboard: TextDisplayBillboard,
        val glowing: Boolean = false,
        val opacity: Int = -1,
        val displayFlags: List<TextDisplayFlags>,
        val backgroundColor: Int = 0x00000000,
        val scale: Vector3f = Vector3f(1f, 1f, 1f),
        val viewRange: Float = 1f,
        val translation: Vector3f = Vector3f(0f, 0f, 0f),
        val leftRotation: Quaternion4f = Quaternion4f(0f, 0f, 0f, 1f),
        val rightRotation: Quaternion4f = Quaternion4f(0f, 0f, 0f, 1f),
    ) {
        /**
         * Vertical offset from the base location.
         * How far above the ground/position the display should appear.
         */
        var yOffset: Double = 0.0

        /**
         * Maximum text width in pixels.
         * Text will wrap if it exceeds this width.
         */
        var width: Float = 200f

        /**
         * Sets the text of the display.
         * Shortcut for [TextDisplayManager.updateText].
         *
         * @param manager The manager instance to use for the update
         * @param newText The new text to display
         */
        fun setText(manager: TextDisplayManager, newText: String) {
            manager.updateText(this, newText)
        }

        /**
         * Gets the glow effect value for the entity metadata.
         *
         * @return 10 if glowing, -1 otherwise
         */
        fun glowingInt(): Int {
            return if (glowing) {
                10
            } else {
                -1
            }
        }

        /**
         * Moves the display to a new position.
         * Shortcut for [TextDisplayManager.updateLocation].
         *
         * @param manager The manager instance to use for the update
         * @param newLocation The new position
         */
        fun moveToLocation(manager: TextDisplayManager, newLocation: Location) {
            manager.updateLocation(this, newLocation)
        }

        /**
         * Adds a new viewer.
         * Shortcut for [TextDisplayManager.addViewer].
         *
         * @param manager The manager instance to use
         * @param viewer The player to add as a viewer
         * @return `true` if the viewer was added, `false` if they could already see it
         */
        fun addViewer(manager: TextDisplayManager, viewer: Player): Boolean {
            return manager.addViewer(this, viewer)
        }

        /**
         * Removes a viewer.
         * Shortcut for [TextDisplayManager.removeViewer].
         *
         * @param manager The manager instance to use
         * @param viewer The player to remove as a viewer
         * @return `true` if the viewer was removed, `false` if they couldn't see it
         */
        fun removeViewer(manager: TextDisplayManager, viewer: Player): Boolean {
            return manager.removeViewer(this, viewer)
        }

        /**
         * Removes this display.
         * Shortcut for [TextDisplayManager.removeTextDisplay].
         *
         * @param manager The manager instance to use
         */
        fun remove(manager: TextDisplayManager) {
            manager.removeTextDisplay(this)
        }

        /**
         * Checks if the display is visible to a specific player.
         *
         * @param player The player to check
         * @return `true` if the player can see the display
         */
        fun isVisibleTo(player: Player): Boolean {
            return player in viewers
        }
    }
}

