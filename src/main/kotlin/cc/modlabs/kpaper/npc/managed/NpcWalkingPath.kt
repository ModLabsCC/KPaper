package cc.modlabs.kpaper.npc.managed

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import java.util.ArrayDeque
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

internal data class WalkNode(val x: Int, val y: Int, val z: Int)
private data class OpenNode(val node: WalkNode, val score: Double)

internal fun findNpcWalkingPath(
    start: Location,
    target: Location,
    stepMode: NpcStepMode,
    allowed: ((Location) -> Boolean)? = null,
): List<Location> {
    if (start.world != target.world) return emptyList()
    val world = start.world
    val startNode = nearestWalkableNode(start) ?: return emptyList()
    val goalNode = nearestWalkableNode(target) ?: return emptyList()
    if (allowed != null && (!allowed(nodeLocation(world, startNode)) || !allowed(nodeLocation(world, goalNode)))) {
        return emptyList()
    }
    if (startNode == goalNode) {
        val ground = world.getBlockAt(goalNode.x, goalNode.y - 1, goalNode.z).type
        return if (stepMode.allowsAscent(target.y - start.y, ground)) listOf(target.clone()) else emptyList()
    }
    val open = PriorityQueue(compareBy(OpenNode::score))
    val cameFrom = hashMapOf<WalkNode, WalkNode>()
    val costs = hashMapOf(startNode to 0.0)
    open += OpenNode(startNode, walkHeuristic(startNode, goalNode))
    var visited = 0
    while (open.isNotEmpty() && visited++ < 3072) {
        val current = open.remove().node
        if (current == goalNode) return buildPath(world, startNode, current, cameFrom, target)
        walkNeighbors(world, current, stepMode).forEach { neighbor ->
            if (allowed != null && !isAllowedPathStep(world, current, neighbor, allowed)) return@forEach
            val diagonal = neighbor.x != current.x && neighbor.z != current.z
            val cost = costs.getValue(current) + if (diagonal) 1.414 else 1.0 + abs(neighbor.y - current.y) * 0.35
            if (cost >= (costs[neighbor] ?: Double.POSITIVE_INFINITY)) return@forEach
            costs[neighbor] = cost
            cameFrom[neighbor] = current
            open += OpenNode(neighbor, cost + walkHeuristic(neighbor, goalNode))
        }
    }
    return emptyList()
}

private fun buildPath(
    world: World,
    start: WalkNode,
    end: WalkNode,
    cameFrom: Map<WalkNode, WalkNode>,
    target: Location,
): List<Location> {
    val nodes = ArrayDeque<WalkNode>()
    var cursor = end
    while (cursor != start) {
        nodes.addFirst(cursor)
        cursor = cameFrom[cursor] ?: break
    }
    return nodes.map { nodeLocation(world, it) }.toMutableList().apply {
        if (isNotEmpty()) this[lastIndex] = target.clone()
    }
}

private fun isAllowedPathStep(
    world: World,
    from: WalkNode,
    to: WalkNode,
    allowed: (Location) -> Boolean,
): Boolean {
    val start = nodeLocation(world, from)
    val end = nodeLocation(world, to)
    return (1..4).all { part ->
        val progress = part / 4.0
        allowed(Location(
            world,
            start.x + (end.x - start.x) * progress,
            start.y + (end.y - start.y) * progress,
            start.z + (end.z - start.z) * progress,
        ))
    }
}

internal fun nearestWalkableNode(location: Location): WalkNode? {
    val baseY = floor(location.y + 0.01).toInt()
    val x = floor(location.x).toInt()
    val z = floor(location.z).toInt()
    return sequenceOf(0, 1, -1, 2, -2).map { WalkNode(x, baseY + it, z) }
        .firstOrNull { isWalkable(location.world, it) }
}

private fun walkNeighbors(world: World, node: WalkNode, stepMode: NpcStepMode): List<WalkNode> {
    val result = ArrayList<WalkNode>(8)
    val currentSurfaceY = walkSurfaceY(world, node)
    for (dx in -1..1) for (dz in -1..1) {
        if (dx == 0 && dz == 0) continue
        val candidate = sequenceOf(0, 1, -1)
            .map { WalkNode(node.x + dx, node.y + it, node.z + dz) }
            .firstOrNull {
                isWalkable(world, it) && stepMode.allowsAscent(
                    walkSurfaceY(world, it) - currentSurfaceY,
                    world.getBlockAt(it.x, it.y - 1, it.z).type,
                )
            } ?: continue
        if (dx != 0 && dz != 0) {
            if (!isWalkable(world, WalkNode(node.x + dx, candidate.y, node.z))) continue
            if (!isWalkable(world, WalkNode(node.x, candidate.y, node.z + dz))) continue
        }
        result += candidate
    }
    return result
}

private fun isWalkable(world: World, node: WalkNode): Boolean {
    if (!world.isChunkLoaded(node.x shr 4, node.z shr 4)) return false
    val feet = world.getBlockAt(node.x, node.y, node.z)
    val head = world.getBlockAt(node.x, node.y + 1, node.z)
    val ground = world.getBlockAt(node.x, node.y - 1, node.z)
    return feet.isPassable && head.isPassable && ground.type.isSolid && !feet.isLiquid && !head.isLiquid
}

internal fun nodeLocation(world: World, node: WalkNode) =
    Location(world, node.x + 0.5, walkSurfaceY(world, node), node.z + 0.5)

private fun walkSurfaceY(world: World, node: WalkNode): Double {
    val ground = world.getBlockAt(node.x, node.y - 1, node.z)
    val top = ground.collisionShape.boundingBoxes.maxOfOrNull { it.maxY } ?: 1.0
    return ground.y + top
}

internal fun NpcStepMode.allowsAscent(rise: Double, destinationGround: Material): Boolean {
    if (rise <= 0.01) return true
    return when (this) {
        NpcStepMode.NONE -> false
        NpcStepMode.STAIRS_SLABS -> rise <= 0.51 ||
            destinationGround.name.endsWith("_STAIRS") || destinationGround.name.endsWith("_SLAB")
        NpcStepMode.FULL_BLOCKS -> rise <= 1.01
    }
}

private fun walkHeuristic(a: WalkNode, b: WalkNode): Double {
    val dx = (a.x - b.x).toDouble()
    val dy = (a.y - b.y).toDouble()
    val dz = (a.z - b.z).toDouble()
    return sqrt(dx * dx + dy * dy + dz * dz)
}
