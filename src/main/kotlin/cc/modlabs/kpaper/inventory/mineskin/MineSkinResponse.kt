package cc.modlabs.kpaper.inventory.mineskin

data class MineSkinResponse(
    val uuid: String,
    val shortId: String,
    val name: String?,
    val visibility: String,
    val variant: String,
    val texture: MineSkinTexture,
    val generator: MineSkinGenerator,
    val tags: List<MineSkinTags>,
    val views: Int,
    val duplicate: Boolean
)

data class MineSkinTags(
    val tag: String,
)

data class MineSkinGenerator(
    val timestamp: Long,
    val account: String,
    val server: String,
    val worker: String,
    val version: String,
    val duration: Int
)