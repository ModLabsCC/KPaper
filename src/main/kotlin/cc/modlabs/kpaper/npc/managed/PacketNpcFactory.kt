package cc.modlabs.kpaper.npc.managed

import org.bukkit.plugin.java.JavaPlugin
import cc.modlabs.kpaper.skins.SkinEntry
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.Interaction
import org.bukkit.craftbukkit.CraftWorld
import java.util.UUID

internal class PacketNpcFactory(private val plugin: JavaPlugin, private val hooks: NpcHooks) {
    fun create(definition: NpcDefinition, skin: SkinEntry?): PacketNpc {
        val profile = npcProfile(definition.id, if (definition.hologram == null) definition.name else definition.id.take(16), skin, plugin.name)
        val level = (definition.location.world as CraftWorld).handle
        val seatPosition = if (definition.pose == NpcPose.SITTING) {
            npcSeatPosition(definition.location, hooks.seatLocation(definition.location))
        } else null
        val seat = seatPosition?.let {
            Interaction(EntityTypes.INTERACTION, level).apply {
                id = level.nextEntityId
                uuid = UUID.randomUUID()
                setPos(it.x, it.y, it.z)
                setRot(it.yaw, 0f)
                setNoGravity(true)
                (bukkitEntity as org.bukkit.entity.Interaction).apply {
                    interactionWidth = 0.01f
                    interactionHeight = 0.01f
                    isResponsive = false
                }
            }
        }
        val hologram = definition.hologram?.takeIf { definition.nameVisible }?.let { label ->
            net.minecraft.world.entity.Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level).apply {
                id = level.nextEntityId
                uuid = UUID.randomUUID()
                val origin = seatPosition ?: definition.location
                setPos(origin.x, origin.y + hooks.hologramHeight(definition.pose), origin.z)
                (bukkitEntity as org.bukkit.entity.TextDisplay).apply {
                    text(npcDialogComponent(label))
                    billboard = org.bukkit.entity.Display.Billboard.CENTER
                    isShadowed = true
                    teleportDuration = 2
                    hooks.configureHologram(this)
                }
            }
        }
        return PacketNpc(
            level.nextEntityId,
            profile.id,
            profile,
            definition.location.world.uid,
            definition.location.x,
            definition.location.y,
            definition.location.z,
            seatPosition?.yaw ?: definition.location.yaw,
            definition.location.pitch,
            definition.lookClose,
            definition.lookCloseRange * definition.lookCloseRange,
            definition.equipment.mapValues { it.value.clone() },
            definition.dialogLines.isNotEmpty() && definition.dialogTrigger.onProximity ||
                definition.action != null && definition.actionTrigger.onProximity,
            definition.dialogRange * definition.dialogRange,
            definition.skinLayers,
            definition.pose,
            definition.nameVisible && definition.hologram == null,
            seat,
            hologram,
        )
    }
}
