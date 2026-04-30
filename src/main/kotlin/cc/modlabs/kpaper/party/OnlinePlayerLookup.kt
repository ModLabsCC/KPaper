package cc.modlabs.kpaper.party

import org.bukkit.entity.Player
import java.util.UUID

/** Resolves whether a UUID corresponds to an online player (used by party APIs; replaceable in tests). */
fun interface OnlinePlayerLookup {
    operator fun invoke(uuid: UUID): Player?
}
