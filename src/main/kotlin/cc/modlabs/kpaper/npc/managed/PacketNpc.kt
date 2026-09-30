package cc.modlabs.kpaper.npc.managed

import com.mojang.authlib.GameProfile
import net.minecraft.world.entity.Interaction
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.UUID

internal data class PacketNpc(
    val entityId: Int,
    val uuid: UUID,
    val profile: GameProfile,
    val worldId: UUID,
    var x: Double,
    var y: Double,
    var z: Double,
    var yaw: Float,
    var pitch: Float,
    val lookClose: Boolean,
    val lookCloseRangeSquared: Double,
    val equipment: Map<EquipmentSlot, ItemStack>,
    val hasProximityDialog: Boolean,
    val dialogRangeSquared: Double,
    val skinLayers: Byte,
    var pose: NpcPose,
    val nameVisible: Boolean,
    val seat: Interaction?,
    val hologram: net.minecraft.world.entity.Display.TextDisplay?,
)

internal data class NpcRotation(val yaw: Float, val pitch: Float)
