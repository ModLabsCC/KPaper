package cc.modlabs.kpaper.npc.managed

import org.bukkit.Location
import org.bukkit.entity.Player

/** Project adapters: no dependency on Geyser, furniture, music or protection plugins. */
class NpcHooks {
    var isBedrock: (Player) -> Boolean = { false }
    var seatLocation: (Location) -> Location? = { null }
    var musicPlaying: (Location, Double) -> Boolean = { _, _ -> false }
    var regionContains: (String, Location) -> Boolean = { _, _ -> false }
    var regionKeys: () -> Collection<String> = { emptyList() }
    var canSee: (Player, String) -> Boolean = { _, _ -> true }
    /** Customize project label scale, width and other display properties before spawning. */
    var configureHologram: (org.bukkit.entity.TextDisplay) -> Unit = {}
    var hologramHeight: (NpcPose) -> Double = ::npcLabelHeight
}

/** Return false to suppress the generic dialogue after handling a project interaction. */
typealias NpcAction = (Player, NpcDefinition, NpcDialogTrigger) -> Boolean
