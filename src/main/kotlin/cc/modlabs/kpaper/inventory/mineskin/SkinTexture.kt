package cc.modlabs.kpaper.inventory.mineskin

import java.util.UUID

data class SkinTexture(
    val name: String,
    val uuid: UUID,
    val texture: Texture
) {

    companion object {
        fun fromMineSkinResponse(response: MineSkinResponse): SkinTexture {
            return SkinTexture(
                name = "skin${response.uuid.take(7)}",
                uuid = UUID.fromString(response.data.uuid),
                texture = response.data.texture
            )
        }
    }

}