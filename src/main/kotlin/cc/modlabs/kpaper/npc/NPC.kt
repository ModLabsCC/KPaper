package cc.modlabs.kpaper.npc

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Mannequin
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

// Import the actual types from Paper API
import io.papermc.paper.datacomponent.item.ResolvableProfile
import com.destroystokyo.paper.SkinParts
import org.bukkit.inventory.MainHand

// Type aliases for cleaner API
typealias MannequinProfile = ResolvableProfile
typealias MannequinSkinParts = SkinParts.Mutable

interface NPC {

    /**
     * Commands the NPC to walk to the specified location.
     *
     * @param location The destination location to which the NPC should walk.
     * @return True if the NPC successfully begins walking to the location, false otherwise.
     */
    fun walkTo(location: Location): Boolean

    /**
     * Commands the NPC to follow a specified path by walking sequentially through a list of locations.
     *
     * @param locations The list of locations that defines the path the NPC should follow.
     * @return True if the NPC successfully begins following the path, false otherwise.
     */
    fun walkPath(locations: List<Location>): Boolean

    /**
     * Temporarily pauses the NPC's walking action if it is currently in progress.
     *
     * @return True if the walking action is successfully paused, false otherwise.
     */
    fun pauseWalking(): Boolean

    /**
     * Resumes the NPC's walking action if it was previously paused.
     *
     * @return True if the walking action is successfully resumed, false otherwise.
     */
    fun resumeWalking(): Boolean

    /**
     * Initiates the walking behavior for the NPC.
     *
     * @return True if the walking action is successfully started, false otherwise.
     */
    fun startWalking(): Boolean

    /**
     * Starts patrolling a path. When the NPC reaches the last location, it will loop back to the first location.
     *
     * @param locations The list of locations that defines the patrol path.
     * @return True if patrolling successfully starts, false otherwise.
     */
    fun startPatrolling(locations: List<Location>): Boolean

    /**
     * Pauses the patrolling behavior if it is currently active.
     *
     * @return True if patrolling is successfully paused, false otherwise.
     */
    fun pausePatrolling(): Boolean

    /**
     * Resumes the patrolling behavior if it was previously paused.
     *
     * @return True if patrolling is successfully resumed, false otherwise.
     */
    fun resumePatrolling(): Boolean

    /**
     * Stops the patrolling behavior. The NPC will stop at its current location.
     *
     * @return True if patrolling is successfully stopped, false otherwise.
     */
    fun stopPatrolling(): Boolean

    /**
     * Instantly moves the NPC to the specified location without walking.
     *
     * @param location The destination location to which the NPC should be teleported.
     * @return True if the teleportation is successfully executed, false otherwise.
     */
    fun teleport(location: Location): Boolean

    /**
     * Changes the name of the NPC to the specified value.
     *
     * @param name The new name to assign to the NPC.
     */
    fun changeName(name: String)

    /**
     * Gets the underlying entity for this NPC.
     * For mannequin-based NPCs, this will return a [Mannequin] instance.
     *
     * @return The underlying entity, or null if the entity is no longer valid.
     */
    fun getEntity(): org.bukkit.entity.Entity?

    /**
     * Gets the underlying Mannequin entity.
     *
     * @return The Mannequin entity, or null if the entity is no longer valid.
     */
    fun getMannequin(): Mannequin?

    /**
     * Gets the resolvable profile for this mannequin.
     * The profile determines the appearance (skin) of the mannequin.
     *
     * @return The resolvable profile for this mannequin.
     */
    fun getProfile(): MannequinProfile

    /**
     * Sets the resolvable profile for this mannequin.
     * The profile determines the appearance (skin) of the mannequin.
     *
     * @param profile The new resolvable profile.
     */
    fun setProfile(profile: MannequinProfile)

    /**
     * Gets a copy of the current skin part options for this mannequin.
     * Skin parts control which parts of the player model are visible (cape, jacket, left sleeve, etc.).
     *
     * @return A mutable copy of the current skin part options.
     */
    fun getSkinParts(): MannequinSkinParts

    /**
     * Sets the skin part options for this mannequin.
     * Skin parts control which parts of the player model are visible (cape, jacket, left sleeve, etc.).
     *
     * @param parts The new skin part options.
     */
    fun setSkinParts(parts: MannequinSkinParts)

