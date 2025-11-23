package cc.modlabs.kpaper.visuals.display

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemType
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A manager class for creating and managing packet-based Item Display entities in Minecraft.
 *
 * Item Displays are client-side entities that can display items at specific locations.
 * This manager handles the lifecycle of these displays, including creation, updates, and removal.
 *
 * @example
 * ```kotlin
 * val manager = ItemDisplayManager()
 * val display = manager.createItemDisplay(
 *     item = ItemStack(Material.DIAMOND_SWORD),
 *     location = player.location,
 *     viewer = player,
 *     billboard = ItemDisplayManager.ItemDisplayBillboard.CENTER,
 *     glow = true,
 *     scale = Vector3f(1.0f, 1.0f, 1.0f),
 *     displayTransformation = ItemDisplayManager.ItemDisplayTransformation.GROUND
 * )
 * ```
 */
class ItemDisplayManager {

    /**
     * Stores all active Item Displays, mapped by Entity ID.
     */
    private val activeDisplays = ConcurrentHashMap<Int, ItemDisplay>()

    /**
     * Counter for generating unique Entity IDs.
     * Starts at 2000000 to avoid conflicts with regular entities and Text Displays.
     */
    private val nextEntityId = AtomicInteger(2000000)

    /**
     * Creates an Item Display for a specific player.
     *
     * @param item The item to display
     * @param location The position of the Item Display
     * @param viewer The player who should see the Item Display
     * @param billboard The billboard mode for the display
     * @param glow Whether the display should glow (outline effect)
     * @param scale The scale of the item (default: 0.5f, 0.5f, 0.5f)
     * @param displayTransformation How the item should be displayed (default: NONE)
     * @return The created ItemDisplay object
     */
    fun createItemDisplay(
        item: ItemStack,
        location: Location,
        viewer: Player,
        billboard: ItemDisplayBillboard,
        glow: Boolean = false,
        scale: Vector3f = Vector3f(0.5f, 0.5f, 0.5f),
        displayTransformation: ItemDisplayTransformation = ItemDisplayTransformation.NONE
    ): ItemDisplay {
        val entityId = nextEntityId.getAndIncrement()
        val itemType = getItemType(item.type)
        val itemDisplay = ItemDisplay(
            entityId, item, location.clone(), mutableSetOf(viewer), billboard, glow, itemType, scale, displayTransformation
        )

        spawnItemDisplay(itemDisplay, viewer)
        activeDisplays[entityId] = itemDisplay

        return itemDisplay
    }

