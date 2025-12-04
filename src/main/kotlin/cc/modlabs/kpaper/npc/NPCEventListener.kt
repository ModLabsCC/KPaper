package cc.modlabs.kpaper.npc

import cc.modlabs.kpaper.event.listen
import cc.modlabs.kpaper.extensions.timer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.*
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Global event listener for NPC events.
 * Listens to Bukkit events and triggers NPC-specific events.
 */
object NPCEventListener {
    private var isRegistered = false
    private val npcMap = mutableMapOf<UUID, NPC>()
    private val proximityNPCs = mutableMapOf<UUID, NPC>()
    private val visibilityNPCs = mutableMapOf<UUID, NPC>()
    private var proximityTask: BukkitTask? = null
    private var lookAtTask: BukkitTask? = null // Separate task for look-at (runs more frequently)
    private val playerPunchingState = mutableMapOf<Player, Long>() // Player -> last punch time
    private val playerSneakingState = mutableMapOf<Player, Boolean>() // Player -> is sneaking

    /**
     * Gets the entity for an NPC by UUID, trying multiple methods.
     * First tries npc.getEntity(), then looks up by UUID from npcMap, then searches all worlds.
     */
    private fun getEntityByUUID(npc: NPC, npcId: UUID): org.bukkit.entity.Entity? {
        // First try the NPC's getEntity() method
        val entity = npc.getEntity()
        if (entity != null && entity.isValid) {
            return entity
        }
        
        // If that fails, try to get it from npcMap (which should have the current entity)
        val npcFromMap = npcMap[npcId]
        val entityFromMap = npcFromMap?.getEntity()
        if (entityFromMap != null && entityFromMap.isValid) {
            return entityFromMap
        }
        
        // Last resort: search all worlds for the entity with this UUID
        for (world in Bukkit.getWorlds()) {
            val foundEntity = world.getEntities().firstOrNull { it.uniqueId == npcId && it.isValid }
            if (foundEntity != null) {
                return foundEntity
            }
        }
        
        return null
    }

