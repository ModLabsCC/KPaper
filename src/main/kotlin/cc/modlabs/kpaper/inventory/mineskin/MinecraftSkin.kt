package cc.modlabs.kpaper.inventory.mineskin

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftSkin(
    val textures: Textures
)