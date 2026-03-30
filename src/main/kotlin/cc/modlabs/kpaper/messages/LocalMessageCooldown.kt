package cc.modlabs.kpaper.messages

import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object LocalMessageCooldown {
    @OptIn(ExperimentalTime::class)
    private val _cache = mutableMapOf<UUID, Pair<String, Instant>>()

    @OptIn(ExperimentalTime::class)
    fun addCooldown(player: UUID, message: String, cooldown: Duration) {
        _cache[player] = message to Clock.System.now().plus(cooldown)
    }

    @OptIn(ExperimentalTime::class)
    fun hasCooldown(player: UUID, message: String): Boolean {
        val cached = _cache[player] ?: return false
        return cached.first == message && cached.second > Clock.System.now()
    }
}