    /**
     * Registers the global event listener.
     * This is called automatically when an NPC registers its first event handler.
     */
    fun register() {
        if (isRegistered) {
            return
        }
        isRegistered = true

        // Listen for player right-click on entities
        listen<PlayerInteractEntityEvent> { event ->
            val entity = event.rightClicked
            val npc = npcMap[entity.uniqueId] ?: return@listen
            val player = event.player
            val isSneaking = player.isSneaking

            // Check if NPC has a conversation and start it (only on normal right-click, not shift-click)
            if (!isSneaking && npc.getConversation() != null) {
                npc.startConversation(player)
                event.isCancelled = true // Prevent other interactions
                return@listen
            }

            // Determine event type based on shift state
            val eventType = if (isSneaking) {
                NPCEventType.SHIFT_RIGHT_CLICKED
            } else {
                NPCEventType.RIGHT_CLICKED
            }

            val npcEvent = NPCEvent(
                npc = npc,
                player = player,
                eventType = eventType,
                data = mapOf(
                    "interactionHand" to event.hand.name,
                    "isSneaking" to isSneaking
                )
            )
            (npc as? NPCImpl)?.triggerEvent(npcEvent)
        }

        // Listen for player interact at entity (click at specific position)
        listen<PlayerInteractAtEntityEvent> { event ->
            val entity = event.rightClicked
            val npc = npcMap[entity.uniqueId] ?: return@listen
            val player = event.player
            val isSneaking = player.isSneaking
            
            // Get the interaction position (where the player clicked on the entity)
            val clickedPosition = event.clickedPosition
            
            val npcEvent = NPCEvent(
                npc = npc,
                player = player,
                eventType = NPCEventType.PLAYER_INTERACT_AT,
                data = mapOf(
                    "interactionHand" to event.hand.name,
                    "isSneaking" to isSneaking,
                    "clickedPosition" to clickedPosition,
                    "clickedPositionX" to clickedPosition.x,
                    "clickedPositionY" to clickedPosition.y,
                    "clickedPositionZ" to clickedPosition.z
                )
            )
            (npc as? NPCImpl)?.triggerEvent(npcEvent)
        }

        // Listen for entity damage (left-click or damage)
        listen<EntityDamageByEntityEvent> { event ->
            val entity = event.entity
            val npc = npcMap[entity.uniqueId] ?: return@listen
            val damager = event.damager

            if (damager is Player) {
                val isSneaking = damager.isSneaking
                // Check if it's a left-click (attack) or actual damage
                // Left-click typically has very low or zero damage
                val isLeftClick = event.damage <= 1.0 && event.finalDamage <= 1.0
                
                if (isLeftClick) {
                    // Trigger left-click event (shift or normal)
                    val eventType = if (isSneaking) {
                        NPCEventType.SHIFT_LEFT_CLICKED
                    } else {
                        NPCEventType.LEFT_CLICKED
                    }
                    
                    val leftClickEvent = NPCEvent(
                        npc = npc,
                        player = damager,
                        eventType = eventType,
                        data = mapOf(
                            "damage" to event.damage,
                            "isSneaking" to isSneaking
                        )
                    )
                    (npc as? NPCImpl)?.triggerEvent(leftClickEvent)
                } else {
                    // Trigger damage event
                    val damageEvent = NPCEvent(
                        npc = npc,
                        player = damager,
                        eventType = NPCEventType.DAMAGED,
                        data = mapOf(
                            "damage" to event.damage,
                            "finalDamage" to event.finalDamage
                        )
                    )
                    (npc as? NPCImpl)?.triggerEvent(damageEvent)
                }
            }
        }

        // Listen for player animations (punching)
        listen<PlayerAnimationEvent> { event ->
            val player = event.player
            if (event.animationType == org.bukkit.event.player.PlayerAnimationType.ARM_SWING) {
                // Player is punching - track it
                playerPunchingState[player] = System.currentTimeMillis()
            }
        }

        // Listen for player join events (for visibility management)
        listen<PlayerJoinEvent> { event ->
            val player = event.player
            visibilityNPCs.values.forEach { npc ->
                (npc as? NPCImpl)?.onPlayerJoin(player)
            }
        }

        // Listen for player quit events (for visibility cleanup)
        listen<PlayerQuitEvent> { event ->
            val player = event.player
            visibilityNPCs.values.forEach { npc ->
                (npc as? NPCImpl)?.onPlayerQuit(player)
            }
        }

        // Start proximity monitoring task
        startProximityMonitoring()
        
        // Start look-at task (runs more frequently for smooth looking)
        startLookAtTask()
        
        // Initialize conversation system
        NPCConversation.initialize()
    }

    /**
     * Starts the look-at task that makes NPCs look at nearby players.
     * This runs more frequently than the proximity task for smooth looking.
     */
    private fun startLookAtTask() {
        // Check if task exists and is still running
        if (lookAtTask != null && !lookAtTask!!.isCancelled) {
            return
        }
        
        // Cancel existing task if it's cancelled but still referenced
        if (lookAtTask != null && lookAtTask!!.isCancelled) {
            lookAtTask = null
        }

        lookAtTask = timer(5, "NPCLookAt") { // Run every 5 ticks for smooth looking
            val lookAtNPCs = proximityNPCs.entries.filter { it.value.isLookingAtPlayers() }
            
            if (lookAtNPCs.isEmpty()) {
                return@timer
            }

            lookAtNPCs.forEach { (npcId, npc) ->
                val entity = getEntityByUUID(npc, npcId) as? org.bukkit.entity.LivingEntity ?: return@forEach
                if (!entity.isValid) return@forEach
                
                val npcLocation = entity.location
                val range = npc.getProximityRange()
                
                // Find nearest player
                val nearbyPlayers = entity.world.getNearbyEntities(npcLocation, range, range, range)
                    .filterIsInstance<Player>()
                    .filter { it.location.distance(npcLocation) <= range }
                
                if (nearbyPlayers.isEmpty()) return@forEach
                
                // Ensure AI is enabled
                if (!entity.hasAI()) {
                    entity.setAI(true)
                }
                
                // Determine which player to look at
                val npcImpl = npc as? NPCImpl
                val isFollowing = npcImpl?.isFollowingEntity() ?: false
                val followedEntity = npcImpl?.getFollowingEntity()
                
                val playerToLookAt = if (isFollowing && followedEntity is Player && nearbyPlayers.contains(followedEntity)) {
                    followedEntity
                } else {
                    nearbyPlayers.minByOrNull { it.location.distance(npcLocation) }
                }
                
                if (playerToLookAt != null) {
                    makeEntityLookAt(entity, playerToLookAt.location)
                }
            }
        }
    }