    /**
     * Gets the description text for this mannequin (appears below the name).
     *
     * @return The description, or null if none is set.
     */
    fun getDescription(): Component?

    /**
     * Sets the description text for this mannequin (appears below the name).
     * Setting the description to null will remove it.
     *
     * @param description The new description, or null to remove it.
     */
    fun setDescription(description: Component?)

    /**
     * Sets whether the custom name is visible.
     *
     * @param visible Whether the custom name should be visible.
     */
    fun setCustomNameVisible(visible: Boolean)

    /**
     * Gets whether the custom name is visible.
     *
     * @return True if the custom name is visible, false otherwise.
     */
    fun isCustomNameVisible(): Boolean

    /**
     * Gets the main hand of this mannequin.
     *
     * @return The main hand (LEFT or RIGHT).
     */
    fun getMainHand(): MainHand

    /**
     * Sets the main hand of this mannequin.
     *
     * @param hand The new main hand (LEFT or RIGHT).
     */
    fun setMainHand(hand: MainHand)

    /**
     * Checks if this mannequin is immovable.
     * Immovable mannequins cannot be pushed by players or entities.
     *
     * @return Whether this mannequin is immovable.
     */
    fun isImmovable(): Boolean

    /**
     * Sets whether this mannequin is immovable.
     * Immovable mannequins cannot be pushed by players or entities.
     *
     * @param immovable The new immovable state.
     */
    fun setImmovable(immovable: Boolean)

    /**
     * Gets the equipment inventory for this mannequin.
     * This allows you to set armor, held items, etc.
     *
     * @return The equipment inventory.
     */
    fun getEquipment(): org.bukkit.inventory.EntityEquipment

    /**
     * Sets an item in a specific equipment slot.
     *
     * @param slot The equipment slot to set.
     * @param item The item to place in the slot, or null to remove the item.
     */
    fun setEquipment(slot: EquipmentSlot, item: ItemStack?)

    /**
     * Sets the item in the main hand.
     *
     * @param item The item to place in the main hand, or null to remove it.
     */
    fun setItemInMainHand(item: ItemStack?)

    /**
     * Sets the item in the off hand.
     *
     * @param item The item to place in the off hand, or null to remove it.
     */
    fun setItemInOffHand(item: ItemStack?)

    /**
     * Sets the helmet.
     *
     * @param item The item to place as helmet, or null to remove it
     */
    fun setHelmet(item: ItemStack?)

    /**
     * Sets the chestplate.
     *
     * @param item The item to place as chestplate, or null to remove it.
     */
    fun setChestplate(item: ItemStack?)

    /**
     * Sets the leggings.
     *
     * @param item The item to place as leggings, or null to remove it.
     */
    fun setLeggings(item: ItemStack?)

    /**
     * Sets the boots.
     *
     * @param item The item to place as boots, or null to remove it.
     */
    fun setBoots(item: ItemStack?)

    /**
     * Registers an event handler for a specific NPC event type.
     *
     * @param eventType The type of event to listen for.
     * @param handler The handler function that will be called when the event occurs.
     */
    fun onEvent(eventType: NPCEventType, handler: (NPCEvent) -> Unit)

    /**
     * Unregisters all event handlers for a specific event type.
     *
     * @param eventType The type of event to unregister handlers for.
     */
    fun removeEventHandlers(eventType: NPCEventType)

    /**
     * Unregisters all event handlers for this NPC.
     */
    fun removeAllEventHandlers()

    /**
     * Sets the detection range for proximity events (sneaking, punching).
     * Default is 5.0 blocks.
     *
     * @param range The detection range in blocks.
     */
    fun setProximityRange(range: Double)

    /**
     * Gets the current proximity detection range.
     *
     * @return The detection range in blocks.
     */
    fun getProximityRange(): Double

    /**
     * Sets whether the NPC should look at players within proximity range.
     * Default is false.
     *
     * @param enabled Whether the NPC should look at nearby players.
     */
    fun setLookAtPlayers(enabled: Boolean)

    /**
     * Gets whether the NPC looks at players within proximity range.
     *
     * @return True if the NPC looks at nearby players, false otherwise.
     */
    fun isLookingAtPlayers(): Boolean
}