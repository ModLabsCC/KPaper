package de.joker.kpaper.main.extensions

import dev.fruxz.ascend.tool.time.TimeUnit
import dev.fruxz.ascend.tool.time.clock.TimeDisplay
import kotlin.time.Duration

fun <T> Iterable<T>.sumOf(selector: (T) -> Duration): Duration {
    var sum = Duration.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

val Duration.betterString: String
    get() {
        return TimeDisplay(this).toClockString(TimeUnit.HOUR, TimeUnit.MINUTE, TimeUnit.SECOND)
    }