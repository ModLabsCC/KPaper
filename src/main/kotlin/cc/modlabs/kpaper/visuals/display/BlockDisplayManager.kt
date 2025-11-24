package cc.modlabs.kpaper.visuals.display

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A manager class for creating and managing packet-based Block Display entities in Minecraft.
 *
 * Block Displays are client-side entities that can display blocks at specific locations.
 * This manager handles the lifecycle of these displays, including creation, updates, and removal.
 *
 * @example
 * ```kotlin
 * val manager = BlockDisplayManager()
 * val display = manager.createBlockDisplay(
 *     blockData = Bukkit.createBlockData(Material.DIAMOND_BLOCK),
 *     location = player.location,
 *     viewer = player,
 *     billboard = BlockDisplayManager.BlockDisplayBillboard.CENTER,
 *     glow = true,
 *     scale = Vector3f(1.0f, 1.0f, 1.0f),
 *     displayTransformation = BlockDisplayManager.BlockDisplayTransformation.NONE
 * )
 * ```
 */
class BlockDisplayManager {

    /**
     * Stores all active Block Displays, mapped by Entity ID.
     */
    private val activeDisplays = ConcurrentHashMap<Int, BlockDisplay>()

    /**
     * Counter for generating unique Entity IDs.
     * Starts at 3000000 to avoid conflicts with regular entities, Text Displays, and Item Displays.
     */
    private val nextEntityId = AtomicInteger(3000000)

