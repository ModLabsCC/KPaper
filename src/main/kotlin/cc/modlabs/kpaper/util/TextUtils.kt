package cc.modlabs.kpaper.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

fun Component.toLegacy(): String {
    return LegacyComponentSerializer.legacySection().serialize(this)
}