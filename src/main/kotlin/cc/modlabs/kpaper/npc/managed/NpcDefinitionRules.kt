package cc.modlabs.kpaper.npc.managed

import org.bukkit.inventory.EquipmentSlot
import java.util.Locale

internal fun updatedNpcAnimations(current: Set<NpcAnimation>, animation: NpcAnimation, enabled: Boolean) =
    if (enabled) current + animation else current - animation

internal fun validatedNpcWaypointName(value: String): String {
    val name = value.trim().lowercase(Locale.ROOT)
    require(name.matches(Regex("[a-z0-9_-]{1,32}"))) {
        "Wegpunkt-Namen dürfen nur a-z, 0-9, _ und - enthalten."
    }
    return name
}

internal val NPC_EQUIPMENT_SLOTS = setOf(
    EquipmentSlot.HAND,
    EquipmentSlot.OFF_HAND,
    EquipmentSlot.HEAD,
    EquipmentSlot.CHEST,
    EquipmentSlot.LEGS,
    EquipmentSlot.FEET,
)

internal fun npcEquipmentSlot(value: String): EquipmentSlot? = when (value.lowercase()) {
    "hand", "mainhand" -> EquipmentSlot.HAND
    "offhand" -> EquipmentSlot.OFF_HAND
    "head", "helmet" -> EquipmentSlot.HEAD
    "chest", "chestplate" -> EquipmentSlot.CHEST
    "legs", "leggings" -> EquipmentSlot.LEGS
    "feet", "boots" -> EquipmentSlot.FEET
    else -> null
}

internal val NPC_SKIN_LAYERS = linkedMapOf(
    "cape" to 0x01,
    "jacket" to 0x02,
    "left_sleeve" to 0x04,
    "right_sleeve" to 0x08,
    "left_pants" to 0x10,
    "right_pants" to 0x20,
    "hat" to 0x40,
)

internal fun updatedNpcSkinLayers(current: Byte, layer: String, enabled: Boolean): Byte {
    if (layer.equals("all", ignoreCase = true)) return if (enabled) ALL_NPC_SKIN_LAYERS else 0
    val bit = NPC_SKIN_LAYERS[layer.lowercase()] ?: error("Unbekannter Skin-Layer.")
    return if (enabled) (current.toInt() or bit).toByte() else (current.toInt() and bit.inv()).toByte()
}
