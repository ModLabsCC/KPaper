package cc.modlabs.kpaper.inventory.mineskin.models.texture

import cc.modlabs.kpaper.inventory.mineskin.models.MineSkinLinks
import cc.modlabs.kpaper.inventory.mineskin.models.MineSkinWarning


data class MineSkinSingleSkinResponse(
    val success: Boolean,
    val skin: MineSkinResponse? = null,
    val warnings: List<MineSkinWarning>,
    val messages: List<Any>,
    val links: MineSkinLinks
)
