package cc.modlabs.kpaper.inventory.mineskin

data class MineSkinPagination(
    val current: After? = null,
    val next: After? = null,
)

data class After(
    val after: String
)
