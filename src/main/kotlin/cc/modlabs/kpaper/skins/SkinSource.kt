package cc.modlabs.kpaper.skins

enum class SkinSource {
    BUNDLED,
    LEGACY,
    MINESKIN,
    PLAYER,
    TEXTURE,
    LIBRARY;

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.name.equals(value, true) } ?: LIBRARY
    }
}
