package cc.modlabs.kpaper.inventory.mineskin

import cc.modlabs.kpaper.inventory.mineskin.MineSkinLinks
import cc.modlabs.kpaper.inventory.mineskin.MineSkinWarning


data class MineSkinSingleSkinResponse(
    val success: Boolean,
    val skin: MineSkinResponse? = null,
    val warnings: List<MineSkinWarning>,
    val messages: List<Any>,
    val links: MineSkinLinks
)