    /**
     * Starts the proximity monitoring task that checks for nearby players.
     */
    private fun startProximityMonitoring() {
        // Check if task exists and is still running
        if (proximityTask != null && !proximityTask!!.isCancelled) {
            return
        }
        
        // Cancel existing task if it's cancelled but still referenced
        if (proximityTask != null && proximityTask!!.isCancelled) {
            proximityTask = null
        }

        proximityTask = timer(10, "NPCProximity") { // Check every 10 ticks (reduced frequency to avoid performance issues)
            val currentTime = System.currentTimeMillis()
            val proximityNPCsCopy = proximityNPCs.entries.toList() // Copy to avoid concurrent modification

            // Debug: Log if no NPCs are registered
            if (proximityNPCsCopy.isEmpty()) {
                return@timer
            }

            proximityNPCsCopy.forEach { (npcId, npc) ->
                val entity = getEntityByUUID(npc, npcId)
                if (entity == null || !entity.isValid) {
                    return@forEach
                }
                
                val npcLocation = entity.location
                val world = entity.world
                if (world == null) {
                    return@forEach
                }
                
                val range = npc.getProximityRange()
                val npcName = entity.customName ?: entity.type.name

                // Get all nearby players
                val allNearbyEntities = world.getNearbyEntities(npcLocation, range, range, range)
                val nearbyPlayers = allNearbyEntities
                    .filterIsInstance<Player>()
                    .filter { it.isValid && !it.isDead && it.location.distance(npcLocation) <= range }

                if (nearbyPlayers.isNotEmpty()) {
                    nearbyPlayers.forEach { player ->
                        val distance = player.location.distance(npcLocation)
                    }
                }

                // Note: Look-at is now handled by a separate, more frequent task (startLookAtTask)
                // This proximity task focuses on event detection only

                // Process events for each nearby player
                nearbyPlayers.forEach { player ->
                    val distance = player.location.distance(npcLocation)
                    
                    // Check for sneaking - trigger event when player starts sneaking
                    val wasSneaking = playerSneakingState[player] ?: false
                    val isSneaking = player.isSneaking
                    
                    if (isSneaking && !wasSneaking) {
                        // Player just started sneaking - trigger event
                        val sneakingEvent = NPCEvent(
                            npc = npc,
                            player = player,
                            eventType = NPCEventType.PLAYER_SNEAKING_NEARBY,
                            data = mapOf(
                                "distance" to distance,
                                "location" to player.location
                            )
                        )
                        (npc as? NPCImpl)?.triggerEvent(sneakingEvent)
                    }
                    
                    // Update sneaking state
                    playerSneakingState[player] = isSneaking

                    // Check for punching (within last 500ms)
                    val lastPunchTime = playerPunchingState[player] ?: 0L
                    if (currentTime - lastPunchTime < 500) {
                        // Player is punching nearby - trigger event
                        val punchingEvent = NPCEvent(
                            npc = npc,
                            player = player,
                            eventType = NPCEventType.PLAYER_PUNCHING_NEARBY,
                            data = mapOf(
                                "distance" to distance,
                                "location" to player.location
                            )
                        )
                        (npc as? NPCImpl)?.triggerEvent(punchingEvent)
                        // Remove punch state after triggering to avoid duplicate events
                        playerPunchingState.remove(player)
                    }
                }
            }

            // Cleanup old punch states
            playerPunchingState.entries.removeAll { currentTime - it.value > 1000 }
        }
    }

