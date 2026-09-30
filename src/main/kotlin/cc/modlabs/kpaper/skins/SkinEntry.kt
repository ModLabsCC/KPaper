package cc.modlabs.kpaper.skins

data class SkinEntry(
    val id: String,
    val texture: String,
    val signature: String? = null,
    val displayName: String = humanizeSkinId(id),
    val source: SkinSource = SkinSource.LIBRARY,
    val mineSkinId: String? = null,
    val playerName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    init {
        require(id == validatedSkinId(id)) { "Skin ID must be normalized" }
        validatedSkinDisplayName(displayName)
        require(texture.isNotBlank() && texture.length <= 32768)
        require(signature == null || signature.isNotBlank() && signature.length <= 8192)
        mineSkinId?.let(::validatedMineSkinId)
        playerName?.let(::validatedPlayerName)
        require(createdAt >= 0)
    }
    /** Compatibility for the packet NPC profile identity. */
    val key: String get() = id
}
