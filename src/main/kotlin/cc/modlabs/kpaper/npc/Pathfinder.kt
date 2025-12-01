package cc.modlabs.kpaper.npc

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import kotlin.math.sqrt

/**
 * Pathfinding node for A* algorithm
 */
private data class PathNode(
    val x: Int,
    val y: Int,
    val z: Int,
    var g: Double = 0.0, // Cost from start
    var h: Double = 0.0, // Heuristic cost to goal
    var parent: PathNode? = null
) {
    fun f(): Double = g + h
    
    fun toLocation(world: World): Location {
        return Location(world, x + 0.5, y.toDouble(), z + 0.5)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PathNode) return false
        return x == other.x && y == other.y && z == other.z
    }
    
    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}

/**
 * Pathfinder utility for NPC navigation with obstacle avoidance and jumping.
 * Uses A* pathfinding algorithm to find optimal paths.
 */
object Pathfinder {
    
    // Maximum search distance to prevent infinite loops
    private const val MAX_SEARCH_DISTANCE = 50
    
    // Maximum height difference NPC can jump
    private const val MAX_JUMP_HEIGHT = 1
    
    // Maximum height difference NPC can drop
    private const val MAX_DROP_HEIGHT = 3
    
    // Maximum pathfinding iterations
    private const val MAX_ITERATIONS = 1000
    
    /**
     * Finds a path from start to target location using A* pathfinding.
     * Returns a list of waypoints that the NPC should follow.
     * 
     * @param start Starting location
     * @param target Target location
     * @return List of waypoint locations, or empty list if no path found
     */
    fun findPath(start: Location, target: Location): List<Location> {
        if (start.world != target.world) return emptyList()
        
        val world = start.world ?: return emptyList()
        
        // If start and target are very close, return direct path
        if (start.distance(target) < 2.0) {
            return listOf(target.clone())
        }
        
        val startNode = PathNode(
            start.blockX,
            start.blockY,
            start.blockZ
        )
        
        val targetNode = PathNode(
            target.blockX,
            target.blockY,
            target.blockZ
        )
        
        val openSet = mutableSetOf<PathNode>()
        val closedSet = mutableSetOf<PathNode>()
        val openMap = mutableMapOf<String, PathNode>()
        
        startNode.h = heuristic(startNode, targetNode)
        openSet.add(startNode)
        openMap[startNode.toString()] = startNode
        
        var iterations = 0
        
        while (openSet.isNotEmpty() && iterations < MAX_ITERATIONS) {
            iterations++
            
            // Get node with lowest f score
            val current = openSet.minByOrNull { it.f() } ?: break
            openSet.remove(current)
            openMap.remove(current.toString())
            closedSet.add(current)
            
            // Check if we reached the target
            if (current == targetNode || distance(current, targetNode) <= 1.5) {
                return reconstructPath(current, world)
            }
            
            // Check distance limit
            if (distance(startNode, current) > MAX_SEARCH_DISTANCE) {
                continue
            }
            
            // Explore neighbors
            val neighbors = getNeighbors(current, world, targetNode)
            for (neighbor in neighbors) {
                val neighborKey = neighbor.toString()
                
                if (closedSet.contains(neighbor)) continue
                
                val tentativeG = current.g + distance(current, neighbor)
                
                val existingNeighbor = openMap[neighborKey]
                if (existingNeighbor == null) {
                    neighbor.g = tentativeG
                    neighbor.h = heuristic(neighbor, targetNode)
                    neighbor.parent = current
                    openSet.add(neighbor)
                    openMap[neighborKey] = neighbor
                } else if (tentativeG < existingNeighbor.g) {
                    existingNeighbor.g = tentativeG
                    existingNeighbor.parent = current
                }
            }
        }
        
        // If no path found, return simplified path (direct line with waypoints for obstacles)
        return findSimplifiedPath(start, target, world)
    }
    
