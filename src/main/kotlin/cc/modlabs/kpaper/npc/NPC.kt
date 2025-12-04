package cc.modlabs.kpaper.npc

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

// Import the actual types from Paper API
import io.papermc.paper.datacomponent.item.ResolvableProfile
import com.destroystokyo.paper.SkinParts
import org.bukkit.inventory.MainHand
import java.util.UUID

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
    fun getEntity(): Entity?

    /**
     * Gets the UUID of the NPC entity
     */
    fun getID(): UUID?

    /**
     * Gets the underlying LivingEntity for this NPC.
     * For mannequin-based NPCs, this will return a [Mannequin] instance (which extends LivingEntity).
     *
     * @return The underlying LivingEntity, or null if the entity is no longer valid.
     */
    fun getLivingEntity(): org.bukkit.entity.LivingEntity?

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

    /**
     * Sets whether the NPC is affected by gravity.
     * Default is true.
     *
     * @param gravity Whether the NPC should be affected by gravity.
     */
    fun setGravity(gravity: Boolean)

    /**
     * Gets whether the NPC is affected by gravity.
     *
     * @return True if the NPC is affected by gravity, false otherwise.
     */
    fun hasGravity(): Boolean

    /**
     * Sets whether the NPC has AI enabled.
     * When AI is enabled, the NPC can move, look at entities, and perform other AI behaviors.
     * Default is true.
     *
     * @param enabled Whether the NPC should have AI enabled.
     */
    fun setAI(enabled: Boolean)

    /**
     * Gets whether the NPC has AI enabled.
     *
     * @return True if the NPC has AI enabled, false otherwise.
     */
    fun hasAI(): Boolean

    /**
     * Sets a conversation for this NPC.
     * The conversation will start when a player right-clicks the NPC.
     *
     * @param conversation The conversation configuration.
     */
    fun setConversation(conversation: NPCConversation.ConversationBuilder.() -> Unit)

    /**
     * Gets the conversation for this NPC, or null if none is set.
     *
     * @return The conversation builder function, or null.
     */
    fun getConversation(): (NPCConversation.ConversationBuilder.() -> Unit)?

    /**
     * Removes the conversation from this NPC.
     */
    fun removeConversation()

    /**
     * Starts a conversation with a player.
     *
     * @param player The player to start the conversation with.
     */
    fun startConversation(player: Player)

    /**
     * Enable or disable pathfinding for this NPC.
     * When enabled, NPCs will use A* pathfinding to navigate around obstacles and jump when needed.
     * When disabled, NPCs will move directly towards targets.
     * Pathfinding is enabled by default.
     *
     * @param enabled Whether pathfinding should be enabled.
     */
    fun setPathfindingEnabled(enabled: Boolean)

    /**
     * Check if pathfinding is enabled for this NPC.
     *
     * @return True if pathfinding is enabled, false otherwise.
     */
    fun isPathfindingEnabled(): Boolean

    /**
     * Makes the NPC follow a target entity using pathfinding.
     * The NPC will automatically navigate around obstacles and jump when needed.
     * The path is recalculated periodically as the target moves.
     *
     * @param entity The entity to follow (player, mob, etc.).
     * @param followDistance The minimum distance to maintain from the target (default: 2.0 blocks).
     * @return True if following successfully starts, false otherwise.
     */
    fun followEntity(entity: Entity, followDistance: Double = 2.0): Boolean

    /**
     * Stops the NPC from following its target entity.
     *
     * @return True if following is successfully stopped, false otherwise.
     */
    fun stopFollowing(): Boolean

    /**
     * Checks if the NPC is currently following an entity.
     *
     * @return True if the NPC is following an entity, false otherwise.
     */
    fun isFollowingEntity(): Boolean

    /**
     * Gets the entity that this NPC is currently following, if any.
     *
     * @return The entity being followed, or null if not following any entity.
     */
    fun getFollowingEntity(): Entity?

    /**
     * Makes the NPC follow nearby players within a specified range.
     * The NPC will automatically switch between following different players as they enter/leave range.
     * If no players are nearby, the NPC will return to its spawn location.
     * Uses pathfinding for navigation.
     *
     * @param range The range in blocks to search for nearby players (default: 10.0).
     * @param followDistance The minimum distance to maintain from the followed player (default: 2.0).
     * @return True if nearby following successfully starts, false otherwise.
     */
    fun followNearbyPlayers(range: Double = 10.0, followDistance: Double = 2.0): Boolean

    /**
     * Stops the NPC from following nearby players.
     *
     * @return True if nearby following is successfully stopped, false otherwise.
     */
    fun stopFollowingNearbyPlayers(): Boolean

    /**
     * Sets the spawn location for this NPC.
     * This is the location the NPC will return to when no players are nearby.
     *
     * @param location The spawn location.
     */
    fun setSpawnLocation(location: Location)

    /**
     * Gets the spawn location for this NPC.
     *
     * @return The spawn location, or null if not set.
     */
    fun getSpawnLocation(): Location?

    /**
     * Checks if the NPC is currently following nearby players.
     *
     * @return True if the NPC is following nearby players, false otherwise.
     */
    fun isFollowingNearbyPlayers(): Boolean

    /**
     * Sets whether the NPC is visible to all players.
     * When set to true, all players can see the NPC (default behavior).
     * When set to false, the NPC is hidden from all players.
     *
     * @param visible True to make visible to all players, false to hide from all.
     */
    fun setVisibleToAllPlayers(visible: Boolean)

    /**
     * Sets the list of players who can see this NPC.
     * If an empty set is provided, the NPC becomes visible to all players.
     * Players not in the list will not be able to see the NPC.
     *
     * @param players The set of players who should be able to see this NPC.
     */
    fun setVisibleToPlayers(players: Set<Player>)

    /**
     * Adds a player to the list of players who can see this NPC.
     * If the NPC was previously visible to all players, it will become visible only to the specified players.
     *
     * @param player The player to add to the visible players list.
     */
    fun addVisiblePlayer(player: Player)

    /**
     * Removes a player from the list of players who can see this NPC.
     * If the list becomes empty after removal, the NPC becomes visible to all players.
     *
     * @param player The player to remove from the visible players list.
     */
    fun removeVisiblePlayer(player: Player)

    /**
     * Gets the set of players who can see this NPC.
     * Returns null if the NPC is visible to all players.
     *
     * @return The set of visible players, or null if visible to all.
     */
    fun getVisiblePlayers(): Set<Player>?

    /**
     * Checks if the NPC is visible to all players.
     *
     * @return True if visible to all players, false if restricted to specific players.
     */
    fun isVisibleToAllPlayers(): Boolean
}