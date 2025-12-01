package cc.modlabs.kpaper.npc

import cc.modlabs.kpaper.event.listen
import cc.modlabs.kpaper.extensions.timer
import cc.modlabs.kpaper.util.getLogger
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Global event listener for NPC events.
 * Listens to Bukkit events and triggers NPC-specific events.
 */
object NPCEventListener {
    private var isRegistered = false
    private val npcMap = mutableMapOf<org.bukkit.entity.Entity, NPC>()
    private val proximityNPCs = mutableSetOf<NPC>()
    private val visibilityNPCs = mutableSetOf<NPC>()
    private var proximityTask: BukkitTask? = null
    private val playerPunchingState = mutableMapOf<Player, Long>() // Player -> last punch time
    private val playerSneakingState = mutableMapOf<Player, Boolean>() // Player -> is sneaking

    /**
     * Registers the global event listener.
     * This is called automatically when an NPC registers its first event handler.
     */
    fun register() {
        if (isRegistered) return
        isRegistered = true

        // Listen for player right-click on entities
        listen<PlayerInteractEntityEvent> { event ->
            val entity = event.rightClicked
            val npc = npcMap[entity] ?: return@listen
            val player = event.player
            val isSneaking = player.isSneaking
            getLogger().info("Interacted with NPC $npc for player $player ($isSneaking) - ${event.hand.name}")

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
            val npc = npcMap[entity] ?: return@listen
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
            val npc = npcMap[entity] ?: return@listen
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
            visibilityNPCs.forEach { npc ->
                (npc as? NPCImpl)?.onPlayerJoin(player)
            }
        }

        // Listen for player quit events (for visibility cleanup)
        listen<PlayerQuitEvent> { event ->
            val player = event.player
            visibilityNPCs.forEach { npc ->
                (npc as? NPCImpl)?.onPlayerQuit(player)
            }
        }

        // Start proximity monitoring task
        startProximityMonitoring()
        
        // Initialize conversation system
        NPCConversation.initialize()
    }

    /**
     * Starts the proximity monitoring task that checks for nearby players.
     */
    private fun startProximityMonitoring() {
        if (proximityTask != null) return

        proximityTask = timer(5, "NPCProximity") { // Check every 5 ticks
            val currentTime = System.currentTimeMillis()
            val proximityNPCsCopy = proximityNPCs.toList() // Copy to avoid concurrent modification

            proximityNPCsCopy.forEach { npc ->
                val entity = npc.getEntity() ?: return@forEach
                val npcLocation = entity.location
                val range = npc.getProximityRange()

                // Get all nearby players
                val nearbyPlayers = entity.world.getNearbyEntities(npcLocation, range, range, range)
                    .filterIsInstance<Player>()
                    .filter { it.location.distance(npcLocation) <= range }

                // Make NPC look at nearest player if enabled
                // Only look at players if NPC is not currently walking (to avoid conflicts)
                if (npc.isLookingAtPlayers() && nearbyPlayers.isNotEmpty() && entity is org.bukkit.entity.LivingEntity) {
                    // Check if NPC is walking
                    val isWalking = (npc as? NPCImpl)?.isCurrentlyWalking() ?: false
                    
                    // Only look at players if not walking
                    if (!isWalking) {
                        val nearestPlayer = nearbyPlayers.minByOrNull { it.location.distance(npcLocation) }
                        if (nearestPlayer != null) {
                            makeEntityLookAt(entity, nearestPlayer.location)
                        }
                    }
                }

                // Process events for each nearby player
                nearbyPlayers.forEach { player ->
                    val distance = player.location.distance(npcLocation)
                    
                    // Check for sneaking
                    val wasSneaking = playerSneakingState[player] ?: false
                    val isSneaking = player.isSneaking
                    playerSneakingState[player] = isSneaking

                    if (isSneaking && !wasSneaking) {
                        // Player just started sneaking
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

                    // Check for punching (within last 500ms)
                    val lastPunchTime = playerPunchingState[player] ?: 0L
                    if (currentTime - lastPunchTime < 500) {
                        // Player is punching nearby
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
                        // Remove punch state after triggering
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
     */
    private fun makeEntityLookAt(entity: org.bukkit.entity.LivingEntity, target: org.bukkit.Location) {
        val entityLoc = entity.location
        val direction = target.toVector().subtract(entityLoc.toVector())
        
        // Calculate yaw (horizontal rotation)
        val yaw = Math.toDegrees(-atan2(direction.x, direction.z)).toFloat()
        
        // Calculate pitch (vertical rotation)
        val horizontalDistance = sqrt(direction.x * direction.x + direction.z * direction.z)
        val pitch = Math.toDegrees(-atan2(direction.y, horizontalDistance)).toFloat()
        
        // Apply rotation
        val newLocation = entityLoc.clone()
        newLocation.yaw = yaw
        newLocation.pitch = pitch
        
        entity.teleport(newLocation)
    }

    /**
     * Registers an NPC entity for event tracking.
     * Called automatically when an NPC is created.
     */
    fun registerNPC(entity: org.bukkit.entity.Entity, npc: NPC) {
        npcMap[entity] = npc
    }

    /**
     * Unregisters an NPC entity from event tracking.
     * Called automatically when an NPC is removed.
     */
    fun unregisterNPC(entity: org.bukkit.entity.Entity) {
        val npc = npcMap.remove(entity)
        if (npc != null) {
            unregisterProximityNPC(npc)
        }
    }

    /**
     * Registers an NPC for proximity event monitoring.
     */
    fun registerProximityNPC(npc: NPC) {
        proximityNPCs.add(npc)
    }

    /**
     * Unregisters an NPC from proximity event monitoring.
     */
    fun unregisterProximityNPC(npc: NPC) {
        proximityNPCs.remove(npc)
    }

    /**
     * Registers an NPC for visibility management.
     * Called automatically when an NPC has visibility restrictions.
     */
    fun registerVisibilityNPC(npc: NPC) {
        visibilityNPCs.add(npc)
    }

    /**
     * Unregisters an NPC from visibility management.
     */
    fun unregisterVisibilityNPC(npc: NPC) {
        visibilityNPCs.remove(npc)
    }
}