    /**
     * Gets valid neighboring nodes for pathfinding.
     */
    private fun getNeighbors(node: PathNode, world: World, target: PathNode): List<PathNode> {
        val neighbors = mutableListOf<PathNode>()
        
        // Check all 8 horizontal directions and vertical movements
        val directions = listOf(
            // Horizontal movements
            intArrayOf(1, 0, 0), intArrayOf(-1, 0, 0),
            intArrayOf(0, 0, 1), intArrayOf(0, 0, -1),
            intArrayOf(1, 0, 1), intArrayOf(1, 0, -1),
            intArrayOf(-1, 0, 1), intArrayOf(-1, 0, -1),
            // Up movements (jumping)
            intArrayOf(1, 1, 0), intArrayOf(-1, 1, 0),
            intArrayOf(0, 1, 1), intArrayOf(0, 1, -1),
            intArrayOf(1, 1, 1), intArrayOf(1, 1, -1),
            intArrayOf(-1, 1, 1), intArrayOf(-1, 1, -1),
            // Down movements (dropping)
            intArrayOf(1, -1, 0), intArrayOf(-1, -1, 0),
            intArrayOf(0, -1, 1), intArrayOf(0, -1, -1),
            intArrayOf(1, -1, 1), intArrayOf(1, -1, -1),
            intArrayOf(-1, -1, 1), intArrayOf(-1, -1, -1)
        )
        
        for (dir in directions) {
            val newX = node.x + dir[0]
            val newY = node.y + dir[1]
            val newZ = node.z + dir[2]
            
            if (isWalkable(world, newX, newY, newZ, node.y)) {
                neighbors.add(PathNode(newX, newY, newZ))
            }
        }
        
        return neighbors
    }
    
    /**
     * Checks if a position is walkable for the NPC.
     */
    private fun isWalkable(world: World, x: Int, y: Int, z: Int, fromY: Int): Boolean {
        // Check if the block at feet level is passable
        val feetBlock = world.getBlockAt(x, y, z)
        if (!isPassable(feetBlock)) return false
        
        // Check if the block at head level (y+1) is passable
        val headBlock = world.getBlockAt(x, y + 1, z)
        if (!isPassable(headBlock)) return false
        
        // Check if there's a solid block to stand on
        val groundBlock = world.getBlockAt(x, y - 1, z)
        if (!isSolid(groundBlock)) return false
        
        // Check height difference (can't jump too high or drop too far)
        val heightDiff = y - fromY
        if (heightDiff > MAX_JUMP_HEIGHT) return false
        if (heightDiff < -MAX_DROP_HEIGHT) return false
        
        return true
    }
    
    /**
     * Checks if a block is passable (air, water, etc.)
     */
    private fun isPassable(block: Block): Boolean {
        val type = block.type
        return type == Material.AIR ||
                type == Material.CAVE_AIR ||
                type == Material.VOID_AIR ||
                type == Material.WATER ||
                type == Material.LAVA ||
                !type.isSolid
    }
    
    /**
     * Checks if a block is solid (can stand on it)
     */
    private fun isSolid(block: Block): Boolean {
        val type = block.type
        return type.isSolid && type != Material.BARRIER
    }
    
