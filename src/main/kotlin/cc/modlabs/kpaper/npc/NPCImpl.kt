package cc.modlabs.kpaper.npc

import cc.modlabs.kpaper.extensions.timer
import dev.fruxz.stacked.text
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MainHand
import org.bukkit.scheduler.BukkitTask

/**
 * Implementation of [NPC] that wraps a [Mannequin] entity.
 * Provides a convenient API for managing mannequin-based NPCs with walking capabilities.
 *
 * @property mannequin The underlying Mannequin entity.
 */
class NPCImpl(
    private val mannequin: Mannequin
) : NPC {

    private var currentTarget: Location? = null
    private val pathQueue = mutableListOf<Location>()
    private var walkingTask: BukkitTask? = null
    private var isPaused = false
    private var isWalking = false
    private val walkSpeed = 0.25 // Blocks per tick
    private val arrivalThreshold = 1.5 // Distance to consider "arrived"
    
    // Patrolling state
    private var isPatrolling = false
    private var isPatrolPaused = false
    private val patrolPath = mutableListOf<Location>()

    init {
        // Enable AI for the mannequin so it can move
        // Mannequin extends LivingEntity, so we can directly enable AI
        mannequin.setAI(true)
        
        // Register this NPC for event tracking
        NPCEventListener.registerNPC(mannequin, this)
    }

    override fun getMannequin(): Mannequin? = if (mannequin.isValid) mannequin else null

    override fun getEntity(): Entity? = getMannequin()

    override fun walkTo(location: Location): Boolean {
        val entity = getMannequin() as? LivingEntity ?: return false

        // Clear existing path and set new target
        pathQueue.clear()
        currentTarget = location.clone()
        isPaused = false

        // Start walking if not already walking
        if (!isWalking) {
            startWalking()
        }

        return true
    }

    override fun walkPath(locations: List<Location>): Boolean {
        if (locations.isEmpty()) return false
        val entity = getMannequin() as? LivingEntity ?: return false

        // Clear existing path and queue new locations
        pathQueue.clear()
        pathQueue.addAll(locations.map { it.clone() })
        isPaused = false

        // Set first location as target
        if (pathQueue.isNotEmpty()) {
            currentTarget = pathQueue.removeAt(0)
        }

        // Start walking if not already walking
        if (!isWalking) {
            startWalking()
        }

        return true
    }

    override fun pauseWalking(): Boolean {
        if (!isWalking) return false
        isPaused = true
        return true
    }

    override fun resumeWalking(): Boolean {
        if (!isWalking) return false
        isPaused = false
        return true
    }

    override fun startWalking(): Boolean {
        val entity = getMannequin() as? LivingEntity ?: return false

        // Enable AI if not already enabled
        entity.setAI(true)

        // If already walking, don't start again
        if (isWalking && walkingTask != null) {
            return true
        }

        isWalking = true
        isPaused = false

        // Start the walking task
        walkingTask = timer(1, "NPCWalking") {
            // Check pause state - use patrol pause if patrolling, otherwise regular pause
            val shouldPause = if (isPatrolling) isPatrolPaused else isPaused
            if (!isWalking || shouldPause) return@timer

            val currentEntity = getMannequin() as? LivingEntity ?: run {
                stopWalking()
                return@timer
            }

            val target = currentTarget ?: run {
                // No current target, check if there's a next location in the path
                if (pathQueue.isEmpty()) {
                    stopWalking()
                    return@timer
                }
                val nextLocation: Location = pathQueue.removeAt(0)
                currentTarget = nextLocation
                nextLocation
            }

            val currentLoc = currentEntity.location
            val distance = currentLoc.distance(target)

            // Check if we've arrived at the target
            if (distance <= arrivalThreshold) {
                // Arrived at current target
                val reachedLocation = currentTarget
                currentTarget = null

                // Trigger patrol point reached event if patrolling
                if (isPatrolling && reachedLocation != null) {
                    val npcEvent = NPCEvent(
                        npc = this@NPCImpl,
                        player = null,
                        eventType = NPCEventType.PATROL_POINT_REACHED,
                        data = mapOf("location" to reachedLocation)
                    )
                    triggerEvent(npcEvent)
                }

                // Check if there's a next location in the path
                if (pathQueue.isEmpty()) {
                    // Finished the path
                    if (isPatrolling) {
                        // If patrolling, loop back to first location
                        if (patrolPath.isNotEmpty()) {
                            // Trigger patrol cycle complete event
                            val cycleEvent = NPCEvent(
                                npc = this@NPCImpl,
                                player = null,
                                eventType = NPCEventType.PATROL_CYCLE_COMPLETE,
                                data = emptyMap()
                            )
                            triggerEvent(cycleEvent)
                            
                            pathQueue.addAll(patrolPath.map { it.clone() })
                            val nextLocation: Location = pathQueue.removeAt(0)
                            currentTarget = nextLocation
                        } else {
                            stopWalking()
                            return@timer
                        }
                    } else {
                        // Regular walking - stop when path is done
                        stopWalking()
                        return@timer
                    }
                } else {
                    val nextLocation: Location = pathQueue.removeAt(0)
                    currentTarget = nextLocation
                }
            } else {
                // Move towards target
                moveTowards(currentEntity, target, walkSpeed)
            }
        }

        return true
    }

    private fun stopWalking() {
        isWalking = false
        isPaused = false
        isPatrolling = false
        isPatrolPaused = false
        currentTarget = null
        pathQueue.clear()
        patrolPath.clear()
        walkingTask?.cancel()
        walkingTask = null
    }
    
    /**
     * Cleanup method called when NPC is removed.
     */
    fun cleanup() {
        val entity = getMannequin()
        if (entity != null) {
            NPCEventListener.unregisterNPC(entity)
        }
        stopWalking()
        removeAllEventHandlers()
    }

    override fun startPatrolling(locations: List<Location>): Boolean {
        if (locations.isEmpty()) return false
        val entity = getMannequin() as? LivingEntity ?: return false

        // Stop any existing walking/patrolling
        stopWalking()

        // Set up patrol path
        patrolPath.clear()
        patrolPath.addAll(locations.map { it.clone() })
        pathQueue.clear()
        pathQueue.addAll(patrolPath.map { it.clone() })
        
        // Set first location as target
        if (pathQueue.isNotEmpty()) {
            currentTarget = pathQueue.removeAt(0)
        }

        // Enable patrolling mode
        isPatrolling = true
        isPatrolPaused = false

        // Start walking
        startWalking()

        return true
    }

    override fun pausePatrolling(): Boolean {
        if (!isPatrolling) return false
        isPatrolPaused = true
        return true
    }

    override fun resumePatrolling(): Boolean {
        if (!isPatrolling) return false
        isPatrolPaused = false
        return true
    }

    override fun stopPatrolling(): Boolean {
        if (!isPatrolling) return false
        
        // Stop patrolling but keep walking task if there's a current target
        isPatrolling = false
        isPatrolPaused = false
        patrolPath.clear()
        
        // If no current target and no path queue, stop walking completely
        if (currentTarget == null && pathQueue.isEmpty()) {
            stopWalking()
        }
        
        return true
    }

    private fun moveTowards(entity: LivingEntity, target: Location, speed: Double) {
        val currentLoc = entity.location
        val direction = target.toVector().subtract(currentLoc.toVector()).normalize()

        // Calculate the movement vector
        val movement = direction.multiply(speed)

        // Get the new location
        val newLoc = currentLoc.clone().add(movement)

        // Make entity look at target
        val lookDirection = target.toVector().subtract(currentLoc.toVector())
        val yaw = Math.toDegrees(-Math.atan2(lookDirection.x, lookDirection.z)).toFloat()
        newLoc.yaw = yaw
        newLoc.pitch = 0f

        // Apply movement
        entity.velocity = movement
        entity.teleport(newLoc)
    }

    override fun teleport(location: Location): Boolean {
        val entity = getMannequin() ?: return false
        return try {
            entity.teleport(location)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun changeName(name: String) {
        val entity = getMannequin() ?: return
        entity.customName(text(name))
        // Ensure custom name is visible when setting a name
        entity.isCustomNameVisible = true
    }

    override fun getProfile(): MannequinProfile {
        return mannequin.profile
    }

    override fun setProfile(profile: MannequinProfile) {
        val entity = getMannequin() ?: return
        entity.profile = profile
    }

    override fun getSkinParts(): MannequinSkinParts {
        return mannequin.skinParts
    }

    override fun setSkinParts(parts: MannequinSkinParts) {
        val entity = getMannequin() ?: return
        // The parts parameter should be the entity's own skinParts that was modified
        // Since skinParts returns a Mutable that can't be reassigned, we assume
        // the caller has already modified the mannequin's skinParts directly
        // This method exists for API consistency
    }

    override fun getDescription(): Component? {
        return mannequin.description
    }

    override fun setDescription(description: Component?) {
        val entity = getMannequin() ?: return
        entity.description = description
    }

    override fun setCustomNameVisible(visible: Boolean) {
        val entity = getMannequin() ?: return
        entity.isCustomNameVisible = visible
    }

    override fun isCustomNameVisible(): Boolean {
        return mannequin.isCustomNameVisible
    }

    override fun getMainHand(): MainHand {
        return mannequin.mainHand
    }

    override fun setMainHand(hand: MainHand) {
        val entity = getMannequin() ?: return
        entity.mainHand = hand
    }

    override fun isImmovable(): Boolean {
        return mannequin.isImmovable
    }

    override fun setImmovable(immovable: Boolean) {
        val entity = getMannequin() ?: return
        entity.isImmovable = immovable
    }

    override fun getEquipment(): org.bukkit.inventory.EntityEquipment {
        return mannequin.equipment
    }

    override fun setEquipment(slot: EquipmentSlot, item: ItemStack?) {
        val entity = getMannequin() ?: return
        when (slot) {
            EquipmentSlot.HAND -> entity.equipment.setItemInMainHand(item)
            EquipmentSlot.OFF_HAND -> entity.equipment.setItemInOffHand(item)
            EquipmentSlot.HEAD -> entity.equipment.helmet = item
            EquipmentSlot.CHEST -> entity.equipment.chestplate = item
            EquipmentSlot.LEGS -> entity.equipment.leggings = item
            EquipmentSlot.FEET -> entity.equipment.boots = item
            EquipmentSlot.BODY, EquipmentSlot.SADDLE -> {
                // These slots don't apply to mannequins
            }
        }
    }

    override fun setItemInMainHand(item: ItemStack?) {
        val entity = getMannequin() ?: return
        entity.equipment.setItemInMainHand(item)
    }

    override fun setItemInOffHand(item: ItemStack?) {
        val entity = getMannequin() ?: return
        entity.equipment.setItemInOffHand(item)
    }

    override fun setHelmet(item: ItemStack?) {
        val entity = getMannequin() ?: return
        entity.equipment.helmet = item
    }

    override fun setChestplate(item: ItemStack?) {
        val entity = getMannequin() ?: return
        entity.equipment.chestplate = item
    }

    override fun setLeggings(item: ItemStack?) {
        val entity = getMannequin() ?: return
        entity.equipment.leggings = item
    }

    override fun setBoots(item: ItemStack?) {
        val entity = getMannequin() ?: return
        entity.equipment.boots = item
    }

    // Event handling
    private val eventHandlers = mutableMapOf<NPCEventType, MutableList<(NPCEvent) -> Unit>>()
    private var proximityRange = 5.0 // Default range in blocks
    private var lookAtPlayers = false // Whether to look at nearby players

    override fun onEvent(eventType: NPCEventType, handler: (NPCEvent) -> Unit) {
        eventHandlers.getOrPut(eventType) { mutableListOf() }.add(handler)
        // Register global listener if not already registered
        NPCEventListener.register()
        
        // Register for proximity monitoring if needed
        if (eventType == NPCEventType.PLAYER_SNEAKING_NEARBY || 
            eventType == NPCEventType.PLAYER_PUNCHING_NEARBY ||
            lookAtPlayers) {
            NPCEventListener.registerProximityNPC(this)
        }
    }

    override fun removeEventHandlers(eventType: NPCEventType) {
        eventHandlers.remove(eventType)
        
        // Unregister from proximity monitoring if no proximity handlers remain and not looking at players
        if (eventType == NPCEventType.PLAYER_SNEAKING_NEARBY || 
            eventType == NPCEventType.PLAYER_PUNCHING_NEARBY) {
            val hasProximityHandlers = eventHandlers.containsKey(NPCEventType.PLAYER_SNEAKING_NEARBY) ||
                                      eventHandlers.containsKey(NPCEventType.PLAYER_PUNCHING_NEARBY)
            if (!hasProximityHandlers && !lookAtPlayers) {
                NPCEventListener.unregisterProximityNPC(this)
            }
        }
    }

    override fun removeAllEventHandlers() {
        eventHandlers.clear()
        NPCEventListener.unregisterProximityNPC(this)
    }

    override fun setProximityRange(range: Double) {
        proximityRange = range.coerceAtLeast(0.0)
    }

    override fun getProximityRange(): Double {
        return proximityRange
    }

    override fun setLookAtPlayers(enabled: Boolean) {
        lookAtPlayers = enabled
        if (enabled) {
            NPCEventListener.registerProximityNPC(this)
        } else {
            // Only unregister if no proximity event handlers are registered
            val hasProximityHandlers = eventHandlers.containsKey(NPCEventType.PLAYER_SNEAKING_NEARBY) ||
                                      eventHandlers.containsKey(NPCEventType.PLAYER_PUNCHING_NEARBY)
            if (!hasProximityHandlers) {
                NPCEventListener.unregisterProximityNPC(this)
            }
        }
    }

    override fun isLookingAtPlayers(): Boolean {
        return lookAtPlayers
    }

    override fun setGravity(gravity: Boolean) {
        val entity = getMannequin() ?: return
        entity.setGravity(gravity)
    }

    override fun hasGravity(): Boolean {
        return mannequin.hasGravity()
    }

    // Conversation state
    private var conversationBuilder: ((NPCConversation.ConversationBuilder.() -> Unit)?) = null

    override fun setConversation(conversation: NPCConversation.ConversationBuilder.() -> Unit) {
        conversationBuilder = conversation
    }

    override fun getConversation(): (NPCConversation.ConversationBuilder.() -> Unit)? {
        return conversationBuilder
    }

    override fun removeConversation() {
        conversationBuilder = null
    }

    override fun startConversation(player: org.bukkit.entity.Player) {
        val builder = conversationBuilder ?: return
        val conversation = NPCConversation(this, player)
        conversation.conversation(builder)
        conversation.start()
    }

    /**
     * Internal method to trigger an event for this NPC.
     */
    internal fun triggerEvent(event: NPCEvent) {
        eventHandlers[event.eventType]?.forEach { handler ->
            try {
                handler(event)
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }

    /**
     * Internal method to check if NPC is currently walking.
     */
    internal fun isCurrentlyWalking(): Boolean {
        return isWalking && !isPaused
    }
}

