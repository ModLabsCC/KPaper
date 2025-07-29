package cc.modlabs.kpaper.inventory.mineskin

data class MineSkinTexture(
    val `data`: MineSkinTextureData,
    val hash: MineSkinHash,
    val url: MineSkinUrls,
)

data class MineSkinTextureData(
    val value: String,
    val signature: String
)

data class MineSkinHash(
    val skin: String,
    val cape: String?
)

data class MineSkinUrls(
    val skin: String
)
