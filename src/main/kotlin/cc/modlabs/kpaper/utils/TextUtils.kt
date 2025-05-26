package cc.modlabs.kpaper.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

fun Component.toLegacy(): String {
    return LegacyComponentSerializer.legacySection().serialize(this)
}