    /**
     * Creates a Block Display for a specific player.
     *
     * @param blockData The block data to display
     * @param location The position of the Block Display
     * @param viewer The player who should see the Block Display
     * @param billboard The billboard mode for the display
     * @param glow Whether the display should glow (outline effect)
     * @param scale The scale of the block (default: 1.0f, 1.0f, 1.0f)
     * @param displayTransformation How the block should be displayed (default: NONE)
     * @return The created BlockDisplay object
     */
    fun createBlockDisplay(
        blockData: BlockData,
        location: Location,
        viewer: Player,
        billboard: BlockDisplayBillboard,
        glow: Boolean = false,
        scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f),
        displayTransformation: BlockDisplayTransformation = BlockDisplayTransformation.NONE
    ): BlockDisplay {
        val entityId = nextEntityId.getAndIncrement()
        val blockDisplay = BlockDisplay(
            entityId, blockData, location.clone(), mutableSetOf(viewer), billboard, glow, scale, displayTransformation
        )

        spawnBlockDisplay(blockDisplay, viewer)
        activeDisplays[entityId] = blockDisplay

        return blockDisplay
    }

    /**
     * Creates a Block Display for multiple players.
     *
     * @param blockData The block data to display
     * @param location The position of the Block Display
     * @param viewers The players who should see the Block Display
     * @param billboard The billboard mode for the display
     * @param glow Whether the display should glow (outline effect)
     * @param scale The scale of the block (default: 1.0f, 1.0f, 1.0f)
     * @param displayTransformation How the block should be displayed (default: NONE)
     * @return The created BlockDisplay object
     */
    fun createBlockDisplay(
        blockData: BlockData,
        location: Location,
        viewers: Collection<Player>,
        billboard: BlockDisplayBillboard,
        glow: Boolean = false,
        scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f),
        displayTransformation: BlockDisplayTransformation = BlockDisplayTransformation.NONE
    ): BlockDisplay {
        val entityId = nextEntityId.getAndIncrement()
        val blockDisplay = BlockDisplay(
            entityId, blockData, location.clone(), viewers.toMutableSet(), billboard, glow, scale, displayTransformation
        )

        for (viewer in viewers) {
            spawnBlockDisplay(blockDisplay, viewer)
        }

        activeDisplays[entityId] = blockDisplay
        return blockDisplay
    }

    /**
     * Shows an existing Block Display to an additional player.
     *
     * @param blockDisplay The BlockDisplay to show
     * @param viewer The player who should see the Block Display
     * @return `true` if the viewer was added, `false` if they could already see the display
     */
    fun addViewer(blockDisplay: BlockDisplay, viewer: Player): Boolean {
        if (blockDisplay.viewers.add(viewer)) {
            spawnBlockDisplay(blockDisplay, viewer)
            return true
        }
        return false
    }

    /**
     * Hides a Block Display for a specific player.
     *
     * @param blockDisplay The BlockDisplay to hide
     * @param viewer The player for whom the display should be hidden
     * @return `true` if the viewer was removed, `false` if they couldn't see the display
     */
    fun removeViewer(blockDisplay: BlockDisplay, viewer: Player): Boolean {
        if (blockDisplay.viewers.remove(viewer)) {
            destroyEntity(viewer, blockDisplay.entityId)

            // If no viewers remain, remove the display completely
            if (blockDisplay.viewers.isEmpty()) {
                activeDisplays.remove(blockDisplay.entityId)
            }

            return true
        }
        return false
    }

    /**
     * Updates the block data of an existing Block Display.
     *
     * @param blockDisplay The BlockDisplay to update
     * @param newBlockData The new block data to display
     */
    fun updateBlock(blockDisplay: BlockDisplay, newBlockData: BlockData) {
        blockDisplay.blockData = newBlockData

        // Send metadata update for all viewers
        val metadataPacket = createMetadataPacket(blockDisplay)
        for (viewer in blockDisplay.viewers) {
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadataPacket)
        }
    }

    /**
     * Updates the scale of an existing Block Display.
     *
     * @param blockDisplay The BlockDisplay to update
     * @param newScale The new scale
     */
    fun updateScale(blockDisplay: BlockDisplay, newScale: Vector3f) {
        blockDisplay.scale = newScale

        val metadataPacket = createMetadataPacket(blockDisplay)
        for (viewer in blockDisplay.viewers) {
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadataPacket)
        }
    }

    /**
     * Updates the transformation type of an existing Block Display.
     *
     * @param blockDisplay The BlockDisplay to update
     * @param newTransformation The new transformation type
     */
    fun updateTransformation(blockDisplay: BlockDisplay, newTransformation: BlockDisplayTransformation) {
        blockDisplay.displayTransformation = newTransformation

        val metadataPacket = createMetadataPacket(blockDisplay)
        for (viewer in blockDisplay.viewers) {
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadataPacket)
        }
    }

    /**
     * Updates the position of an existing Block Display.
     *
     * Note: This requires respawning the entity for all viewers, which may cause a brief flicker.
     *
     * @param blockDisplay The BlockDisplay to update
     * @param newLocation The new position
     */
    fun updateLocation(blockDisplay: BlockDisplay, newLocation: Location) {
        blockDisplay.location = newLocation.clone()

        // For all viewers, respawn the display (there's no simple packet for position updates)
        for (viewer in blockDisplay.viewers.toSet()) { // Create copy to avoid ConcurrentModification
            destroyEntity(viewer, blockDisplay.entityId)
            spawnBlockDisplay(blockDisplay, viewer)
        }
    }

    /**
     * Removes a Block Display completely.
     *
     * @param blockDisplay The BlockDisplay to remove
     */
    fun removeBlockDisplay(blockDisplay: BlockDisplay) {
        activeDisplays.remove(blockDisplay.entityId)

        for (viewer in blockDisplay.viewers.toSet()) {
            destroyEntity(viewer, blockDisplay.entityId)
        }

        blockDisplay.viewers.clear()
    }

    /**
     * Removes all Block Displays.
     */
    fun removeAllBlockDisplays() {
        for (blockDisplay in activeDisplays.values.toList()) {
            removeBlockDisplay(blockDisplay)
        }
    }

    /**
     * Gets a BlockDisplay by its entity ID.
     *
     * @param entityId The entity ID of the BlockDisplay
     * @return The BlockDisplay or `null` if it doesn't exist
     */
    operator fun get(entityId: Int): BlockDisplay? {
        return activeDisplays[entityId]
    }

    /**
     * Gets all active BlockDisplays.
     *
     * @return A collection of all active BlockDisplays
     */
    fun getAllBlockDisplays(): Collection<BlockDisplay> {
        return activeDisplays.values
    }

    /**
     * Checks if a specific entity ID belongs to a BlockDisplay.
     *
     * @param entityId The entity ID to check
     * @return `true` if a BlockDisplay with this ID exists
     */
    fun isBlockDisplay(entityId: Int): Boolean {
        return activeDisplays.containsKey(entityId)
    }

    /**
     * Sends the packets to spawn a Block Display for a player.
     *
     * @param blockDisplay The BlockDisplay to spawn
     * @param viewer The player who should see the display
     */
    private fun spawnBlockDisplay(blockDisplay: BlockDisplay, viewer: Player) {
        try {
            // 1. Spawn Entity Packet
            val location = blockDisplay.location
            val spawnPacket = WrapperPlayServerSpawnEntity(
                blockDisplay.entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.BLOCK_DISPLAY,
                Vector3d(location.x, location.y + blockDisplay.yOffset, location.z),
                location.pitch.toFloat(),
                location.yaw.toFloat(),
                location.yaw.toFloat(),
                0,
                Optional.of(Vector3d(0.0, 0.0, 0.0))
            )

            // 2. Entity Metadata Packet
            val metadataPacket = createMetadataPacket(blockDisplay)

            // Send packets
            PacketEvents.getAPI().playerManager.sendPacket(viewer, spawnPacket)
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadataPacket)
        } catch (e: Exception) {
            // In case of an error, log it instead of crashing the plugin
            e.printStackTrace()
        }
    }

    /**
     * Creates a metadata packet for a Block Display.
     *
     * @param blockDisplay The BlockDisplay
     * @return The created EntityMetadata packet
     */
    private fun createMetadataPacket(blockDisplay: BlockDisplay): WrapperPlayServerEntityMetadata {
        val metadataList = ArrayList<EntityData<*>>()

        // Billboard mode (index 15)
        metadataList.add(EntityData(15, EntityDataTypes.BYTE, blockDisplay.billboard.billboardValue.toByte()))

        // Glow effect (index 22)
        metadataList.add(EntityData(22, EntityDataTypes.INT, blockDisplay.glowingInt()))

        // The block state itself - Conversion from Bukkit BlockData to WrappedBlockState
        val blockState = WrappedBlockState.getByString(blockDisplay.blockData.asString)
        metadataList.add(EntityData(23, EntityDataTypes.BLOCK_STATE, blockState.typeData.ordinal))

        // Display transformation (index 24) - how the block is displayed
        metadataList.add(EntityData(24, EntityDataTypes.BYTE, blockDisplay.displayTransformation.value.toByte()))

        // Scale of the block (index 12)
        metadataList.add(EntityData(12, EntityDataTypes.VECTOR3F, blockDisplay.scale))

        return WrapperPlayServerEntityMetadata(blockDisplay.entityId, metadataList)
    }

    /**
     * Sends a Destroy-Entity packet to a player.
     *
     * @param player The player
     * @param entityId The entity ID to destroy
     */
    internal fun destroyEntity(player: Player, entityId: Int) {
        val destroyPacket = WrapperPlayServerDestroyEntities(entityId)
        PacketEvents.getAPI().playerManager.sendPacket(player, destroyPacket)
    }

    /**
     * Billboard modes for Block Displays.
     * Controls how the block display rotates relative to the viewer.
     */
    enum class BlockDisplayBillboard(val billboardValue: Int) {
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
     * Display transformation modes for Block Displays.
     * Controls how the block is rendered (perspective, position, etc.).
     */
    enum class BlockDisplayTransformation(val value: Int) {
        /**
         * No transformation - default display.
         */
        NONE(0),

        /**
         * Third person left hand perspective.
         */
        THIRDPERSON_LEFTHAND(1),

        /**
         * Third person right hand perspective.
         */
        THIRDPERSON_RIGHTHAND(2),

        /**
         * First person left hand perspective.
         */
        FIRSTPERSON_LEFTHAND(3),

        /**
         * First person right hand perspective.
         */
        FIRSTPERSON_RIGHTHAND(4),

        /**
         * Head/helmet perspective.
         */
        HEAD(5),

        /**
         * GUI/inventory perspective.
         */
        GUI(6),

        /**
         * Ground/dropped block perspective.
         */
        GROUND(7),

        /**
         * Fixed perspective.
         */
        FIXED(8)
    }

    /**
     * Represents a Block Display entity.
     *
     * @property entityId The unique entity ID assigned to this display
     * @property blockData The block data being displayed
     * @property location The current location of the display
     * @property viewers The set of players who can currently see this display
     * @property billboard The billboard mode for rotation behavior
     * @property glowing Whether the display has a glow effect
     * @property scale The scale of the block display
     * @property displayTransformation How the block is displayed
     */
    class BlockDisplay(
        val entityId: Int,
        var blockData: BlockData,
        var location: Location,
        val viewers: MutableSet<Player>,
        val billboard: BlockDisplayBillboard,
        val glowing: Boolean = false,
        var scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f),
        var displayTransformation: BlockDisplayTransformation = BlockDisplayTransformation.NONE
    ) {
        /**
         * Vertical offset from the base location.
         * How far above the ground/position the display should appear.
         */
        var yOffset: Double = 0.0

        /**
         * Sets the block data of the display.
         * Shortcut for [BlockDisplayManager.updateBlock].
         *
         * @param manager The manager instance to use for the update
         * @param newBlockData The new block data to display
         */
        fun setBlock(manager: BlockDisplayManager, newBlockData: BlockData) {
            manager.updateBlock(this, newBlockData)
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
         * Changes the scale of the display.
         * Shortcut for [BlockDisplayManager.updateScale].
         *
         * @param manager The manager instance to use for the update
         * @param newScale The new scale
         */
        fun setScale(manager: BlockDisplayManager, newScale: Vector3f) {
            manager.updateScale(this, newScale)
        }

        /**
         * Changes the transformation type of the display.
         * Shortcut for [BlockDisplayManager.updateTransformation].
         *
         * @param manager The manager instance to use for the update
         * @param newTransformation The new transformation type
         */
        fun setTransformation(manager: BlockDisplayManager, newTransformation: BlockDisplayTransformation) {
            manager.updateTransformation(this, newTransformation)
        }

        /**
         * Moves the display to a new position.
         * Shortcut for [BlockDisplayManager.updateLocation].
         *
         * @param manager The manager instance to use for the update
         * @param newLocation The new position
         */
        fun moveToLocation(manager: BlockDisplayManager, newLocation: Location) {
            manager.updateLocation(this, newLocation)
        }

        /**
         * Adds a new viewer.
         * Shortcut for [BlockDisplayManager.addViewer].
         *
         * @param manager The manager instance to use
         * @param viewer The player to add as a viewer
         * @return `true` if the viewer was added, `false` if they could already see it
         */
        fun addViewer(manager: BlockDisplayManager, viewer: Player): Boolean {
            return manager.addViewer(this, viewer)
        }

        /**
         * Removes a viewer.
         * Shortcut for [BlockDisplayManager.removeViewer].
         *
         * @param manager The manager instance to use
         * @param viewer The player to remove as a viewer
         * @return `true` if the viewer was removed, `false` if they couldn't see it
         */
        fun removeViewer(manager: BlockDisplayManager, viewer: Player): Boolean {
            return manager.removeViewer(this, viewer)
        }

        /**
         * Removes this display.
         * Shortcut for [BlockDisplayManager.removeBlockDisplay].
         *
         * @param manager The manager instance to use
         */
        fun remove(manager: BlockDisplayManager) {
            manager.removeBlockDisplay(this)
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

