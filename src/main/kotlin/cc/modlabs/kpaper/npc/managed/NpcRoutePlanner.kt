package cc.modlabs.kpaper.npc.managed

import org.bukkit.Location
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal class NpcRoutePlanner(private val hooks: NpcHooks) {
    fun plan(npc: NpcDefinition, state: NpcMovementState, ticks: Long): Boolean {
        val destination = nextDestination(npc, state) ?: run {
            state.waitUntil = ticks + 20
            return true
        }
        val roamingRegion = activeRoamingRegion(npc.movement)
        val allowed = roamingRegion?.let { regionId ->
            { location: Location -> hooks.regionContains(regionId, location) }
        }
        val route = findNpcWalkingPath(state.position, destination, npc.movement.stepMode, allowed)
        if (route.isEmpty()) {
            state.waitUntil = ticks + 40
            return true
        }
        state.path.addAll(route)
        return true
    }

    private fun nextDestination(npc: NpcDefinition, state: NpcMovementState): Location? {
        val settings = npc.movement
        return when (settings.mode) {
            NpcMovementMode.OFF -> null
            NpcMovementMode.ROAMING -> {
                val region = activeRoamingRegion(settings)
                val origin = if (region == null) npc.location else state.position
                randomRoamingDestination(origin, settings.roamingRadius) { location ->
                    region == null || hooks.regionContains(region, location)
                }
            }
            NpcMovementMode.RANDOM_RETURN -> if (state.returningHome) npc.location.clone()
            else selectWeightedWaypoint(settings.waypoints, state.lastWaypoint)
                ?.also { state.lastWaypoint = it.name }?.location?.clone()
            NpcMovementMode.RANDOM -> selectWeightedWaypoint(settings.waypoints, state.lastWaypoint)
                ?.also { state.lastWaypoint = it.name }?.location?.clone()
            NpcMovementMode.PATH_CIRCLE, NpcMovementMode.PATH_PINGPONG -> settings.waypoints
                .takeIf { it.isNotEmpty() }
                ?.get(state.pathIndex.coerceIn(settings.waypoints.indices))
                ?.location
                ?.clone()
            NpcMovementMode.DANCING -> null
        }
    }

    private fun randomRoamingDestination(
        home: Location,
        radius: Double,
        allowed: (Location) -> Boolean,
    ): Location? {
        repeat(32) {
            val angle = Random.nextDouble(0.0, Math.PI * 2.0)
            val distance = sqrt(Random.nextDouble()) * radius
            val candidate = home.clone().add(cos(angle) * distance, 0.0, sin(angle) * distance)
            nearestWalkableNode(candidate)?.let { node ->
                val destination = nodeLocation(home.world, node).apply {
                    yaw = home.yaw
                    pitch = 0f
                }
                if (allowed(destination)) return destination
            }
        }
        return null
    }
}
