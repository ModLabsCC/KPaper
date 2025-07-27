package cc.modlabs.kpaper.messages

import dev.fruxz.ascend.tool.time.calendar.Calendar
import java.util.*
import kotlin.time.Duration

object LocalMessageCooldown {
    private val _cache = mutableMapOf<UUID, Pair<String, Calendar>>()

    fun addCooldown(player: UUID, message: String, cooldown: Duration) {
        _cache[player] = message to Calendar.now().add(cooldown)
    }

    fun hasCooldown(player: UUID, message: String): Boolean {
        val cached = _cache[player] ?: return false
        return cached.first == message && cached.second.isAfter(Calendar.now())
    }
}