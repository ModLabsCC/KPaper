package cc.modlabs.kpaper.inventory.mineskin

import kotlinx.serialization.Serializable

@Serializable
data class MineSkinResponse(
    val account: Int = 0,
    val accountId: Int = 0,
    val `data`: MineSkinTextureData = MineSkinTextureData(),
    val duration: Int = 0,
    val hash: String = "",
    val id: Int = 0,
    val idStr: String,
    val model: String,
    val name: String,
    val `private`: Boolean,
    val server: String,
    val timestamp: Int,
    val uuid: String,
    val variant: String,
    val views: Int
)