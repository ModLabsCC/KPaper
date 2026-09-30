package cc.modlabs.kpaper.npc.managed

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

internal class NpcVisibilityRuntime(
    private val hooks: NpcHooks,
    private val rendered: Map<String, PacketNpc>,
    private val approach: (Player, String) -> Unit,
    private val packets: NpcPacketSender,
) {
    private val visible = hashMapOf<UUID, MutableSet<String>>()
    private val rotations = hashMapOf<UUID, MutableMap<String, NpcRotation>>()
    private val pitchOffsets = hashMapOf<String, Float>()
    private val nearby = hashMapOf<UUID, MutableSet<String>>()

    fun clear() {
        Bukkit.getOnlinePlayers().forEach { player ->
            visible[player.uniqueId].orEmpty().forEach { id -> rendered[id]?.let { packets.remove(player, it) } }
        }
        visible.clear()
        rotations.clear()
        pitchOffsets.clear()
        nearby.clear()
    }

    fun remove(id: String, npc: PacketNpc) {
        pitchOffsets.remove(id)
        Bukkit.getOnlinePlayers().forEach { player ->
            if (visible[player.uniqueId]?.remove(id) == true) packets.remove(player, npc)
            rotations[player.uniqueId]?.remove(id)
            nearby[player.uniqueId]?.remove(id)
        }
    }

    fun move(id: String, npc: PacketNpc, location: Location) {
        if (location.world.uid != npc.worldId) return
        npc.x = location.x
        npc.y = location.y
        npc.z = location.z
        npc.yaw = location.yaw
        npc.pitch = location.pitch
        npc.seat?.let { seat ->
            val position = npcSeatPosition(location, hooks.seatLocation(location))
            seat.setPos(position.x, position.y, position.z)
            seat.setRot(position.yaw, 0f)
            npc.yaw = position.yaw
        }
        Bukkit.getOnlinePlayers().forEach { player ->
            if (visible[player.uniqueId]?.contains(id) != true) return@forEach
            val playerLocation = player.location
            val dx = playerLocation.x - npc.x
            val dy = playerLocation.y - npc.y
            val dz = playerLocation.z - npc.z
            val close = player.world.uid == npc.worldId && dx * dx + dy * dy + dz * dz <= npc.lookCloseRangeSquared
            val rotation = if (npc.lookClose && close) {
                val eye = player.eyeLocation
                npcLookRotation(npc.x, npc.y + 1.62, npc.z, eye.x, eye.y, eye.z)
            } else NpcRotation(npc.yaw, npc.pitch)
            packets.teleport(player, npc, npcTalkingRotation(rotation, pitchOffsets[id] ?: 0f))
            rotations[player.uniqueId]?.set(id, rotation)
        }
    }

    fun location(id: String): Location? = rendered[id.lowercase()]?.let { npc ->
        Bukkit.getWorld(npc.worldId)?.let { Location(it, npc.x, npc.y, npc.z, npc.yaw, npc.pitch) }
    }

    fun swing(id: String, offHand: Boolean) {
        val npc = rendered[id] ?: return
        forViewers(id) { packets.swing(it, npc, offHand) }
    }

    fun headLook(id: String, yaw: Float) {
        val npc = rendered[id] ?: return
        forViewers(id) { packets.headLook(it, npc, yaw) }
    }

    fun headPitch(id: String, offset: Float) {
        val npc = rendered[id] ?: return
        if (offset == 0f) pitchOffsets.remove(id) else pitchOffsets[id] = offset
        forViewers(id) { player ->
            packets.rotation(player, npc, rotations[player.uniqueId]?.get(id) ?: NpcRotation(npc.yaw, npc.pitch), offset)
        }
    }

    fun pose(id: String, pose: NpcPose) {
        val npc = rendered[id] ?: return
        npc.pose = pose
        forViewers(id) { packets.sendMetadata(it, npc); packets.moveLabel(it, npc) }
    }

    fun sync() {
        // A furniture seat may become available after its chunk loads.
        rendered.forEach { (id, npc) ->
            val seat = npc.seat ?: return@forEach
            val authored = location(id) ?: return@forEach
            if (!authored.world.isChunkLoaded(authored.blockX shr 4, authored.blockZ shr 4)) return@forEach
            val position = npcSeatPosition(authored, hooks.seatLocation(authored))
            if (needsNpcSeatRefresh(position, seat.x, seat.y, seat.z, seat.yRot)) {
                move(id, npc, authored)
            }
        }
        // ponytail: O(players * NPCs) culling; use a chunk index when profiling warrants it.
        Bukkit.getOnlinePlayers().forEach { player ->
            val playerVisible = visible.getOrPut(player.uniqueId, ::hashSetOf)
            val playerRotations = rotations.getOrPut(player.uniqueId, ::hashMapOf)
            val playerNearby = nearby.getOrPut(player.uniqueId, ::hashSetOf)
            val location = player.location
            rendered.forEach { (id, npc) ->
                val dx = location.x - npc.x
                val dy = location.y - npc.y
                val dz = location.z - npc.z
                val distanceSquared = dx * dx + dy * dy + dz * dz
                val shouldSee = shouldRenderNpc(player.world.uid, npc.worldId, distanceSquared) && hooks.canSee(player, id)
                if (shouldSee && playerVisible.add(id)) {
                    packets.spawn(player, npc)
                    playerRotations[id] = NpcRotation(npc.yaw, npc.pitch)
                }
                if (shouldSee && npc.lookClose) syncLookClose(player, id, npc, distanceSquared, playerRotations)
                val isNear = shouldSee && npc.hasProximityDialog && distanceSquared <= npc.dialogRangeSquared
                if (isNear && playerNearby.add(id)) approach(player, id)
                if (!isNear) playerNearby.remove(id)
                if (!shouldSee && playerVisible.remove(id)) {
                    packets.remove(player, npc)
                    playerRotations.remove(id)
                }
            }
        }
        visible.keys.removeIf { Bukkit.getPlayer(it) == null }
        rotations.keys.removeIf { Bukkit.getPlayer(it) == null }
        nearby.keys.removeIf { Bukkit.getPlayer(it) == null }
    }

    private fun syncLookClose(
        player: Player,
        id: String,
        npc: PacketNpc,
        distanceSquared: Double,
        playerRotations: MutableMap<String, NpcRotation>,
    ) {
        val rotation = if (distanceSquared <= npc.lookCloseRangeSquared) {
            val eye = player.eyeLocation
            npcLookRotation(npc.x, npc.y + 1.62, npc.z, eye.x, eye.y, eye.z)
        } else NpcRotation(npc.yaw, npc.pitch)
        if (samePacketRotation(playerRotations[id], rotation)) return
        playerRotations[id] = rotation
        packets.rotation(player, npc, rotation, pitchOffsets[id] ?: 0f)
    }

    fun reset(player: Player) {
        visible.remove(player.uniqueId)?.forEach { id -> rendered[id]?.let { packets.remove(player, it) } }
        rotations.remove(player.uniqueId)
        nearby.remove(player.uniqueId)
    }

    fun isVisible(player: Player, id: String) = visible[player.uniqueId]?.contains(id) == true

    private inline fun forViewers(id: String, action: (Player) -> Unit) = Bukkit.getOnlinePlayers().forEach {
        if (visible[it.uniqueId]?.contains(id) == true) action(it)
    }
}
