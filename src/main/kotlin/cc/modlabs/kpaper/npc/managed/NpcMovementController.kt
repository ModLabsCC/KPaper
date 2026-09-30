package cc.modlabs.kpaper.npc.managed

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Location
import org.bukkit.scheduler.BukkitTask
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

internal class NpcMovementController(
    private val plugin: JavaPlugin,
    private val renderer: PacketNpcRenderer,
    private val definition: (String) -> NpcDefinition?,
    hooks: NpcHooks,
) {
    private val states = linkedMapOf<String, NpcMovementState>()
    private val dance = NpcDanceRuntime(hooks, renderer)
    private val routes = NpcRoutePlanner(hooks)
    private var task: BukkitTask? = null
    private var ticks = 0L

    fun start(definitions: Collection<NpcDefinition>) {
        stop()
        states.clear()
        ticks = 0
        definitions.forEach { npc ->
            states[npc.id] = NpcMovementState(npc.location.clone(), waitUntil = staggeredStart(npc.id))
        }
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable(::tick), 1L, 1L)
    }

    fun stop() {
        task?.cancel()
        task = null
        states.clear()
    }

    fun currentLocation(id: String): Location? = states[id.lowercase()]?.position?.clone()

    fun update(npc: NpcDefinition, resetPosition: Boolean = false) {
        val state = states.getOrPut(npc.id) { NpcMovementState(npc.location.clone()) }
        if (resetPosition || state.position.world != npc.location.world) {
            state.position = npc.location.clone()
            state.dancing = false
            state.danceOrigin = null
            state.dancePose = null
            renderer.move(npc.id, state.position, moving = false)
        }
        resetRoute(state, staggeredStart(npc.id))
    }

    fun remove(id: String) {
        states.remove(id.lowercase())
    }

    private fun tick() {
        ticks++
        var routePlanned = false
        states.forEach { (id, state) ->
            val npc = definition(id)?.takeIf { it.location.world != null } ?: return@forEach
            val settings = npc.movement
            val danceToMusic = settings.danceToMusicRadius?.let { dance.nearbyMusicIsPlaying(state, it, ticks) } == true
            dance.idleAnimations(npc, state, ticks, settings.mode == NpcMovementMode.DANCING || danceToMusic)
            if (settings.mode == NpcMovementMode.OFF && !danceToMusic) {
                if (state.dancing) dance.stopDancing(npc, state)
                return@forEach
            }
            if (settings.onlyWhenUnwatched && dance.isWatched(state)) return@forEach
            if (settings.mode == NpcMovementMode.DANCING || danceToMusic) {
                dance.dance(npc, state, settings.mode == NpcMovementMode.DANCING)
                return@forEach
            }
            if (state.dancing) dance.stopDancing(npc, state)
            if (state.path.isEmpty()) {
                if (ticks < state.waitUntil || routePlanned) return@forEach
                // ponytail: one bounded route per tick; add a planning queue if this becomes limiting.
                routePlanned = routes.plan(npc, state, ticks)
                if (state.path.isEmpty()) return@forEach
            }
            moveOneTick(npc, state)
        }
    }

    private fun moveOneTick(npc: NpcDefinition, state: NpcMovementState) {
        var remaining = npc.movement.speed.coerceIn(0.2, 8.0) / 20.0
        while (remaining > 0.0001 && state.path.isNotEmpty()) {
            val next = state.path.first()
            val dx = next.x - state.position.x
            val dy = next.y - state.position.y
            val dz = next.z - state.position.z
            val distance = sqrt(dx * dx + dy * dy + dz * dz)
            if (distance <= remaining) {
                state.position.x = next.x
                state.position.y = next.y
                state.position.z = next.z
                state.path.removeFirst()
                remaining -= distance
            } else {
                state.position.add(dx / distance * remaining, dy / distance * remaining, dz / distance * remaining)
                remaining = 0.0
            }
            if (hypot(dx, dz) > 0.0001) {
                state.position.yaw = Math.toDegrees(-atan2(dx, dz)).toFloat()
                state.position.pitch = 0f
            }
        }
        val moving = state.path.isNotEmpty()
        renderer.move(npc.id, state.position, moving)
        if (!moving) arrived(npc, state)
    }

    private fun arrived(npc: NpcDefinition, state: NpcMovementState) {
        when (npc.movement.mode) {
            NpcMovementMode.RANDOM_RETURN -> state.returningHome = !state.returningHome
            NpcMovementMode.PATH_CIRCLE, NpcMovementMode.PATH_PINGPONG -> {
                val step = nextNpcPathStep(
                    state.pathIndex,
                    state.pathDirection,
                    npc.movement.waypoints.size,
                    npc.movement.mode == NpcMovementMode.PATH_CIRCLE,
                )
                state.pathIndex = step.index
                state.pathDirection = step.direction
            }
            else -> Unit
        }
        state.waitUntil = ticks + npc.movement.waitTicks.coerceIn(0, 6000)
    }

    private fun resetRoute(state: NpcMovementState, waitUntil: Long) {
        state.path.clear()
        state.waitUntil = waitUntil
        state.pathIndex = 0
        state.pathDirection = 1
        state.returningHome = false
        state.lastWaypoint = null
        state.danceMove = null
        state.danceMoveTick = 0
        state.nextMusicCheck = 0
    }

    private fun staggeredStart(id: String) = ticks + 10L + abs(id.hashCode().toLong()) % 80L
}
