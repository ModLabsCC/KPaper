package dev.fruxz.stacked.extension

import cc.modlabs.kpaper.messages.asPlainString as kpaperAsPlainString
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.title.Title as AdventureTitle
import java.time.Duration
import kotlin.time.Duration as KotlinDuration

val ComponentLike.asPlainString: String
    get() = kpaperAsPlainString

fun Times(
    fadeIn: KotlinDuration,
    stay: KotlinDuration,
    fadeOut: KotlinDuration,
): AdventureTitle.Times = AdventureTitle.Times.times(
    Duration.ofMillis(fadeIn.inWholeMilliseconds),
    Duration.ofMillis(stay.inWholeMilliseconds),
    Duration.ofMillis(fadeOut.inWholeMilliseconds),
)

fun Title(
    title: Component,
    subtitle: Component,
    times: AdventureTitle.Times,
): AdventureTitle = AdventureTitle.title(title, subtitle, times)
