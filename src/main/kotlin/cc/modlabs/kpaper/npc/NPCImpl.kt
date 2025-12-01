package cc.modlabs.kpaper.npc

import cc.modlabs.kpaper.extensions.timer
import dev.fruxz.stacked.text
import net.kyori.adventure.text.Component
import cc.modlabs.kpaper.main.PluginInstance
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MainHand
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import kotlin.math.atan2

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
    
    // Pathfinding state
    private var usePathfinding = true // Enable pathfinding by default
    private var currentPath = mutableListOf<Location>() // Current calculated path
    private var pathIndex = 0 // Current index in the path
    
    // Patrolling state
    private var isPatrolling = false
    private var isPatrolPaused = false
    private val patrolPath = mutableListOf<Location>()
    
    // Following state
    private var isFollowing = false
    private var followingEntity: Entity? = null
    private var followDistance = 2.0 // Minimum distance to maintain from target
    private var followUpdateInterval = 20 // Ticks between path recalculations (1 second)
    private var followUpdateCounter = 0
    
    // Nearby player following state
    private var isFollowingNearbyPlayers = false
    private var nearbyFollowRange = 10.0 // Range to search for nearby players
    private var nearbyFollowDistance = 2.0 // Distance to maintain from followed player
    private var spawnLocation: Location? = null // Spawn location to return to
    private var nearbyFollowTask: BukkitTask? = null
    private var nearbyFollowCheckInterval = 20L // Ticks between checking for nearby players (1 second)
    
    // Visibility state
    // null = visible to all players, non-null = only visible to players in the set
    private var visibleToPlayers: MutableSet<Player>? = null

    init {
        // Enable AI for the mannequin so it can move
        // Mannequin extends LivingEntity, so we can directly enable AI
        mannequin.setAI(true)
        
        // Register this NPC for event tracking
        NPCEventListener.registerNPC(mannequin, this)
    }

    override fun getMannequin(): Mannequin? = if (mannequin.isValid) mannequin else null

    override fun getEntity(): Entity? = getMannequin()

    override fun getLivingEntity(): LivingEntity? = if (mannequin.isValid) mannequin else null

    override fun walkTo(location: Location): Boolean {
        val entity = getMannequin() as? LivingEntity ?: return false

        // Clear existing path
        pathQueue.clear()
        currentPath.clear()
        pathIndex = 0
        
        val targetLocation = location.clone()
        val currentLoc = entity.location

        // Use pathfinding if enabled
        if (usePathfinding) {
            val path = Pathfinder.findPath(currentLoc, targetLocation)
            if (path.isNotEmpty()) {
                // Add all waypoints except the first (current position) to the queue
                currentPath.addAll(path)
                if (currentPath.size > 1) {
                    pathQueue.addAll(currentPath.subList(1, currentPath.size))
                } else {
                    pathQueue.add(targetLocation)
                }
            } else {
                // Pathfinding failed, use direct path
                pathQueue.add(targetLocation)
            }
        } else {
            // Direct path without pathfinding
            pathQueue.add(targetLocation)
        }

        // Set first location as target
        if (pathQueue.isNotEmpty()) {
            currentTarget = pathQueue.removeAt(0)
        } else {
            currentTarget = targetLocation
        }
        
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

        // Clear existing path
        pathQueue.clear()
        currentPath.clear()
        pathIndex = 0
        
        val currentLoc = entity.location
        val processedLocations = mutableListOf<Location>()

        // Use pathfinding between each waypoint if enabled
        if (usePathfinding) {
            var lastLocation = currentLoc
            for (target in locations) {
                val path = Pathfinder.findPath(lastLocation, target)
                if (path.isNotEmpty()) {
                    // Add waypoints except the first (which is the last location)
                    if (processedLocations.isEmpty()) {
                        processedLocations.addAll(path)
                    } else {
                        processedLocations.addAll(path.subList(1, path.size))
                    }
                    lastLocation = target
                } else {
                    // Pathfinding failed, add direct waypoint
                    processedLocations.add(target)
                    lastLocation = target
                }
            }
            pathQueue.addAll(processedLocations.map { it.clone() })
        } else {
            // Direct path without pathfinding
            pathQueue.addAll(locations.map { it.clone() })
        }
        
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

            // Handle following behavior (both direct following and nearby following)
            if (isFollowing || isFollowingNearbyPlayers) {
                // For nearby following, we might not have a followingEntity yet
                // In that case, let the nearby follow task handle it
                if (isFollowingNearbyPlayers && followingEntity == null) {
                    return@timer // Let nearby follow task find a player
                }
                
                val targetEntity = followingEntity
                if (targetEntity == null || !targetEntity.isValid) {
                    // Target entity is invalid
                    if (isFollowing) {
                        stopFollowing()
                    }
                    return@timer
                }

                val targetLoc = targetEntity.location
                val currentLoc = currentEntity.location
                val distanceToTarget = currentLoc.distance(targetLoc)

                // Check if we're close enough to the target
                // For nearby player following, always try to maintain followDistance
                // For direct following, stop moving when close enough
                if (distanceToTarget <= followDistance && !isFollowingNearbyPlayers) {
                    // Close enough, just look at the target (only for direct following)
                    val lookDirection = targetLoc.toVector().subtract(currentLoc.toVector())
                    val yaw = Math.toDegrees(-Math.atan2(lookDirection.x, lookDirection.z)).toFloat()
                    val newLoc = currentLoc.clone()
                    newLoc.yaw = yaw
                    currentEntity.teleport(newLoc)
                    return@timer
                }
                
                // Update path periodically or if target moved significantly
                // For nearby following, update more frequently to always follow player movement
                followUpdateCounter++
                val updateInterval = if (isFollowingNearbyPlayers) followUpdateInterval / 2 else followUpdateInterval
                val shouldUpdatePath = followUpdateCounter >= updateInterval ||
                        currentTarget == null ||
                        (currentTarget != null && currentTarget!!.distance(targetLoc) > (if (isFollowingNearbyPlayers) 1.5 else 3.0))

                if (shouldUpdatePath) {
                    followUpdateCounter = 0
                    // Recalculate path to target entity
                    updateFollowingPath(currentEntity, targetLoc)
                }

                // Use current target from path
                val target = currentTarget ?: targetLoc
                val distance = currentLoc.distance(target)

                // Check if we've arrived at the current waypoint
                if (distance <= arrivalThreshold) {
                    // Arrived at waypoint, get next one or recalculate
                    if (pathQueue.isEmpty()) {
                        // No more waypoints, recalculate path
                        updateFollowingPath(currentEntity, targetLoc)
                    } else {
                        currentTarget = pathQueue.removeAt(0)
                    }
                } else {
                    // Move towards current waypoint
                    moveTowards(currentEntity, target, walkSpeed)
                }
                return@timer
            }

            // Regular walking behavior (non-following)
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
        isFollowing = false
        followingEntity = null
        currentTarget = null
        pathQueue.clear()
        patrolPath.clear()
        currentPath.clear()
        pathIndex = 0
        followUpdateCounter = 0
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
            NPCEventListener.unregisterVisibilityNPC(this)
        }
        stopFollowingNearbyPlayers()
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

    override fun followEntity(entity: Entity, followDistance: Double): Boolean {
        if (!entity.isValid) return false
        val npcEntity = getMannequin() as? LivingEntity ?: return false

        // Ensure entity can move
        if (npcEntity.isImmovable) {
            npcEntity.isImmovable = false
        }
        
        // Ensure AI is enabled
        npcEntity.setAI(true)

        // Stop any existing walking/patrolling
        if (isPatrolling) {
            stopPatrolling()
        }

        // Set following state
        isFollowing = true
        followingEntity = entity
        this.followDistance = followDistance.coerceAtLeast(1.0) // Minimum 1 block
        followUpdateCounter = 0

        // Calculate initial path
        val currentLoc = npcEntity.location
        val targetLoc = entity.location
        updateFollowingPath(npcEntity, targetLoc)

        // Start walking if not already walking
        if (!isWalking) {
            startWalking()
        }

        return true
    }

    override fun stopFollowing(): Boolean {
        if (!isFollowing) return false

        isFollowing = false
        followingEntity = null
        followUpdateCounter = 0

        // If no current target and no path queue, stop walking completely
        if (currentTarget == null && pathQueue.isEmpty()) {
            stopWalking()
        } else {
            // Clear following-specific state but keep walking to current target
            currentTarget = null
            pathQueue.clear()
            currentPath.clear()
            pathIndex = 0
        }

        return true
    }

    override fun isFollowingEntity(): Boolean {
        return isFollowing && followingEntity != null && followingEntity!!.isValid
    }

    override fun getFollowingEntity(): Entity? {
        return if (isFollowingEntity()) followingEntity else null
    }

    override fun followNearbyPlayers(range: Double, followDistance: Double): Boolean {
        val npcEntity = getMannequin() as? LivingEntity ?: return false
        
        // Ensure entity can move
        if (npcEntity.isImmovable) {
            npcEntity.isImmovable = false
        }
        
        // Ensure AI is enabled
        npcEntity.setAI(true)
        
        // Stop any existing nearby following
        if (isFollowingNearbyPlayers) {
            isFollowingNearbyPlayers = false
            nearbyFollowTask?.cancel()
            nearbyFollowTask = null
            if (isFollowing && followingEntity is Player) {
                stopFollowing()
            }
        }
        
        // Stop patrolling if active
        if (isPatrolling) {
            stopPatrolling()
        }
        
        // Set spawn location to current location if not already set
        if (spawnLocation == null) {
            spawnLocation = npcEntity.location.clone()
        }
        
        // Set following state
        isFollowingNearbyPlayers = true
        nearbyFollowRange = range.coerceAtLeast(1.0)
        nearbyFollowDistance = followDistance.coerceAtLeast(1.0)
        
        // Immediately check for nearby players before starting the timer
        val currentLoc = npcEntity.location
        val world = currentLoc.world
        if (world != null) {
            val nearbyPlayers = world.getNearbyEntities(
                currentLoc,
                nearbyFollowRange,
                nearbyFollowRange,
                nearbyFollowRange
            ).filterIsInstance<Player>()
                .filter { it.isValid && it.location.distance(currentLoc) <= nearbyFollowRange }
            
            val targetPlayer = nearbyPlayers.firstOrNull()
            if (targetPlayer != null) {
                // Found a nearby player immediately, start following
                followEntity(targetPlayer, nearbyFollowDistance)
            } else {
                // No nearby players, go to spawn
                val spawn = spawnLocation ?: currentLoc.clone()
                walkTo(spawn)
            }
        }
        
        // Start monitoring task
        nearbyFollowTask = timer(nearbyFollowCheckInterval, "NPCNearbyFollow") {
            if (!isFollowingNearbyPlayers) {
                nearbyFollowTask?.cancel()
                nearbyFollowTask = null
                return@timer
            }
            
            val currentEntity = getMannequin() as? LivingEntity ?: run {
                stopFollowingNearbyPlayers()
                return@timer
            }
            
            val currentLoc = currentEntity.location
            val world = currentLoc.world ?: return@timer
            
            // Find nearby players
            val nearbyPlayers = world.getNearbyEntities(
                currentLoc,
                nearbyFollowRange,
                nearbyFollowRange,
                nearbyFollowRange
            ).filterIsInstance<Player>()
                .filter { it.isValid && it.location.distance(currentLoc) <= nearbyFollowRange }
            
            val currentFollowing = followingEntity as? Player
            
            // Check if current followed player is still in range
            if (currentFollowing != null && currentFollowing.isValid) {
                val distanceToCurrent = currentLoc.distance(currentFollowing.location)
                if (distanceToCurrent <= nearbyFollowRange) {
                    // Still in range, continue following
                    return@timer
                } else {
                    // Current player went out of range, stop following them
                    stopFollowing()
                }
            }
            
            // Find a new player to follow
            val targetPlayer = nearbyPlayers.firstOrNull()
            
            if (targetPlayer != null) {
                // Found a nearby player, follow them
                followEntity(targetPlayer, nearbyFollowDistance)
            } else {
                // No nearby players, return to spawn
                val spawn = spawnLocation ?: return@timer
                val distanceToSpawn = currentLoc.distance(spawn)
                
                if (distanceToSpawn > 1.5) {
                    // Not at spawn yet, walk to spawn
                    if (!isWalking || currentTarget == null || currentTarget!!.distance(spawn) > 2.0) {
                        walkTo(spawn)
                    }
                } else {
                    // At spawn, stop walking
                    if (isWalking && !isFollowing) {
                        stopWalking()
                    }
                }
            }
        }
        
        return true
    }

    override fun stopFollowingNearbyPlayers(): Boolean {
        if (!isFollowingNearbyPlayers) return false
        
        isFollowingNearbyPlayers = false
        nearbyFollowTask?.cancel()
        nearbyFollowTask = null
        
        // Stop following current entity if it was from nearby following
        if (isFollowing && followingEntity is Player) {
            stopFollowing()
        }
        
        return true
    }

    override fun setSpawnLocation(location: Location) {
        spawnLocation = location.clone()
    }

    override fun getSpawnLocation(): Location? {
        return spawnLocation?.clone()
    }

    override fun isFollowingNearbyPlayers(): Boolean {
        return isFollowingNearbyPlayers
    }

    /**
     * Updates the path when following an entity.
     * Recalculates the path using pathfinding if enabled.
     */
    private fun updateFollowingPath(npcEntity: LivingEntity, targetLoc: Location) {
        val currentLoc = npcEntity.location

        // Clear existing path
        pathQueue.clear()
        currentPath.clear()
        pathIndex = 0

        // Use pathfinding if enabled
        if (usePathfinding) {
            val path = Pathfinder.findPath(currentLoc, targetLoc)
            if (path.isNotEmpty()) {
                // Add all waypoints except the first (current position) to the queue
                currentPath.addAll(path)
                if (currentPath.size > 1) {
                    pathQueue.addAll(currentPath.subList(1, currentPath.size))
                } else {
                    pathQueue.add(targetLoc.clone())
                }
            } else {
                // Pathfinding failed, use direct path
                pathQueue.add(targetLoc.clone())
            }
        } else {
            // Direct path without pathfinding
            pathQueue.add(targetLoc.clone())
        }

        // Set first location as target
        if (pathQueue.isNotEmpty()) {
            currentTarget = pathQueue.removeAt(0)
        } else {
            currentTarget = targetLoc.clone()
        }
    }

    private fun moveTowards(entity: LivingEntity, target: Location, speed: Double) {
        val currentLoc = entity.location
        val direction = target.toVector().subtract(currentLoc.toVector())
        val distance = direction.length()
        
        // If very close, just look at target
        if (distance < 0.1) {
            val lookDirection = direction.normalize()
            val yaw = Math.toDegrees(-atan2(lookDirection.x, lookDirection.z)).toFloat()
            val newLoc = currentLoc.clone()
            newLoc.yaw = yaw
            entity.teleport(newLoc)
            return
        }
        
        val normalizedDirection = direction.normalize()
        val movementVector = normalizedDirection.multiply(speed)

        // Check if NPC needs to jump
        if (usePathfinding && Pathfinder.needsJump(entity, target)) {
            // Apply upward velocity for jumping
            val jumpVelocity = Vector(0.0, 0.42, 0.0) // Standard jump velocity
            entity.velocity = movementVector.add(jumpVelocity)
        } else {
            // Normal movement
            val heightDiff = target.y - currentLoc.y
            if (heightDiff > 0.1 && heightDiff <= 0.5) {
                // Small step up, add slight upward velocity
                val stepVelocity = Vector(0.0, 0.2, 0.0)
                entity.velocity = movementVector.add(stepVelocity)
            } else {
                entity.velocity = movementVector
            }
        }

        // Make entity look at target (don't teleport, let velocity handle movement)
        val lookDirection = target.toVector().subtract(currentLoc.toVector())
        val yaw = Math.toDegrees(-atan2(lookDirection.x, lookDirection.z)).toFloat()
        val pitch = Math.toDegrees(-Math.asin(lookDirection.y / lookDirection.length())).toFloat()
        
        // Update rotation without teleporting (to not override velocity)
        val rotationLoc = currentLoc.clone()
        rotationLoc.yaw = yaw
        rotationLoc.pitch = pitch.coerceIn(-90f, 90f)
        entity.teleport(rotationLoc) // Only teleport for rotation, velocity handles position
    }
    
    /**
     * Enable or disable pathfinding for this NPC.
     * When enabled, NPCs will use A* pathfinding to navigate around obstacles and jump when needed.
     * When disabled, NPCs will move directly towards targets.
     */
    override fun setPathfindingEnabled(enabled: Boolean) {
        usePathfinding = enabled
    }
    
    /**
     * Check if pathfinding is enabled for this NPC.
     */
    override fun isPathfindingEnabled(): Boolean {
        return usePathfinding
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

    override fun setVisibleToAllPlayers(visible: Boolean) {
        val entity = getMannequin() ?: return
        
        if (visible) {
            // Make visible to all players
            visibleToPlayers = null
            // Show entity to all online players
            Bukkit.getOnlinePlayers().forEach { player ->
                player.showEntity(PluginInstance, entity)
            }
            // Unregister from visibility tracking since visible to all
            NPCEventListener.unregisterVisibilityNPC(this)
        } else {
            // Hide from all players (set empty set)
            val currentVisible = visibleToPlayers ?: mutableSetOf()
            Bukkit.getOnlinePlayers().forEach { player ->
                if (!currentVisible.contains(player)) {
                    player.hideEntity(PluginInstance, entity)
                }
            }
            visibleToPlayers = mutableSetOf()
            // Register for visibility tracking
            NPCEventListener.registerVisibilityNPC(this)
        }
    }

    override fun setVisibleToPlayers(players: Set<Player>) {
        val entity = getMannequin() ?: return
        
        val oldVisible = visibleToPlayers
        visibleToPlayers = if (players.isEmpty()) {
            null // Empty set means visible to all
        } else {
            players.toMutableSet()
        }
        
        // Update visibility for all online players
        Bukkit.getOnlinePlayers().forEach { player ->
            val shouldBeVisible = visibleToPlayers == null || visibleToPlayers!!.contains(player)
            val wasVisible = oldVisible == null || oldVisible.contains(player)
            
            if (shouldBeVisible && !wasVisible) {
                // Show entity to this player
                player.showEntity(PluginInstance, entity)
            } else if (!shouldBeVisible && wasVisible) {
                // Hide entity from this player
                player.hideEntity(PluginInstance, entity)
            }
        }
        
        // Register/unregister for visibility tracking
        if (visibleToPlayers == null) {
            NPCEventListener.unregisterVisibilityNPC(this)
        } else {
            NPCEventListener.registerVisibilityNPC(this)
        }
    }

    override fun addVisiblePlayer(player: Player) {
        val entity = getMannequin() ?: return
        
        if (visibleToPlayers == null) {
            // Currently visible to all, switch to specific list
            // Hide from all players first
            Bukkit.getOnlinePlayers().forEach { p ->
                if (p != player) {
                    p.hideEntity(PluginInstance, entity)
                }
            }
            visibleToPlayers = mutableSetOf(player)
            // Register for visibility tracking
            NPCEventListener.registerVisibilityNPC(this)
        } else {
            visibleToPlayers!!.add(player)
        }
        
        // Show entity to this player
        player.showEntity(PluginInstance, entity)
    }

    override fun removeVisiblePlayer(player: Player) {
        val entity = getMannequin() ?: return
        
        if (visibleToPlayers == null) {
            // Currently visible to all, switch to list without this player
            visibleToPlayers = Bukkit.getOnlinePlayers().filter { it != player }.toMutableSet()
            // Register for visibility tracking
            NPCEventListener.registerVisibilityNPC(this)
        } else {
            visibleToPlayers!!.remove(player)
            // If list becomes empty, make visible to all
            if (visibleToPlayers!!.isEmpty()) {
                visibleToPlayers = null
                Bukkit.getOnlinePlayers().forEach { p ->
                    p.showEntity(PluginInstance, entity)
                }
                // Unregister from visibility tracking
                NPCEventListener.unregisterVisibilityNPC(this)
                return
            }
        }
        
        // Hide entity from this player
        player.hideEntity(PluginInstance, entity)
    }

    override fun getVisiblePlayers(): Set<Player>? {
        return visibleToPlayers?.toSet()
    }

    override fun isVisibleToAllPlayers(): Boolean {
        return visibleToPlayers == null
    }

    /**
     * Internal method called when a player joins the server.
     * Updates visibility for the new player.
     */
    internal fun onPlayerJoin(player: Player) {
        val entity = getMannequin() ?: return
        
        if (visibleToPlayers == null) {
            // Visible to all, show to new player
            player.showEntity(PluginInstance, entity)
        } else if (visibleToPlayers!!.contains(player)) {
            // Player is in visible list, show to them
            player.showEntity(PluginInstance, entity)
        } else {
            // Player not in visible list, ensure hidden
            player.hideEntity(PluginInstance, entity)
        }
    }

    /**
     * Internal method called when a player leaves the server.
     * Cleans up visibility tracking.
     */
    internal fun onPlayerQuit(player: Player) {
        // Remove from visible players set if present
        visibleToPlayers?.remove(player)
    }
}

