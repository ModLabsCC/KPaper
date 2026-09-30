package cc.modlabs.kpaper.npc.managed

import org.bukkit.plugin.java.JavaPlugin
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers
import com.github.retrooper.packetevents.util.Vector3d
import com.mojang.datafixers.util.Pair
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.player.Player as NmsPlayer
import net.minecraft.world.item.component.SwingAnimation
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import java.util.EnumSet
import java.util.UUID
import org.bukkit.scheduler.BukkitTask

internal class NpcPacketSender(private val plugin: JavaPlugin, private val hooks: NpcHooks) {
    private val profileRemovals = mutableMapOf<kotlin.Pair<UUID, Int>, BukkitTask>()
    fun spawn(player: Player, npc: PacketNpc) {
        val connection = (player as CraftPlayer).handle.connection
        val entry = ClientboundPlayerInfoUpdatePacket.Entry(
            npc.uuid, npc.profile, false, 0, GameType.SURVIVAL, null, true, 0, null,
        )
        connection.send(ClientboundPlayerInfoUpdatePacket(PLAYER_INFO_ACTIONS, entry))
        if (!npc.nameVisible) connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(hiddenNpcNameTeam(npc), true))
        npc.seat?.let { seat ->
            connection.send(
                ClientboundAddEntityPacket(
                    seat.id, seat.uuid, seat.x, seat.y, seat.z, 0f, seat.yRot,
                    EntityTypes.INTERACTION, 0, Vec3.ZERO, npc.yaw.toDouble(),
                ),
            )
            connection.send(ClientboundSetEntityDataPacket(seat.id, seat.entityData.packAll()))
        }
        npc.hologram?.let { display ->
            connection.send(ClientboundAddEntityPacket(display.id, display.uuid, display.x, display.y, display.z,
                0f, 0f, EntityTypes.TEXT_DISPLAY, 0, Vec3.ZERO, 0.0))
            connection.send(ClientboundSetEntityDataPacket(display.id, display.entityData.packAll()))
        }
        connection.send(
            ClientboundAddEntityPacket(
                npc.entityId, npc.uuid, npc.x, npc.y, npc.z, npc.pitch, npc.yaw,
                EntityTypes.PLAYER, 0, Vec3.ZERO, npc.yaw.toDouble(),
            ),
        )
        connection.send(
            ClientboundSetEntityDataPacket(
                npc.entityId,
                listOf(SynchedEntityData.DataValue.create(NmsPlayer.DATA_PLAYER_MODE_CUSTOMISATION, npc.skinLayers)),
            ),
        )
        sendMetadata(player, npc)
        npc.seat?.let {
            PacketEvents.getAPI().playerManager.sendPacket(player, WrapperPlayServerSetPassengers(it.id, intArrayOf(npc.entityId)))
        }
        val equipment = npc.equipment.map { (slot, item) -> Pair.of(slot.toNmsSlot(), CraftItemStack.asNMSCopy(item)) }
        if (equipment.isNotEmpty()) connection.send(ClientboundSetEquipmentPacket(npc.entityId, equipment))
        npcProfileRemovalDelay(hooks.isBedrock(player))?.let { delay ->
            val key = player.uniqueId to npc.entityId
            profileRemovals.remove(key)?.cancel()
            profileRemovals[key] = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                profileRemovals.remove(key)
                if (player.isOnline) player.handle.connection.send(ClientboundPlayerInfoRemovePacket(listOf(npc.uuid)))
            }, delay)
        }
    }

    fun remove(player: Player, npc: PacketNpc) {
        profileRemovals.remove(player.uniqueId to npc.entityId)?.cancel()
        val connection = (player as CraftPlayer).handle.connection
        connection.send(ClientboundRemoveEntitiesPacket(npc.entityId))
        npc.seat?.let { connection.send(ClientboundRemoveEntitiesPacket(it.id)) }
        npc.hologram?.let { connection.send(ClientboundRemoveEntitiesPacket(it.id)) }
        if (!npc.nameVisible) connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(hiddenNpcNameTeam(npc)))
        connection.send(ClientboundPlayerInfoRemovePacket(listOf(npc.uuid)))
    }

    fun teleport(player: Player, npc: PacketNpc, rotation: NpcRotation) {
        val manager = PacketEvents.getAPI().playerManager
        manager.sendPacket(
            player,
            WrapperPlayServerEntityTeleport(
                npc.entityId, Vector3d(npc.x, npc.y, npc.z), rotation.yaw, rotation.pitch, true,
            ),
        )
        npc.seat?.let {
            manager.sendPacket(
                player,
                WrapperPlayServerEntityTeleport(it.id, Vector3d(it.x, it.y, it.z), it.yRot, 0f, true),
            )
        }
        manager.sendPacket(player, WrapperPlayServerEntityHeadLook(npc.entityId, rotation.yaw))
        moveLabel(player, npc)
    }

    fun swing(player: Player, npc: PacketNpc, offHand: Boolean) {
        val hand = if (offHand) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        (player as CraftPlayer).handle.connection.send(ClientboundSwingAnimationPacket(npc.entityId, hand, SwingAnimation.DEFAULT))
    }

    fun headLook(player: Player, npc: PacketNpc, yaw: Float) =
        PacketEvents.getAPI().playerManager.sendPacket(player, WrapperPlayServerEntityHeadLook(npc.entityId, yaw))

    fun sendMetadata(player: Player, npc: PacketNpc) = PacketEvents.getAPI().playerManager.sendPacket(
        player,
        WrapperPlayServerEntityMetadata(
            npc.entityId,
            listOf(EntityData(6, EntityDataTypes.ENTITY_POSE, npcPacketPose(npc.pose))),
        ),
    )

    fun rotation(player: Player, npc: PacketNpc, rotation: NpcRotation, pitchOffset: Float) {
        val displayed = npcTalkingRotation(rotation, pitchOffset)
        val manager = PacketEvents.getAPI().playerManager
        manager.sendPacket(player, WrapperPlayServerEntityRotation(npc.entityId, displayed.yaw, displayed.pitch, true))
        manager.sendPacket(player, WrapperPlayServerEntityHeadLook(npc.entityId, displayed.yaw))
    }

    fun moveLabel(player: Player, npc: PacketNpc) {
        npc.hologram?.let { display ->
            display.setPos(npc.x, npc.y + npcLabelHeight(npc.pose), npc.z)
            PacketEvents.getAPI().playerManager.sendPacket(player,
                WrapperPlayServerEntityTeleport(display.id, Vector3d(display.x, display.y, display.z), 0f, 0f, true))
        }
    }

    fun clear() {
        profileRemovals.values.forEach(BukkitTask::cancel)
        profileRemovals.clear()
    }

    companion object {
        private val PLAYER_INFO_ACTIONS = EnumSet.of(
            ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT,
        )
    }
}
