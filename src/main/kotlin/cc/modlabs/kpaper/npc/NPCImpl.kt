package cc.modlabs.kpaper.npc

import cc.modlabs.kpaper.extensions.timer
import cc.modlabs.kpaper.util.logDebug
import dev.fruxz.stacked.text
import net.kyori.adventure.text.Component
import cc.modlabs.kpaper.main.PluginInstance
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
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
    private var nearbyFollowCheckInterval = 10L // Ticks between checking for nearby players (0.5 seconds - more responsive)

    // Visibility state
    // null = visible to all players, non-null = only visible to players in the set
    private var visibleToPlayers: MutableSet<Player>? = null

    init {
        val npcName = mannequin.customName ?: mannequin.type.name
        val npcId = mannequin.uniqueId
        
        // Enable AI for the mannequin so it can move
        // Mannequin extends LivingEntity, so we can directly enable AI
        val aiWasEnabled = mannequin.hasAI()
        mannequin.setAI(true)
        val aiNowEnabled = mannequin.hasAI()
        
        logDebug("[NPC] NPCImpl init: Created NPC '$npcName' ($npcId)")
        logDebug("[NPC] NPCImpl init: AI was $aiWasEnabled, now $aiNowEnabled, immovable=${mannequin.isImmovable}")

        // Register this NPC for event tracking
        NPCEventListener.registerNPC(mannequin, this)
        logDebug("[NPC] NPCImpl init: Registered NPC '$npcName' for event tracking")
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
        logDebug("[NPC] startWalking called")
        val entity = getMannequin() as? LivingEntity ?: run {
            logDebug("[NPC] startWalking failed: cannot get LivingEntity")
            return false
        }

        // Enable AI if not already enabled
        val aiWasEnabled = entity.hasAI()
        entity.setAI(true)
        logDebug("[NPC] startWalking: AI was $aiWasEnabled, now enabled: ${entity.hasAI()}, immovable: ${isImmovable()}")

        // If already walking, don't start again
        if (isWalking && walkingTask != null) {
            logDebug("[NPC] startWalking: Already walking, skipping")
            return true
        }

        isWalking = true
        isPaused = false
        logDebug("[NPC] startWalking: Starting walking task - isWalking=$isWalking, isPaused=$isPaused, isFollowing=$isFollowing, isFollowingNearbyPlayers=$isFollowingNearbyPlayers")

        // Start the walking task
        walkingTask = timer(1, "NPCWalking") {
            // Check pause state - use patrol pause if patrolling, otherwise regular pause
            val shouldPause = if (isPatrolling) isPatrolPaused else isPaused
            if (!isWalking || shouldPause) {
                if (shouldPause) {
                    logDebug("[NPC] Walking task: Paused")
                }
                return@timer
            }

            val currentEntity = getMannequin() as? LivingEntity ?: run {
                logDebug("[NPC] Walking task: Entity invalid, stopping")
                stopWalking()
                return@timer
            }

            // Handle following behavior (both direct following and nearby following)
            if (isFollowing || isFollowingNearbyPlayers) {
                // For nearby following, we might not have a followingEntity yet
                // In that case, the nearby follow task will handle finding players
                // The walking task should just wait - nearby follow task will call followEntity when it finds a player
                if (isFollowingNearbyPlayers && followingEntity == null) {
                    logDebug("[NPC] Walking task: Nearby following active but no followingEntity yet - waiting for nearby follow task to find player")
                    // Just wait - nearby follow task will handle everything
                    // Don't try to go to spawn here, let the nearby follow task handle it
                    return@timer
                }

                val targetEntity = followingEntity
                if (targetEntity == null || !targetEntity.isValid) {
                    logDebug("[NPC] Walking task: Target entity is null or invalid")
                    // Target entity is invalid
                    if (isFollowing) {
                        stopFollowing()
                    }
                    return@timer
                }

                val targetLoc = targetEntity.location
                val currentLoc = currentEntity.location
                val distanceToTarget = currentLoc.distance(targetLoc)

                logDebug("[NPC] Walking task: Following - distanceToTarget=$distanceToTarget, followDistance=$followDistance, isFollowingNearbyPlayers=$isFollowingNearbyPlayers")

                // Check if we're close enough to the target
                // For nearby player following, always try to maintain followDistance
                // For direct following, stop moving when close enough
                if (distanceToTarget <= followDistance) {
                    if (!isFollowingNearbyPlayers) {
                        logDebug("[NPC] Walking task: Close enough to target (direct following), just looking")
                        // Close enough, continuously look at the target (only for direct following)
                        // Update look direction every tick to track player movement
                        val lookDirection = targetLoc.toVector().subtract(currentLoc.toVector())
                        val horizontalDistance = Math.sqrt(lookDirection.x * lookDirection.x + lookDirection.z * lookDirection.z)
                        val yaw = Math.toDegrees(-Math.atan2(lookDirection.x, lookDirection.z)).toFloat()
                        val pitch = Math.toDegrees(-Math.atan2(lookDirection.y, horizontalDistance)).toFloat().coerceIn(-90f, 90f)
                        val newLoc = currentLoc.clone()
                        newLoc.yaw = yaw
                        newLoc.pitch = pitch
                        currentEntity.teleport(newLoc)
                        return@timer
                    } else {
                        // For nearby following, still look at player when close enough, but continue following
                        // This ensures the NPC looks at the player even when maintaining distance
                        val lookDirection = targetLoc.toVector().subtract(currentLoc.toVector())
                        val horizontalDistance = Math.sqrt(lookDirection.x * lookDirection.x + lookDirection.z * lookDirection.z)
                        val yaw = Math.toDegrees(-Math.atan2(lookDirection.x, lookDirection.z)).toFloat()
                        val pitch = Math.toDegrees(-Math.atan2(lookDirection.y, horizontalDistance)).toFloat().coerceIn(-90f, 90f)
                        val newLoc = currentLoc.clone()
                        newLoc.yaw = yaw
                        newLoc.pitch = pitch
                        currentEntity.teleport(newLoc)
                        // Continue with movement logic below to maintain followDistance
                    }
                }

                // Update path periodically or if target moved significantly
                // For nearby following, update more frequently to always follow player movement
                followUpdateCounter++
                val updateInterval = if (isFollowingNearbyPlayers) followUpdateInterval / 2 else followUpdateInterval
                val shouldUpdatePath = followUpdateCounter >= updateInterval ||
                        currentTarget == null ||
                        (currentTarget != null && currentTarget!!.distance(targetLoc) > (if (isFollowingNearbyPlayers) 1.5 else 3.0))

                if (shouldUpdatePath) {
                    logDebug("[NPC] Walking task: Updating path - followUpdateCounter=$followUpdateCounter, updateInterval=$updateInterval")
                    followUpdateCounter = 0
                    // Recalculate path to target entity
                    updateFollowingPath(currentEntity, targetLoc)
                }

                // Use current target from path
                val target = currentTarget ?: targetLoc
                val distance = currentLoc.distance(target)

                logDebug("[NPC] Walking task: Moving - currentTarget=${currentTarget?.blockX},${currentTarget?.blockY},${currentTarget?.blockZ}, distance=$distance, arrivalThreshold=$arrivalThreshold, pathQueue.size=${pathQueue.size}")

                // Check if we've arrived at the current waypoint
                if (distance <= arrivalThreshold) {
                    logDebug("[NPC] Walking task: Arrived at waypoint")
                    // Arrived at waypoint, get next one or recalculate
                    if (pathQueue.isEmpty()) {
                        logDebug("[NPC] Walking task: No more waypoints, recalculating path")
                        // No more waypoints, recalculate path
                        updateFollowingPath(currentEntity, targetLoc)
                    } else {
                        currentTarget = pathQueue.removeAt(0)
                        logDebug("[NPC] Walking task: Next waypoint set to ${currentTarget?.blockX},${currentTarget?.blockY},${currentTarget?.blockZ}")
                    }
                } else {
                    // Move towards current waypoint
                    logDebug("[NPC] Walking task: Calling moveTowards - target=${target.blockX},${target.blockY},${target.blockZ}, speed=$walkSpeed")
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
        logDebug("[NPC] followEntity called: entity=${entity.type}, followDistance=$followDistance")

        if (!entity.isValid) {
            logDebug("[NPC] followEntity failed: entity is invalid")
            return false
        }
        val npcEntity = getMannequin() as? LivingEntity ?: run {
            logDebug("[NPC] followEntity failed: cannot get LivingEntity")
            return false
        }

        // Ensure entity can move
        if (isImmovable()) {
            logDebug("[NPC] followEntity: Setting immovable to false")
            setImmovable(false)
        }

        // Ensure AI is enabled
        val aiEnabled = npcEntity.hasAI()
        npcEntity.setAI(true)
        logDebug("[NPC] followEntity: AI was $aiEnabled, now enabled: ${npcEntity.hasAI()}")

        // Stop any existing walking/patrolling
        if (isPatrolling) {
            logDebug("[NPC] followEntity: Stopping patrolling")
            stopPatrolling()
        }

        // Set following state
        isFollowing = true
        followingEntity = entity
        this.followDistance = followDistance.coerceAtLeast(1.0) // Minimum 1 block
        followUpdateCounter = 0
        logDebug("[NPC] followEntity: Following state set - isFollowing=$isFollowing, followDistance=${this.followDistance}")

        // Calculate initial path
        val currentLoc = npcEntity.location
        val targetLoc = entity.location
        val distance = currentLoc.distance(targetLoc)
        logDebug("[NPC] followEntity: Current location=${currentLoc.blockX},${currentLoc.blockY},${currentLoc.blockZ}, Target location=${targetLoc.blockX},${targetLoc.blockY},${targetLoc.blockZ}, Distance=$distance")

        updateFollowingPath(npcEntity, targetLoc)
        logDebug("[NPC] followEntity: Path calculated - pathQueue.size=${pathQueue.size}, currentTarget=${currentTarget?.blockX},${currentTarget?.blockY},${currentTarget?.blockZ}")

        // Start walking if not already walking
        if (!isWalking) {
            logDebug("[NPC] followEntity: Starting walking task")
            startWalking()
        } else {
            logDebug("[NPC] followEntity: Walking task already running")
        }

        logDebug("[NPC] followEntity: Successfully started following")
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
        logDebug("[NPC] followNearbyPlayers called: range=$range, followDistance=$followDistance")
        val npcEntity = getMannequin() as? LivingEntity ?: run {
            logDebug("[NPC] followNearbyPlayers failed: cannot get LivingEntity")
            return false
        }

        // Ensure entity can move
        if (isImmovable()) {
            logDebug("[NPC] followNearbyPlayers: Setting immovable to false")
            setImmovable(false)
        }

        // Ensure AI is enabled
        val aiEnabled = npcEntity.hasAI()
        npcEntity.setAI(true)
        logDebug("[NPC] followNearbyPlayers: AI was $aiEnabled, now enabled: ${npcEntity.hasAI()}")

        // Stop any existing nearby following
        if (isFollowingNearbyPlayers) {
            logDebug("[NPC] followNearbyPlayers: Stopping existing nearby following")
            isFollowingNearbyPlayers = false
            nearbyFollowTask?.cancel()
            nearbyFollowTask = null
            if (isFollowing && followingEntity is Player) {
                stopFollowing()
            }
        }

        // Stop patrolling if active
        if (isPatrolling) {
            logDebug("[NPC] followNearbyPlayers: Stopping patrolling")
            stopPatrolling()
        }

        // Set spawn location to current location if not already set
        if (spawnLocation == null) {
            spawnLocation = npcEntity.location.clone()
            logDebug("[NPC] followNearbyPlayers: Set spawn location to ${spawnLocation?.blockX},${spawnLocation?.blockY},${spawnLocation?.blockZ}")
        }

        // Set following state
        isFollowingNearbyPlayers = true
        nearbyFollowRange = range.coerceAtLeast(1.0)
        nearbyFollowDistance = followDistance.coerceAtLeast(1.0)
        logDebug("[NPC] followNearbyPlayers: State set - range=$nearbyFollowRange, followDistance=$nearbyFollowDistance")

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

            logDebug("[NPC] followNearbyPlayers: Found ${nearbyPlayers.size} nearby players")

            val targetPlayer = nearbyPlayers.firstOrNull()
            if (targetPlayer != null) {
                // Found a nearby player immediately, start following
                logDebug("[NPC] followNearbyPlayers: Found player ${targetPlayer.name}, starting to follow")
                followEntity(targetPlayer, nearbyFollowDistance)
            } else {
                // No nearby players, go to spawn
                val spawn = spawnLocation ?: currentLoc.clone()
                logDebug("[NPC] followNearbyPlayers: No players found, going to spawn at ${spawn.blockX},${spawn.blockY},${spawn.blockZ}")
                walkTo(spawn)
            }
        } else {
            logDebug("[NPC] followNearbyPlayers: World is null!")
        }

        // Start monitoring task - this will run continuously every nearbyFollowCheckInterval ticks
        logDebug("[NPC] followNearbyPlayers: Starting monitoring task with interval=$nearbyFollowCheckInterval ticks (${nearbyFollowCheckInterval * 50}ms)")
        nearbyFollowTask = timer(nearbyFollowCheckInterval, "NPCNearbyFollow") {
            // Log every tick to confirm task is running
            logDebug("[NPC] NearbyFollow task: [TICK START] isFollowingNearbyPlayers=$isFollowingNearbyPlayers")

            // Wrap everything in try-catch to ensure task never stops on error
            try {
                // Only cancel if explicitly stopped
                if (!isFollowingNearbyPlayers) {
                    logDebug("[NPC] NearbyFollow task: isFollowingNearbyPlayers=false, canceling task")
                    nearbyFollowTask?.cancel()
                    nearbyFollowTask = null
                    return@timer
                }

                val currentEntity = getMannequin() as? LivingEntity
                if (currentEntity == null || !currentEntity.isValid) {
                    logDebug("[NPC] NearbyFollow task: Entity invalid, but continuing to check")
                    // Don't stop the task, just skip this tick
                    return@timer
                }

                val npcLocation = currentEntity.location
                val npcWorld = npcLocation.world
                if (npcWorld == null) {
                    logDebug("[NPC] NearbyFollow task: World is null, but continuing to check")
                    // Don't stop the task, just skip this tick
                    return@timer
                }

                logDebug("[NPC] NearbyFollow task: [TICK] Checking for players at ${npcLocation.blockX},${npcLocation.blockY},${npcLocation.blockZ}, range=$nearbyFollowRange")

                // Find nearby players - check ALL online players in the world, not just nearby entities
                // This ensures we detect players that just joined or moved into range
                val nearbyPlayers = mutableListOf<Player>()

                // Method 1: Check nearby entities (faster for large worlds)
                val allNearbyEntities = npcWorld.getNearbyEntities(npcLocation, nearbyFollowRange, nearbyFollowRange, nearbyFollowRange)
                logDebug("[NPC] NearbyFollow task: Found ${allNearbyEntities.size} total entities nearby")

                for (entity in allNearbyEntities) {
                    if (entity is Player && entity.isValid && !entity.isDead) {
                        val distance = entity.location.distance(npcLocation)
                        if (distance <= nearbyFollowRange) {
                            nearbyPlayers.add(entity)
                            logDebug("[NPC] NearbyFollow task: Found nearby player ${entity.name} at distance=$distance")
                        }
                    }
                }

                // Method 2: Also check all online players in the same world (catches players that just joined)
                // This is a fallback to ensure we don't miss anyone
                if (nearbyPlayers.isEmpty()) {
                    for (player in Bukkit.getOnlinePlayers()) {
                        if (player.world == npcWorld && player.isValid && !player.isDead) {
                            val distance = player.location.distance(npcLocation)
                            if (distance <= nearbyFollowRange) {
                                if (!nearbyPlayers.contains(player)) {
                                    nearbyPlayers.add(player)
                                    logDebug("[NPC] NearbyFollow task: Found player ${player.name} via online check at distance=$distance")
                                }
                            }
                        }
                    }
                }

                logDebug("[NPC] NearbyFollow task: Found ${nearbyPlayers.size} valid players within range")

                val currentFollowing = followingEntity as? Player

                // Check if current followed player is still in range
                if (currentFollowing != null && currentFollowing.isValid && !currentFollowing.isDead) {
                    val distanceToCurrent = npcLocation.distance(currentFollowing.location)
                    logDebug("[NPC] NearbyFollow task: Currently following ${currentFollowing.name}, distance=$distanceToCurrent, range=$nearbyFollowRange")

                    if (distanceToCurrent <= nearbyFollowRange) {
                        // Still in range, but check if there's a closer player
                        val closerPlayer = nearbyPlayers.minByOrNull { it.location.distance(npcLocation) }
                        if (closerPlayer != null && closerPlayer != currentFollowing) {
                            val distanceToCloser = npcLocation.distance(closerPlayer.location)
                            // Switch to closer player if they're significantly closer (at least 2 blocks)
                            if (distanceToCloser < distanceToCurrent - 2.0) {
                                logDebug("[NPC] NearbyFollow task: Found closer player ${closerPlayer.name} (distance=$distanceToCloser vs $distanceToCurrent), switching")
                                stopFollowing()
                                followEntity(closerPlayer, nearbyFollowDistance)
                                return@timer
                            }
                        }
                        // Still in range, continue following - task will check again next tick
                        logDebug("[NPC] NearbyFollow task: Player still in range, continuing to follow")
                        return@timer
                    } else {
                        // Current player went out of range, stop following them
                        logDebug("[NPC] NearbyFollow task: Player went out of range, stopping follow")
                        stopFollowing()
                    }
                }

                // Find a new player to follow (closest one)
                val targetPlayer = nearbyPlayers.minByOrNull { it.location.distance(npcLocation) }

                if (targetPlayer != null) {
                    // Found a nearby player, follow them
                    val distance = npcLocation.distance(targetPlayer.location)
                    logDebug("[NPC] NearbyFollow task: Found new player ${targetPlayer.name} at distance=$distance, starting to follow")
                    followEntity(targetPlayer, nearbyFollowDistance)
                } else {
                    // No nearby players, return to spawn
                    val spawn = spawnLocation
                    if (spawn == null) {
                        logDebug("[NPC] NearbyFollow task: No spawn location set, waiting for players")
                        // Don't return - let task continue checking
                        return@timer
                    }

                    val distanceToSpawn = npcLocation.distance(spawn)
                    logDebug("[NPC] NearbyFollow task: No players found, distance to spawn=$distanceToSpawn")

                    // Stop following current entity if we were following one
                    if (isFollowing && followingEntity != null) {
                        logDebug("[NPC] NearbyFollow task: No players nearby, stopping current follow")
                        stopFollowing()
                    }

                    if (distanceToSpawn > 1.5) {
                        // Not at spawn yet, walk to spawn
                        logDebug("[NPC] NearbyFollow task: Walking to spawn")
                        if (!isWalking || currentTarget == null || currentTarget!!.distance(spawn) > 2.0) {
                            walkTo(spawn)
                        }
                    } else {
                        // At spawn - keep task running to detect players
                        logDebug("[NPC] NearbyFollow task: At spawn, waiting for players (task continues checking)")
                        // Ensure walking task is running so we can respond immediately when a player comes
                        if (!isWalking) {
                            logDebug("[NPC] NearbyFollow task: Starting walking task to be ready for players")
                            startWalking()
                        }
                    }
                }

                // Task will automatically continue to next tick - timer handles scheduling
                logDebug("[NPC] NearbyFollow task: [TICK COMPLETE] Will check again in ${nearbyFollowCheckInterval} ticks")

            } catch (e: Exception) {
                // Log error but don't stop the task - it will continue checking
                logDebug("[NPC] NearbyFollow task: ERROR in tick - ${e.message}, but task will continue")
                e.printStackTrace()
                // Task continues to next tick automatically
            }

            // Explicitly log that we're returning and the task should continue
            // The timer will automatically schedule the next run
        }

        // Verify the task was created
        if (nearbyFollowTask == null) {
            logDebug("[NPC] followNearbyPlayers: ERROR - Task was not created!")
        } else {
            logDebug("[NPC] followNearbyPlayers: Monitoring task started successfully, taskId=${nearbyFollowTask?.taskId}, will run every ${nearbyFollowCheckInterval} ticks")

            // Add a verification log after a short delay to confirm it's still running
            Bukkit.getScheduler().runTaskLater(PluginInstance, Runnable {
                if (nearbyFollowTask?.isCancelled == true) {
                    logDebug("[NPC] followNearbyPlayers: WARNING - Task was canceled after ${nearbyFollowCheckInterval * 2} ticks!")
                } else {
                    logDebug("[NPC] followNearbyPlayers: Task is still active after ${nearbyFollowCheckInterval * 2} ticks")
                }
            }, nearbyFollowCheckInterval * 2)
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
        logDebug("[NPC] updateFollowingPath: from=${currentLoc.blockX},${currentLoc.blockY},${currentLoc.blockZ} to=${targetLoc.blockX},${targetLoc.blockY},${targetLoc.blockZ}, usePathfinding=$usePathfinding")

        // Clear existing path
        pathQueue.clear()
        currentPath.clear()
        pathIndex = 0

        // Use pathfinding if enabled
        if (usePathfinding) {
            val path = Pathfinder.findPath(currentLoc, targetLoc)
            logDebug("[NPC] updateFollowingPath: Pathfinding returned ${path.size} waypoints")
            if (path.isNotEmpty()) {
                // Add all waypoints except the first (current position) to the queue
                currentPath.addAll(path)
                if (currentPath.size > 1) {
                    pathQueue.addAll(currentPath.subList(1, currentPath.size))
                } else {
                    pathQueue.add(targetLoc.clone())
                }
                logDebug("[NPC] updateFollowingPath: Added ${pathQueue.size} waypoints to queue")
            } else {
                logDebug("[NPC] updateFollowingPath: Pathfinding failed, using direct path")
                // Pathfinding failed, use direct path
                pathQueue.add(targetLoc.clone())
            }
        } else {
            logDebug("[NPC] updateFollowingPath: Pathfinding disabled, using direct path")
            // Direct path without pathfinding
            pathQueue.add(targetLoc.clone())
        }

        // Set first location as target
        if (pathQueue.isNotEmpty()) {
            currentTarget = pathQueue.removeAt(0)
            logDebug("[NPC] updateFollowingPath: Set currentTarget to ${currentTarget?.blockX},${currentTarget?.blockY},${currentTarget?.blockZ}, remaining waypoints=${pathQueue.size}")
        } else {
            currentTarget = targetLoc.clone()
            logDebug("[NPC] updateFollowingPath: No waypoints, set currentTarget to target location")
        }
    }

    private fun moveTowards(entity: LivingEntity, target: Location, speed: Double) {
        val currentLoc = entity.location
        val direction = target.toVector().subtract(currentLoc.toVector())
        val distance = direction.length()

        logDebug("[NPC] moveTowards: current=${currentLoc.blockX},${currentLoc.blockY},${currentLoc.blockZ}, target=${target.blockX},${target.blockY},${target.blockZ}, distance=$distance, speed=$speed")

        // If very close, just look at target
        if (distance < 0.1) {
            logDebug("[NPC] moveTowards: Very close (<0.1), just rotating")
            val lookDirection = direction.normalize()
            val yaw = Math.toDegrees(-atan2(lookDirection.x, lookDirection.z)).toFloat()
            val newLoc = currentLoc.clone()
            newLoc.yaw = yaw
            entity.teleport(newLoc)
            return
        }

        // Use hybrid approach: teleport for horizontal movement, simulate gravity for vertical
        val normalizedDirection = direction.normalize()
        val moveDistance = speed.coerceAtMost(distance) // Don't overshoot the target

        // Calculate horizontal movement (X, Z only)
        val horizontalDirection = Vector(normalizedDirection.x, 0.0, normalizedDirection.z).normalize()
        val horizontalMove = horizontalDirection.multiply(moveDistance)
        val newPosition = currentLoc.clone().add(horizontalMove)

        // Check if NPC needs to jump
        val needsJump = usePathfinding && Pathfinder.needsJump(entity, target)

        // Check for ground below the new horizontal position
        val world = currentLoc.world
        val groundY = findGroundLevel(world, newPosition.blockX, newPosition.blockZ, currentLoc.blockY.toInt())

        // Handle vertical positioning
        if (needsJump) {
            logDebug("[NPC] moveTowards: Needs to jump, adding jump height")
            // Jump: move up from current position
            newPosition.y = currentLoc.y + 0.42
        } else {
            val heightDiff = target.y - currentLoc.y
            if (heightDiff > 0.1 && heightDiff <= 0.5) {
                logDebug("[NPC] moveTowards: Small step up (heightDiff=$heightDiff), adding step height")
                // Small step up
                newPosition.y = currentLoc.y + 0.2
            } else if (groundY != null) {
                // There's ground below - check if we should be on it or falling to it
                val distanceToGround = currentLoc.y - groundY
                if (distanceToGround > 0.1) {
                    // We're above ground - simulate gravity (fall down)
                    // Gravity: 0.08 blocks per tick (1.6 blocks per second)
                    val fallDistance = 0.08.coerceAtMost(distanceToGround - 0.1)
                    newPosition.y = currentLoc.y - fallDistance
                    logDebug("[NPC] moveTowards: Falling - distanceToGround=$distanceToGround, fallDistance=$fallDistance, newY=${newPosition.y}")
                } else if (distanceToGround < -0.1) {
                    // We're below ground - place on ground
                    newPosition.y = groundY
                    logDebug("[NPC] moveTowards: Below ground, placed on ground at Y=$groundY")
                } else {
                    // We're on ground - stay there
                    newPosition.y = currentLoc.y
                    logDebug("[NPC] moveTowards: On ground, maintaining Y=${newPosition.y}")
                }
            } else {
                // No ground found - keep current Y (might be in air, let it fall naturally if gravity is enabled)
                newPosition.y = currentLoc.y
                logDebug("[NPC] moveTowards: No ground found, keeping current Y=${newPosition.y}")
            }
        }

        // Make entity look at target
        val lookDirection = target.toVector().subtract(newPosition.toVector())
        val yaw = Math.toDegrees(-atan2(lookDirection.x, lookDirection.z)).toFloat()
        val pitch = Math.toDegrees(-Math.asin(lookDirection.y / lookDirection.length())).toFloat()

        newPosition.yaw = yaw
        newPosition.pitch = pitch.coerceIn(-90f, 90f)

        // Teleport to new position
        entity.teleport(newPosition)
        logDebug("[NPC] moveTowards: Teleported to ${newPosition.blockX},${newPosition.blockY},${newPosition.blockZ}, yaw=$yaw, pitch=$pitch")
    }

    /**
     * Finds the ground level (top solid block) at the given X, Z coordinates.
     * Returns null if no solid ground is found within reasonable range.
     */
    private fun findGroundLevel(world: World, x: Int, z: Int, startY: Int): Double? {
        // Search from startY + 2 down to startY - 10
        for (y in (startY + 2).downTo(startY - 10)) {
            val block = world.getBlockAt(x, y, z)
            if (block.type.isSolid && block.type != Material.BARRIER) {
                // Found solid ground, return the top of this block
                return (y + 1).toDouble()
            }
        }
        return null
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
        val npcName = entity.customName ?: entity.type.name
        val aiBefore = entity.hasAI()
        entity.isImmovable = immovable
        // Ensure AI is enabled when immovable is set to false
        if (!immovable && !entity.hasAI()) {
            logDebug("[NPC] setImmovable: NPC '$npcName' immovable set to false but AI was disabled, enabling AI")
            entity.setAI(true)
        }
        val aiAfter = entity.hasAI()
        logDebug("[NPC] setImmovable: NPC '$npcName' immovable=$immovable, AI was $aiBefore, now $aiAfter")
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
        val entity = getMannequin()
        if (entity == null) {
            logDebug("[NPC] setLookAtPlayers: ERROR - Cannot get mannequin entity!")
            return
        }
        
        val npcName = entity.customName ?: entity.type.name
        val npcId = entity.uniqueId
        val aiBefore = entity.hasAI()
        
        logDebug("[NPC] setLookAtPlayers: Setting lookAtPlayers=$enabled for NPC '$npcName' ($npcId)")
        logDebug("[NPC] setLookAtPlayers: Current lookAtPlayers state: $lookAtPlayers, AI enabled: $aiBefore")
        
        // Ensure AI is enabled for look-at functionality
        if (!entity.hasAI()) {
            logDebug("[NPC] setLookAtPlayers: AI was disabled, enabling it for NPC '$npcName'")
            entity.setAI(true)
        }
        
        lookAtPlayers = enabled
        if (enabled) {
            logDebug("[NPC] setLookAtPlayers: Registering NPC '$npcName' for proximity monitoring")
            NPCEventListener.registerProximityNPC(this)
            logDebug("[NPC] setLookAtPlayers: NPC '$npcName' registered for proximity monitoring")
        } else {
            // Only unregister if no proximity event handlers are registered
            val hasProximityHandlers = eventHandlers.containsKey(NPCEventType.PLAYER_SNEAKING_NEARBY) ||
                    eventHandlers.containsKey(NPCEventType.PLAYER_PUNCHING_NEARBY)
            logDebug("[NPC] setLookAtPlayers: hasProximityHandlers=$hasProximityHandlers")
            if (!hasProximityHandlers) {
                logDebug("[NPC] setLookAtPlayers: Unregistering NPC '$npcName' from proximity monitoring")
                NPCEventListener.unregisterProximityNPC(this)
            } else {
                logDebug("[NPC] setLookAtPlayers: Keeping NPC '$npcName' registered due to proximity event handlers")
            }
        }
        
        val aiAfter = entity.hasAI()
        logDebug("[NPC] setLookAtPlayers: Final lookAtPlayers state: $lookAtPlayers, AI enabled: $aiAfter")
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

    override fun setAI(enabled: Boolean) {
        val entity = getMannequin() ?: return
        val npcName = entity.customName ?: entity.type.name
        val aiBefore = entity.hasAI()
        entity.setAI(enabled)
        val aiAfter = entity.hasAI()
        logDebug("[NPC] setAI: NPC '$npcName' AI was $aiBefore, now $aiAfter")
    }

    override fun hasAI(): Boolean {
        val entity = getMannequin() ?: return false
        return entity.hasAI()
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