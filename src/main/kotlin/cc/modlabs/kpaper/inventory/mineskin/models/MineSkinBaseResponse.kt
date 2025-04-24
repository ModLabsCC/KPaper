package cc.modlabs.kpaper.inventory.mineskin.models

data class MineSkinBaseResponse(
    val success: Boolean,
    val skins: List<MineSkinRawSkinResponse>,
    val pagination: MineSkinPagination,
    val warnings: List<MineSkinWarning>,
    val messages: List<Any>,
    val links: MineSkinLinks,
)