    /**
     * Creates an Item Display for multiple players.
     *
     * @param item The item to display
     * @param location The position of the Item Display
     * @param viewers The players who should see the Item Display
     * @param billboard The billboard mode for the display
     * @param glow Whether the display should glow (outline effect)
     * @param scale The scale of the item (default: 1.0f, 1.0f, 1.0f)
     * @param displayTransformation How the item should be displayed (default: NONE)
     * @return The created ItemDisplay object
     */
    fun createItemDisplay(
        item: ItemStack,
        location: Location,
        viewers: Collection<Player>,
        billboard: ItemDisplayBillboard,
        glow: Boolean = false,
        scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f),
        displayTransformation: ItemDisplayTransformation = ItemDisplayTransformation.NONE
    ): ItemDisplay {
        val entityId = nextEntityId.getAndIncrement()
        val itemType = getItemType(item.type)
        val itemDisplay = ItemDisplay(
            entityId, item, location.clone(), viewers.toMutableSet(), billboard, glow, itemType, scale, displayTransformation
        )

        for (viewer in viewers) {
            spawnItemDisplay(itemDisplay, viewer)
        }

        activeDisplays[entityId] = itemDisplay
        return itemDisplay
    }

    /**
     * Shows an existing Item Display to an additional player.
     *
     * @param itemDisplay The ItemDisplay to show
     * @param viewer The player who should see the Item Display
     * @return `true` if the viewer was added, `false` if they could already see the display
     */
    fun addViewer(itemDisplay: ItemDisplay, viewer: Player): Boolean {
        if (itemDisplay.viewers.add(viewer)) {
            spawnItemDisplay(itemDisplay, viewer)
            return true
        }
        return false
    }

    /**
     * Hides an Item Display for a specific player.
     *
     * @param itemDisplay The ItemDisplay to hide
     * @param viewer The player for whom the display should be hidden
     * @return `true` if the viewer was removed, `false` if they couldn't see the display
     */
    fun removeViewer(itemDisplay: ItemDisplay, viewer: Player): Boolean {
        if (itemDisplay.viewers.remove(viewer)) {
            destroyEntity(viewer, itemDisplay.entityId)

            // If no viewers remain, remove the display completely
            if (itemDisplay.viewers.isEmpty()) {
                activeDisplays.remove(itemDisplay.entityId)
            }

            return true
        }
        return false
    }

    /**
     * Updates the item of an existing Item Display.
     *
     * @param itemDisplay The ItemDisplay to update
     * @param newItem The new item to display
     */
    fun updateItem(itemDisplay: ItemDisplay, newItem: ItemStack) {
        itemDisplay.item = newItem
        itemDisplay.itemType = getItemType(newItem.type)

        // Send metadata update for all viewers
        val metadataPacket = createMetadataPacket(itemDisplay)
        for (viewer in itemDisplay.viewers) {
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadataPacket)
        }
    }

    /**
     * Updates the scale of an existing Item Display.
     *
     * @param itemDisplay The ItemDisplay to update
     * @param newScale The new scale
     */
    fun updateScale(itemDisplay: ItemDisplay, newScale: Vector3f) {
        itemDisplay.scale = newScale

        val metadataPacket = createMetadataPacket(itemDisplay)
        for (viewer in itemDisplay.viewers) {
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadataPacket)
        }
    }

    /**
     * Updates the transformation type of an existing Item Display.
     *
     * @param itemDisplay The ItemDisplay to update
     * @param newTransformation The new transformation type
     */
    fun updateTransformation(itemDisplay: ItemDisplay, newTransformation: ItemDisplayTransformation) {
        itemDisplay.displayTransformation = newTransformation

        val metadataPacket = createMetadataPacket(itemDisplay)
        for (viewer in itemDisplay.viewers) {
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadataPacket)
        }
    }

    /**
     * Updates the position of an existing Item Display.
     *
     * Note: This requires respawning the entity for all viewers, which may cause a brief flicker.
     *
     * @param itemDisplay The ItemDisplay to update
     * @param newLocation The new position
     */
    fun updateLocation(itemDisplay: ItemDisplay, newLocation: Location) {
        itemDisplay.location = newLocation.clone()

        // For all viewers, respawn the display (there's no simple packet for position updates)
        for (viewer in itemDisplay.viewers.toSet()) { // Create copy to avoid ConcurrentModification
            destroyEntity(viewer, itemDisplay.entityId)
            spawnItemDisplay(itemDisplay, viewer)
        }
    }

    /**
     * Removes an Item Display completely.
     *
     * @param itemDisplay The ItemDisplay to remove
     */
    fun removeItemDisplay(itemDisplay: ItemDisplay) {
        activeDisplays.remove(itemDisplay.entityId)

        for (viewer in itemDisplay.viewers.toSet()) {
            destroyEntity(viewer, itemDisplay.entityId)
        }

        itemDisplay.viewers.clear()
    }

    /**
     * Removes all Item Displays.
     */
    fun removeAllItemDisplays() {
        for (itemDisplay in activeDisplays.values.toList()) {
            removeItemDisplay(itemDisplay)
        }
    }

    /**
     * Gets an ItemDisplay by its entity ID.
     *
     * @param entityId The entity ID of the ItemDisplay
     * @return The ItemDisplay or `null` if it doesn't exist
     */
    operator fun get(entityId: Int): ItemDisplay? {
        return activeDisplays[entityId]
    }

    /**
     * Gets all active ItemDisplays.
     *
     * @return A collection of all active ItemDisplays
     */
    fun getAllItemDisplays(): Collection<ItemDisplay> {
        return activeDisplays.values
    }

    /**
     * Checks if a specific entity ID belongs to an ItemDisplay.
     *
     * @param entityId The entity ID to check
     * @return `true` if an ItemDisplay with this ID exists
     */
    fun isItemDisplay(entityId: Int): Boolean {
        return activeDisplays.containsKey(entityId)
    }

    /**
     * Converts a Bukkit Material to a PacketEvents ItemType.
     *
     * @param material The Bukkit Material
     * @return The corresponding PacketEvents ItemType
     */
    private fun getItemType(material: Material): ItemType = ItemTypes.getByName(material.name) ?: ItemTypes.AIR

    /**
     * Converts a Bukkit ItemStack to a PacketEvents ItemStack.
     *
     * @param bukkitItem The Bukkit ItemStack
     * @return The PacketEvents ItemStack
     */
    private fun toPacketItemStack(bukkitItem: ItemStack): PacketItemStack {
        val itemType = getItemType(bukkitItem.type)
        return PacketItemStack.builder()
            .type(itemType)
            .amount(bukkitItem.amount)
            .build()
    }

    /**
     * Sends the packets to spawn an Item Display for a player.
     *
     * @param itemDisplay The ItemDisplay to spawn
     * @param viewer The player who should see the display
     */
    private fun spawnItemDisplay(itemDisplay: ItemDisplay, viewer: Player) {
        try {
            // 1. Spawn Entity Packet
            val location = itemDisplay.location
            val spawnPacket = WrapperPlayServerSpawnEntity(
                itemDisplay.entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ITEM_DISPLAY,
                Vector3d(location.x, location.y + itemDisplay.yOffset, location.z),
                location.pitch,
                location.yaw,
                location.yaw,
                0,
                Optional.of(Vector3d(0.0, 0.0, 0.0))
            )

            // 2. Entity Metadata Packet
            val metadataPacket = createMetadataPacket(itemDisplay)

            // Send packets
            PacketEvents.getAPI().playerManager.sendPacket(viewer, spawnPacket)
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadataPacket)
        } catch (e: Exception) {
            // In case of an error, log it instead of crashing the plugin
            e.printStackTrace()
        }
    }

    /**
     * Creates a metadata packet for an Item Display.
     *
     * @param itemDisplay The ItemDisplay
     * @return The created EntityMetadata packet
     */
    private fun createMetadataPacket(itemDisplay: ItemDisplay): WrapperPlayServerEntityMetadata {
        val metadataList = ArrayList<EntityData<*>>()

        // Billboard mode (index 15)
        metadataList.add(EntityData(15, EntityDataTypes.BYTE, itemDisplay.billboard.billboardValue.toByte()))

        // Glow effect (index 22)
        metadataList.add(EntityData(22, EntityDataTypes.INT, itemDisplay.glowingInt()))

        // The item itself (index 23)
        val packetItem = toPacketItemStack(itemDisplay.item)
        metadataList.add(EntityData(23, EntityDataTypes.ITEMSTACK, packetItem))

        // Display transformation (index 24) - how the item is displayed
        metadataList.add(EntityData(24, EntityDataTypes.BYTE, itemDisplay.displayTransformation.value.toByte()))

        // Scale of the item (index 12)
        metadataList.add(EntityData(12, EntityDataTypes.VECTOR3F, itemDisplay.scale))

        return WrapperPlayServerEntityMetadata(itemDisplay.entityId, metadataList)
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
     * Billboard modes for Item Displays.
     * Controls how the item display rotates relative to the viewer.
     */
    enum class ItemDisplayBillboard(val billboardValue: Int) {
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
     * Display transformation modes for Item Displays.
     * Controls how the item is rendered (perspective, position, etc.).
     */
    enum class ItemDisplayTransformation(val value: Int) {
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
         * Ground/dropped item perspective.
         */
        GROUND(7),

        /**
         * Fixed perspective.
         */
        FIXED(8)
    }

    /**
     * Represents an Item Display entity.
     *
     * @property entityId The unique entity ID assigned to this display
     * @property item The item being displayed
     * @property location The current location of the display
     * @property viewers The set of players who can currently see this display
     * @property billboard The billboard mode for rotation behavior
     * @property glowing Whether the display has a glow effect
     * @property itemType The PacketEvents ItemType (internal use)
     * @property scale The scale of the item display
     * @property displayTransformation How the item is displayed
     */
    class ItemDisplay(
        val entityId: Int,
        var item: ItemStack,
        var location: Location,
        val viewers: MutableSet<Player>,
        val billboard: ItemDisplayBillboard,
        val glowing: Boolean = false,
        var itemType: ItemType,
        var scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f),
        var displayTransformation: ItemDisplayTransformation = ItemDisplayTransformation.NONE
    ) {
        /**
         * Vertical offset from the base location.
         * How far above the ground/position the display should appear.
         */
        var yOffset: Double = 0.0

        /**
         * Sets the item of the display.
         * Shortcut for [ItemDisplayManager.updateItem].
         *
         * @param manager The manager instance to use for the update
         * @param newItem The new item to display
         */
        fun setItem(manager: ItemDisplayManager, newItem: ItemStack) {
            manager.updateItem(this, newItem)
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
         * Shortcut for [ItemDisplayManager.updateScale].
         *
         * @param manager The manager instance to use for the update
         * @param newScale The new scale
         */
        fun setScale(manager: ItemDisplayManager, newScale: Vector3f) {
            manager.updateScale(this, newScale)
        }

        /**
         * Changes the transformation type of the display.
         * Shortcut for [ItemDisplayManager.updateTransformation].
         *
         * @param manager The manager instance to use for the update
         * @param newTransformation The new transformation type
         */
        fun setTransformation(manager: ItemDisplayManager, newTransformation: ItemDisplayTransformation) {
            manager.updateTransformation(this, newTransformation)
        }

        /**
         * Moves the display to a new position.
         * Shortcut for [ItemDisplayManager.updateLocation].
         *
         * @param manager The manager instance to use for the update
         * @param newLocation The new position
         */
        fun moveToLocation(manager: ItemDisplayManager, newLocation: Location) {
            manager.updateLocation(this, newLocation)
        }

        /**
         * Adds a new viewer.
         * Shortcut for [ItemDisplayManager.addViewer].
         *
         * @param manager The manager instance to use
         * @param viewer The player to add as a viewer
         * @return `true` if the viewer was added, `false` if they could already see it
         */
        fun addViewer(manager: ItemDisplayManager, viewer: Player): Boolean {
            return manager.addViewer(this, viewer)
        }

        /**
         * Removes a viewer.
         * Shortcut for [ItemDisplayManager.removeViewer].
         *
         * @param manager The manager instance to use
         * @param viewer The player to remove as a viewer
         * @return `true` if the viewer was removed, `false` if they couldn't see it
         */
        fun removeViewer(manager: ItemDisplayManager, viewer: Player): Boolean {
            return manager.removeViewer(this, viewer)
        }

        /**
         * Removes this display.
         * Shortcut for [ItemDisplayManager.removeItemDisplay].
         *
         * @param manager The manager instance to use
         */
        fun remove(manager: ItemDisplayManager) {
            manager.removeItemDisplay(this)
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

