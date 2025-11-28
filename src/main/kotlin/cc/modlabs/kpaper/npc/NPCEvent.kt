package cc.modlabs.kpaper.npc

import org.bukkit.entity.Player

/**
 * Enum representing different types of NPC events.
 */
enum class NPCEventType {
    /**
     * Triggered when a player right-clicks on the NPC.
     */
    RIGHT_CLICKED,
    
    /**
     * Triggered when a player shift-right-clicks on the NPC.
     */
    SHIFT_RIGHT_CLICKED,
    
    /**
     * Triggered when a player left-clicks on the NPC.
     */
    LEFT_CLICKED,
    
    /**
     * Triggered when a player shift-left-clicks on the NPC.
     */
    SHIFT_LEFT_CLICKED,
    
    /**
     * Triggered when a player damages the NPC.
     */
    DAMAGED,
    
    /**
     * Triggered when the NPC reaches a patrol point.
     */
    PATROL_POINT_REACHED,
    
    /**
     * Triggered when the NPC completes a full patrol cycle.
     */
    PATROL_CYCLE_COMPLETE,
    
    /**
     * Triggered when a player is sneaking within range of the NPC.
     */
    PLAYER_SNEAKING_NEARBY,
    
    /**
     * Triggered when a player is punching within range of the NPC.
     */
    PLAYER_PUNCHING_NEARBY
}

/**
 * Data class representing an NPC event.
 *
 * @param npc The NPC that triggered the event.
 * @param player The player involved in the event (null for non-player events).
 * @param eventType The type of event that occurred.
 * @param data Additional event-specific data.
 */
data class NPCEvent(
    val npc: NPC,
    val player: Player?,
    val eventType: NPCEventType,
    val data: Map<String, Any> = emptyMap()
)

