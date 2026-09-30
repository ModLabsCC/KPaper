package cc.modlabs.kpaper.npc.managed

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Jukebox
import org.bukkit.util.Vector
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal class NpcDanceRuntime(
    private val hooks: NpcHooks,
    private val renderer: PacketNpcRenderer,
) {
    fun idleAnimations(npc: NpcDefinition, state: NpcMovementState, ticks: Long, dancing: Boolean) {
        if (dancing || state.path.isNotEmpty()) return
        if (NpcAnimation.ARM_SWING in npc.animations) {
            if (state.nextIdleSwing == 0L) state.nextIdleSwing = ticks + Random.nextInt(40, 121)
            if (ticks >= state.nextIdleSwing) {
                renderer.swing(npc.id, offHand = Random.nextBoolean())
                state.nextIdleSwing = ticks + Random.nextInt(80, 201)
            }
        }
        if (NpcAnimation.IDLE_LOOK in npc.animations) {
            if (state.nextIdleLook == 0L) state.nextIdleLook = ticks + Random.nextInt(20, 81)
            if (ticks >= state.nextIdleLook) {
                renderer.headLook(npc.id, state.position.yaw + Random.nextInt(-50, 51))
                state.nextIdleLook = ticks + Random.nextInt(40, 101)
            }
        }
    }

    fun nearbyMusicIsPlaying(state: NpcMovementState, radius: Double, ticks: Long): Boolean {
        if (ticks < state.nextMusicCheck) return state.musicPlaying
        state.nextMusicCheck = ticks + 10
        val world = state.position.world ?: return false
        val chunkRadius = ceil(radius).toInt()
        val radiusSquared = radius * radius
        val chunkXs = ((state.position.blockX - chunkRadius) shr 4)..((state.position.blockX + chunkRadius) shr 4)
        val chunkZs = ((state.position.blockZ - chunkRadius) shr 4)..((state.position.blockZ + chunkRadius) shr 4)
        state.musicPlaying = hooks.musicPlaying(state.position, radius) || chunkXs.any { chunkX ->
            chunkZs.any { chunkZ ->
                world.isChunkLoaded(chunkX, chunkZ) && world.getChunkAt(chunkX, chunkZ)
                    .getTileEntities({ it.type == Material.JUKEBOX }, false)
                    .filterIsInstance<Jukebox>()
                    .any { it.isPlaying && it.location.distanceSquared(state.position) <= radiusSquared }
            }
        }
        return state.musicPlaying
    }

    fun isWatched(state: NpcMovementState): Boolean {
        val world = state.position.world ?: return false
        val target = state.position.toVector().add(Vector(0.0, 1.0, 0.0))
        return world.players.any { player ->
            if (player.isDead || player.gameMode == GameMode.SPECTATOR) return@any false
            val eye = player.eyeLocation
            isNpcInsideViewCone(eye.toVector(), eye.direction, target)
        }
    }

    fun dance(npc: NpcDefinition, state: NpcMovementState, atHome: Boolean) {
        if (!state.dancing) state.danceOrigin = (if (atHome) npc.location else state.position).clone()
        state.dancing = true
        if (state.danceMove == null || state.danceMoveTick >= state.danceMoveDuration) selectNextMove(state)
        val frame = npcDanceFrame(state.danceMove!!, state.danceMoveTick, state.danceMoveDuration)
        val pose = if (frame.sneaking) NpcPose.CROUCHING else NpcPose.STANDING
        if (state.dancePose != pose) {
            state.dancePose = pose
            renderer.pose(npc.id, pose)
        }
        val origin = state.danceOrigin!!
        val yawRadians = Math.toRadians(origin.yaw.toDouble())
        state.position = origin.clone().add(
            cos(yawRadians) * frame.sideOffset,
            frame.height,
            sin(yawRadians) * frame.sideOffset,
        ).apply { yaw += frame.yawOffset }
        renderer.move(npc.id, state.position, moving = true)
        if (state.danceMoveTick % state.danceSwingInterval == 0) renderer.swing(npc.id, Random.nextBoolean())
        state.danceMoveTick++
    }

    fun stopDancing(npc: NpcDefinition, state: NpcMovementState) {
        state.dancing = false
        state.danceMove = null
        state.danceMoveTick = 0
        state.position = state.danceOrigin ?: npc.location.clone()
        state.danceOrigin = null
        state.dancePose = null
        renderer.pose(npc.id, npc.pose)
        renderer.move(npc.id, state.position, moving = false)
    }

    private fun selectNextMove(state: NpcMovementState) {
        val moves = NpcDanceMove.entries.filterNot { it == state.danceMove }
        state.danceMove = moves.random()
        state.danceMoveTick = 0
        state.danceMoveDuration = when (state.danceMove) {
            NpcDanceMove.JUMP -> Random.nextInt(14, 25)
            NpcDanceMove.SNEAK -> Random.nextInt(16, 33)
            NpcDanceMove.SPIN -> Random.nextInt(24, 49)
            NpcDanceMove.PUNCH -> Random.nextInt(12, 29)
            null -> error("Dance move missing")
        }
        state.danceSwingInterval = Random.nextInt(5, 12)
    }
}
