package cc.modlabs.kpaper.npc

import org.bukkit.Location
import org.bukkit.entity.Mannequin

/**
 * Extension function to create a [NPC] from a [Location].
 *
 * @param block The configuration block for the mannequin.
 * @return The created MannequinNPC.
 *
 * @example
 * ```kotlin
 * val npc = location.createMannequinNPC {
 *     name("Shop Keeper")
 *     description("&7Click to open shop!")
 *     profile(playerProfile)
 *     immovable(true)
 *     helmet(ItemStack(Material.DIAMOND_HELMET))
 * }
 * ```
 */
fun Location.createNPC(block: NPCBuilder.() -> Unit = {}): NPC {
    return NPCBuilder(this).apply(block).build()
}

/**
 * Creates a simple [NPC] with just a name and location.
 *
 * @param location The location where the mannequin should be spawned.
 * @param name The name of the mannequin.
 * @return The created MannequinNPC.
 */
fun createSimpleNPC(location: Location, name: String): NPC {
    return NPCBuilder(location)
        .name(name)
        .build()
}

/**
 * Creates a [NPC] with a player profile (skin).
 *
 * @param location The location where the mannequin should be spawned.
 * @param profile The player profile to use for the mannequin's appearance.
 * @param name The name of the mannequin (optional).
 * @return The created MannequinNPC.
 */
fun createNPCWithProfile(
    location: Location,
    profile: MannequinProfile,
    name: String? = null
): NPC {
    return NPCBuilder(location).apply {
        this.profile(profile)
        name?.let { this.name(it) }
    }.build()
}

/**
 * Converts an existing [Mannequin] entity to a [NPC].
 * Useful when you already have a mannequin entity and want to use the NPC API.
 *
 * @param mannequin The existing Mannequin entity.
 * @return A MannequinNPC wrapping the entity.
 */
fun Mannequin.toNPC(): NPC {
    return NPCImpl(this)
}

