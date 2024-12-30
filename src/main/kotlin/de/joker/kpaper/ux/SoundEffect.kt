package de.joker.kpaper.ux

import org.bukkit.Sound

data class SoundEffect(
    val sound: Sound,
    val volume: Float = 0.4f,
    val pitch: Float = 1f
) {

    constructor(sound: String, volume: Float = 0.4f, pitch: Float = 1f) : this(Sound.valueOf(sound), volume, pitch)

}