    /**
     * Heuristic function for A* (Euclidean distance)
     */
    private fun heuristic(a: PathNode, b: PathNode): Double {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        val dz = (a.z - b.z).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Distance between two nodes
     */
    private fun distance(a: PathNode, b: PathNode): Double {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        val dz = (a.z - b.z).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Reconstructs the path from the target node back to start
     */
    private fun reconstructPath(node: PathNode, world: World): List<Location> {
        val path = mutableListOf<Location>()
        var current: PathNode? = node
        
        while (current != null) {
            path.add(0, current.toLocation(world))
            current = current.parent
        }
        
        // Simplify path by removing unnecessary waypoints
        return simplifyPath(path)
    }
    
    /**
     * Simplifies a path by removing waypoints that are in a straight line
     */
    private fun simplifyPath(path: List<Location>): List<Location> {
        if (path.size <= 2) return path
        
        val simplified = mutableListOf<Location>()
        simplified.add(path[0])
        
        var i = 0
        while (i < path.size - 1) {
            var j = path.size - 1
            while (j > i + 1) {
                if (hasLineOfSight(path[i], path[j])) {
                    simplified.add(path[j])
                    i = j
                    break
                }
                j--
            }
            if (j == i + 1) {
                simplified.add(path[i + 1])
                i++
            }
        }
        
        return simplified
    }
    
    /**
     * Checks if there's a clear line of sight between two locations
     */
    private fun hasLineOfSight(from: Location, to: Location): Boolean {
        if (from.world != to.world) return false
        
        val world = from.world ?: return false
        val direction = to.toVector().subtract(from.toVector())
        val distance = direction.length()
        val step = direction.normalize().multiply(0.5)
        
        var current = from.clone()
        var traveled = 0.0
        
        while (traveled < distance) {
            val block = current.block
            if (!isPassable(block) && !isPassable(world.getBlockAt(block.x, block.y + 1, block.z))) {
                return false
            }
            current.add(step)
            traveled += 0.5
        }
        
        return true
    }
    
    /**
     * Creates a simplified path when full pathfinding fails.
     * Uses raycasting to find obstacles and creates waypoints around them.
     */
    private fun findSimplifiedPath(start: Location, target: Location, world: World): List<Location> {
        val path = mutableListOf<Location>()
        path.add(start.clone())
        
        val direction = target.toVector().subtract(start.toVector())
        val distance = direction.length()
        val step = direction.normalize().multiply(1.0)
        
        var current = start.clone()
        var traveled = 0.0
        var lastWaypoint = start.clone()
        
        while (traveled < distance) {
            val nextPos = current.clone().add(step)
            val block = nextPos.block
            
            // Check if we hit an obstacle
            if (!isPassable(block) || !isPassable(world.getBlockAt(block.x, block.y + 1, block.z))) {
                // Try to find a way around
                val waypoint = findWaypointAroundObstacle(current, target, world)
                if (waypoint != null) {
                    path.add(waypoint)
                    lastWaypoint = waypoint
                    current = waypoint
                } else {
                    // Can't find way around, add intermediate point
                    path.add(current.clone())
                    break
                }
            } else {
                current = nextPos
            }
            
            traveled += 1.0
        }
        
        path.add(target.clone())
        return path
    }
    
    /**
     * Finds a waypoint to go around an obstacle
     */
    private fun findWaypointAroundObstacle(from: Location, target: Location, world: World): Location? {
        val direction = target.toVector().subtract(from.toVector()).normalize()
        val perpendicular = Vector(-direction.z, 0.0, direction.x).normalize()
        
        // Try going left and right around the obstacle
        for (offset in listOf(2.0, -2.0, 3.0, -3.0)) {
            val waypoint = from.clone().add(perpendicular.multiply(offset))
            if (isWalkable(world, waypoint.blockX, waypoint.blockY, waypoint.blockZ, from.blockY)) {
                return waypoint
            }
        }
        
        return null
    }
    
    /**
     * Checks if the NPC needs to jump at the current position.
     * Returns true if there's a block in front that requires jumping.
     */
    fun needsJump(entity: LivingEntity, target: Location): Boolean {
        val currentLoc = entity.location
        val world = currentLoc.world ?: return false
        
        // Check direction to target
        val direction = target.toVector().subtract(currentLoc.toVector()).normalize()
        val checkPos = currentLoc.clone().add(direction.multiply(0.6))
        
        val checkX = checkPos.blockX
        val checkY = currentLoc.blockY
        val checkZ = checkPos.blockZ
        
        // Check if there's a solid block in front at the current Y level (obstacle to jump over)
        val obstacleBlock = world.getBlockAt(checkX, checkY, checkZ)
        if (isPassable(obstacleBlock)) return false // No obstacle, no need to jump
        
        // Check if the space above the obstacle (at Y+1) is passable (can jump into it)
        val jumpSpace = world.getBlockAt(checkX, checkY + 1, checkZ)
        if (!isPassable(jumpSpace)) return false // Can't jump, space is blocked
        
        // Check if there's space for the head at Y+2
        val headSpace = world.getBlockAt(checkX, checkY + 2, checkZ)
        if (!isPassable(headSpace)) return false // Not enough space for head
        
        // Check if there's ground to land on at Y+1 level (the top of the obstacle)
        val landingGround = world.getBlockAt(checkX, checkY, checkZ)
        if (!isSolid(landingGround)) return false // No solid ground to land on
        
        // All conditions met - need to jump
        return true
    }
}

