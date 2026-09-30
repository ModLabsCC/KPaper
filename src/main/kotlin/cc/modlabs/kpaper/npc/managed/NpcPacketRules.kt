package cc.modlabs.kpaper.npc.managed

import cc.modlabs.kpaper.skins.SkinEntry
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import com.google.common.collect.ImmutableMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import org.bukkit.Location
import org.bukkit.inventory.EquipmentSlot
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

internal const val ALL_NPC_SKIN_LAYERS: Byte = 0x7f

internal fun npcSeatPosition(location: Location, furnitureSeat: Location? = null): Location {
    furnitureSeat?.let { return it.clone() }
    if (location.isWorldLoaded) {
        val feet = location.block
        val support = if (feet.isPassable) location.clone().subtract(0.0, 0.0625, 0.0).block else feet
        if (!support.isPassable) return Location(support.world, support.x + 0.5,
            support.boundingBox.maxY + 0.05, support.z + 0.5, location.yaw, 0f)
    }
    return location.clone().apply {
        x = floor(x) + 0.5
        y += 0.05
        z = floor(z) + 0.5
    }
}

internal fun npcNameTeamName(entityId: Int) = "kpnpc${entityId.toUInt().toString(16)}"

internal fun hiddenNpcNameTeam(npc: PacketNpc) = PlayerTeam(Scoreboard(), npcNameTeamName(npc.entityId)).apply {
    nameTagVisibility = Team.Visibility.NEVER
    players.add(npc.profile.name)
}

internal fun npcPacketPose(pose: NpcPose) = if (pose == NpcPose.SITTING) EntityPose.STANDING else pose.entityPose
internal fun npcLabelHeight(pose: NpcPose) = when (pose) {
    NpcPose.SITTING -> 1.4
    NpcPose.CROUCHING -> 1.95
    NpcPose.LYING, NpcPose.CRAWLING -> 0.6
    else -> 2.3
}
internal fun npcProfileRemovalDelay(isBedrock: Boolean): Long? = if (isBedrock) null else 40L

internal fun npcLookRotation(
    fromX: Double,
    fromY: Double,
    fromZ: Double,
    toX: Double,
    toY: Double,
    toZ: Double,
): NpcRotation {
    val dx = toX - fromX
    val dy = toY - fromY
    val dz = toZ - fromZ
    val horizontal = sqrt(dx * dx + dz * dz)
    return NpcRotation(
        Math.toDegrees(-atan2(dx, dz)).toFloat(),
        Math.toDegrees(-atan2(dy, horizontal)).toFloat().coerceIn(-90f, 90f),
    )
}

internal fun npcTalkingRotation(rotation: NpcRotation, pitchOffset: Float) =
    rotation.copy(pitch = (rotation.pitch + pitchOffset).coerceIn(-90f, 90f))

internal fun samePacketRotation(first: NpcRotation?, second: NpcRotation) =
    first != null && angleByte(first.yaw) == angleByte(second.yaw) && angleByte(first.pitch) == angleByte(second.pitch)

private fun angleByte(angle: Float) = (angle * 256f / 360f).toInt().toByte()

internal fun EquipmentSlot.toNmsSlot() = when (this) {
    EquipmentSlot.HAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND
    EquipmentSlot.OFF_HAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND
    EquipmentSlot.HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD
    EquipmentSlot.CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST
    EquipmentSlot.LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS
    EquipmentSlot.FEET -> net.minecraft.world.entity.EquipmentSlot.FEET
    else -> error("Unsupported NPC equipment slot: $this")
}

internal fun shouldRenderNpc(playerWorld: UUID, npcWorld: UUID, distanceSquared: Double) =
    playerWorld == npcWorld && distanceSquared <= 96.0 * 96.0

internal fun npcProfile(id: String, name: String, skin: SkinEntry?, namespace: String): GameProfile {
    val profileName = renderedNpcName(name)
    val texture = skin?.let { entry -> entry.signature?.let { Property("textures", entry.texture, it) }
        ?: Property("textures", entry.texture) }
    return GameProfile(
        UUID.nameUUIDFromBytes(
            "$namespace:npc:$id:$profileName:${skin?.id}:${skin?.texture}".toByteArray(StandardCharsets.UTF_8),
        ),
        profileName,
        PropertyMap(if (texture == null) ImmutableMultimap.of() else ImmutableMultimap.of("textures", texture)),
    )
}

internal fun validatedNpcName(value: String): String {
    val name = value.trim()
    require(name.length <= 256 && name.none(Char::isISOControl)) { "Der NPC-Name enthält ungültige Zeichen." }
    renderedNpcName(name)
    return name
}

internal fun renderedNpcName(value: String): String {
    val component = NPC_MINI_MESSAGE.deserialize(value)
    val plain = NPC_PLAIN_NAME.serialize(component)
    require(plain.length in 1..16 && plain.none(Char::isISOControl)) {
        "Der NPC-Name muss 1 bis 16 sichtbare Zeichen lang sein."
    }
    return NPC_LEGACY_NAME.serialize(component).also {
        require(it.length <= 16) { "Der formatierte NPC-Name ist für ein natives Nametag zu lang." }
    }
}

internal fun parsedNpcName(value: String) = NPC_MINI_MESSAGE.deserialize(value)

private val NPC_MINI_MESSAGE = MiniMessage.miniMessage()
private val NPC_LEGACY_NAME = LegacyComponentSerializer.legacySection()
private val NPC_PLAIN_NAME = PlainTextComponentSerializer.plainText()