    /**
     * Makes an entity look at a target location.
     * Uses teleport to update the look direction, which works even when AI is disabled.
     */
    private fun makeEntityLookAt(entity: org.bukkit.entity.LivingEntity, target: org.bukkit.Location) {
        val entityLoc = entity.location
        val direction = target.toVector().subtract(entityLoc.toVector())
        val distance = direction.length()
        
        // Don't try to look if too far away or invalid
        if (distance > 64.0 || !entity.isValid) {
            return
        }
        
        // Calculate yaw (horizontal rotation)
        val yaw = Math.toDegrees(-atan2(direction.x, direction.z)).toFloat()
        
        // Calculate pitch (vertical rotation)
        val horizontalDistance = sqrt(direction.x * direction.x + direction.z * direction.z)
        val pitch = Math.toDegrees(-atan2(direction.y, horizontalDistance)).toFloat().coerceIn(-90f, 90f)
        
        val entityName = entity.customName ?: entity.type.name
        
        // Ensure AI is enabled for mannequins to maintain look direction
        // In MC 1.21.10, mannequins need AI to look at entities properly
        if (!entity.hasAI()) {
            entity.setAI(true)
        }
        
        // Apply rotation using teleport (this works even without AI, but AI helps maintain it)
        val newLocation = entityLoc.clone()
        newLocation.yaw = yaw
        newLocation.pitch = pitch
        
        try {
            // Use teleport with relative rotation to update look direction
            entity.teleport(newLocation)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Registers an NPC entity for event tracking.
     * Called automatically when an NPC is created.
     */
    fun registerNPC(entity: org.bukkit.entity.Entity, npc: NPC) {
        npcMap[entity.uniqueId] = npc
    }

    /**
     * Gets an NPC by UUID.
     * Used internally to look up NPCs when entity references become invalid.
     */
    fun getNPCByUUID(uuid: UUID): NPC? {
        return npcMap[uuid]
    }

    /**
     * Unregisters an NPC entity from event tracking.
     * Called automatically when an NPC is removed.
     */
    fun unregisterNPC(entity: org.bukkit.entity.Entity) {
        val npc = npcMap.remove(entity.uniqueId)
        if (npc != null) {
            unregisterProximityNPC(npc)
        }
    }

    /**
     * Registers an NPC for proximity event monitoring.
     */
    fun registerProximityNPC(npc: NPC) {
        val entity = npc.getEntity()
        val npcName = entity?.customName ?: entity?.type?.name ?: "Unknown"
        val npcId = npc.getID() ?: return

        proximityNPCs[npcId] = npc
        
        // Ensure the event listener is registered and proximity monitoring is started
        register()
        // Ensure proximity monitoring task is running (in case it was cancelled or never started)
        startProximityMonitoring()
        // Ensure look-at task is running if NPC wants to look at players
        if (npc.isLookingAtPlayers()) {
            startLookAtTask()
        }
    }

    /**
     * Unregisters an NPC from proximity event monitoring.
     */
    fun unregisterProximityNPC(npc: NPC) {
        val npcId = npc.getID() ?: return
        proximityNPCs.remove(npcId)
    }

    /**
     * Registers an NPC for visibility management.
     * Called automatically when an NPC has visibility restrictions.
     */
    fun registerVisibilityNPC(npc: NPC) {
        val npcId = npc.getID() ?: return
        visibilityNPCs[npcId] = npc
    }

    /**
     * Unregisters an NPC from visibility management.
     */
    fun unregisterVisibilityNPC(npc: NPC) {
        val npcId = npc.getID() ?: return
        visibilityNPCs.remove(npcId)
    }
}

