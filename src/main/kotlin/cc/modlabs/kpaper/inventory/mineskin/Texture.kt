package cc.modlabs.kpaper.inventory.mineskin

import kotlinx.serialization.Serializable

@Serializable
data class Texture(
    val signature: String = "",
    val url: String = "",
    val urls: Urls = Urls(),
    val value: String = ""
)