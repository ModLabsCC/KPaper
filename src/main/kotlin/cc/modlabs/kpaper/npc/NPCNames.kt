package cc.modlabs.kpaper.npc

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Entity

internal fun Entity.plainCustomName(): String? =
    customName()?.let(PlainTextComponentSerializer.plainText()::serialize)
