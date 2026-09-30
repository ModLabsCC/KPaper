package cc.modlabs.kpaper.npc.managed

import cc.modlabs.klassicx.tools.minecraft.StringLocation
import cc.modlabs.kpaper.world.toBukkitLocation
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.Locale

/** Authored position survives unloaded worlds; runtime movement does not rewrite it. */
data class NpcDefinition(
    val id: String,
    val position: StringLocation,
    val name: String = id.take(16),
    val skin: String? = null,
    val nameVisible: Boolean = true,
    val lookClose: Boolean = false,
    val lookCloseRange: Double = 5.0,
    val equipment: Map<EquipmentSlot, ItemStack> = emptyMap(),
    val skinLayers: Byte = ALL_NPC_SKIN_LAYERS,
    val pose: NpcPose = NpcPose.STANDING,
    val animations: Set<NpcAnimation> = emptySet(),
    val dialogLines: List<String> = emptyList(),
    val dialogTrigger: NpcDialogTrigger = NpcDialogTrigger.CLICK,
    val dialogSelection: NpcDialogSelection = NpcDialogSelection.ALL,
    val dialogRange: Double = 4.0,
    val dialogSound: String? = null,
    val dialogSoundVolume: Float = 1.0f,
    val dialogSoundPitch: Float = 1.0f,
    val clickSound: String? = null,
    val clickSoundPitch: Float = 1.0f,
    val action: String? = null,
    val actionTrigger: NpcDialogTrigger = NpcDialogTrigger.CLICK,
    val actionData: Map<String, String> = emptyMap(),
    val traits: Map<String, String> = emptyMap(),
    val movement: NpcMovementSettings = NpcMovementSettings(),
    /** Optional MiniMessage label above the NPC; replaces the native nameplate. */
    val hologram: String? = null,
) {
    val location get() = position.toBukkitLocation()

    init {
        require(id == normalizedNpcId(id)) { "NPC ID must be normalized" }
        if (hologram == null) validatedNpcName(name) else {
            require(name.none(Char::isISOControl))
            validatedNpcLabel(name)
            validatedNpcLabel(hologram)
        }
        validateNpcPosition(position)
        require(lookCloseRange in 1.0..32.0 && dialogRange in 1.0..16.0)
        require(equipment.keys.all { it in NPC_EQUIPMENT_SLOTS }) { "Unsupported equipment slot" }
        require(skinLayers.toInt() in 0..127)
        require(dialogLines.size <= 256)
        dialogLines.forEach(::validatedNpcDialogLine)
        dialogSound?.let(::validatedNpcDialogSound)
        clickSound?.let(::validatedNpcDialogSound)
        require(dialogSoundVolume in 0f..16f && dialogSoundPitch in 0.5f..2f && clickSoundPitch in 0.5f..2f)
        action?.let(::validatedNpcKey)
        traits.keys.forEach(::validatedNpcKey)
        require((actionData.values + traits.values).all { it.length <= 4096 })
        require(actionData.keys.all { it.matches(Regex("[a-z0-9_-]{1,64}")) })
        require(movement.speed in 0.2..8.0 && movement.waitTicks in 0..6000 && movement.roamingRadius in 1.0..64.0)
        require(movement.danceToMusicRadius == null || movement.danceToMusicRadius in 1.0..64.0)
        require(movement.waypoints.size <= 256 && movement.waypoints.map { it.name }.distinct().size == movement.waypoints.size)
        movement.waypoints.forEach {
            require(it.name == validatedNpcWaypointName(it.name) && it.weight in 1..1000)
            validateNpcPosition(it.position)
            require(it.position.world == position.world) { "Waypoints must be in the NPC world" }
        }
    }
}

data class NpcWaypoint(val name: String, val position: StringLocation, val weight: Int = 1) {
    val location get() = position.toBukkitLocation()
}

internal fun normalizedNpcId(value: String): String = value.trim().lowercase(Locale.ROOT).also {
    require(it.matches(Regex("[a-z0-9_-]{1,64}"))) { "NPC IDs use a-z, 0-9, _ and - (1-64 characters)" }
}

internal fun validatedNpcKey(value: String): String = value.also {
    require(it.matches(Regex("[a-z0-9_-]+:[a-z0-9_/-]+"))) { "Expected a namespaced key (plugin:action)" }
}

private fun validateNpcPosition(position: StringLocation) {
    require(position.world.isNotBlank() && listOf(position.x, position.y, position.z).all(Double::isFinite))
    require(position.yaw.isFinite() && position.pitch in -90f..90f)
}

internal fun StringLocation.snapshot() = StringLocation(x, y, z, yaw, pitch, world)

internal fun NpcDefinition.snapshot() = copy(
    position = position.snapshot(), equipment = equipment.mapValues { it.value.clone() },
    dialogLines = dialogLines.toList(), animations = animations.toSet(),
    actionData = actionData.toMap(), traits = traits.toMap(),
    movement = movement.copy(waypoints = movement.waypoints.map { it.copy(position = it.position.snapshot()) }),
)
