package dev.fruxz.stacked

import cc.modlabs.kpaper.messages.miniMessageSerializer
import cc.modlabs.kpaper.messages.text as kpaperText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

fun text(
    content: String,
    serializer: MiniMessage = miniMessageSerializer,
    tagResolver: TagResolver = TagResolver.standard(),
): Component = kpaperText(content, serializer, tagResolver)
