package cc.modlabs.kpaper.npc.managed

import org.bukkit.util.Vector
import kotlin.math.sin
import kotlin.random.Random

internal fun isNpcInsideViewCone(eye: Vector, direction: Vector, npc: Vector): Boolean {
    val toNpc = npc.clone().subtract(eye)
    if (toNpc.lengthSquared() !in 0.0001..(64.0 * 64.0) || direction.lengthSquared() < 0.0001) return false
    return toNpc.normalize().dot(direction.clone().normalize()) >= 0.0
}

internal data class NpcDanceFrame(
    val height: Double,
    val yawOffset: Float,
    val sneaking: Boolean,
    val sideOffset: Double,
)

internal enum class NpcDanceMove { JUMP, SNEAK, SPIN, PUNCH }

internal fun npcDanceFrame(move: NpcDanceMove, tick: Int, duration: Int): NpcDanceFrame {
    val progress = tick.coerceIn(0, duration - 1) / (duration - 1).coerceAtLeast(1).toDouble()
    val sideOffset = sin(progress * Math.PI * 2.0) * 0.5
    return when (move) {
        NpcDanceMove.JUMP -> NpcDanceFrame(sin(progress * Math.PI) * 0.45, 0f, false, sideOffset)
        NpcDanceMove.SNEAK -> NpcDanceFrame(0.0, 0f, tick / 4 % 2 == 0, sideOffset)
        NpcDanceMove.SPIN -> NpcDanceFrame(0.0, (progress * 360.0).toFloat(), false, sideOffset)
        NpcDanceMove.PUNCH -> NpcDanceFrame(0.0, 0f, false, sideOffset)
    }
}

internal fun activeRoamingRegion(settings: NpcMovementSettings) =
    settings.roamingRegionId?.takeIf { settings.mode == NpcMovementMode.ROAMING }

internal fun selectWeightedWaypoint(
    waypoints: List<NpcWaypoint>,
    excludedName: String? = null,
    random: Random = Random.Default,
): NpcWaypoint? {
    val candidates = waypoints.filterNot { waypoints.size > 1 && it.name == excludedName }
    if (candidates.isEmpty()) return null
    val total = candidates.sumOf { it.weight.coerceAtLeast(1) }
    var pick = random.nextInt(total)
    return candidates.first { waypoint ->
        pick -= waypoint.weight.coerceAtLeast(1)
        pick < 0
    }
}

internal data class NpcPathStep(val index: Int, val direction: Int)

internal fun nextNpcPathStep(index: Int, direction: Int, size: Int, circle: Boolean): NpcPathStep {
    if (size <= 1) return NpcPathStep(0, 1)
    if (circle) return NpcPathStep((index + 1) % size, 1)
    val next = index + direction
    return when {
        next >= size -> NpcPathStep(size - 2, -1)
        next < 0 -> NpcPathStep(1, 1)
        else -> NpcPathStep(next, direction)
    }
}
