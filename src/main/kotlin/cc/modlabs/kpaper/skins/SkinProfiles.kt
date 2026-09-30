package cc.modlabs.kpaper.skins

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import org.bukkit.Bukkit
import java.util.UUID

object SkinProfiles {
    private const val TEXTURES_PROPERTY = "textures"

    fun apply(profile: PlayerProfile, skin: SkinEntry): PlayerProfile = apply(profile, skin.texture, skin.signature)

    fun apply(profile: PlayerProfile, texture: String, signature: String?): PlayerProfile = profile.also {
        it.removeProperty(TEXTURES_PROPERTY)
        it.setProperty(
            signature?.let { value -> ProfileProperty(TEXTURES_PROPERTY, texture, value) }
                ?: ProfileProperty(TEXTURES_PROPERTY, texture),
        )
    }

    fun create(skin: SkinEntry, profileName: String = "SkinLibrary"): PlayerProfile =
        apply(Bukkit.createProfile(UUID.randomUUID(), profileName.take(16)), skin)

    fun fromProfile(id: String, displayName: String, profile: PlayerProfile): SkinEntry? =
        profile.properties.firstOrNull { it.name == TEXTURES_PROPERTY && it.value.isNotBlank() }?.let {
            SkinEntry(
                id = id,
                texture = it.value,
                signature = it.signature,
                displayName = displayName,
                source = SkinSource.LIBRARY,
            )
        }
}
