package cc.modlabs.kpaper.inventory.mineskin

import kotlinx.serialization.Serializable


@Serializable
data class MineSkinTextureData(
    val texture: Texture = Texture(),
    val uuid: String = ""
)