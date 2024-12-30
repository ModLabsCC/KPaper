package cc.modlabs.kpaper.extensions

import dev.fruxz.ascend.tool.time.TimeUnit
import dev.fruxz.ascend.tool.time.clock.TimeDisplay
import kotlin.time.Duration
import dev.fruxz.ascend.tool.time.calendar.Calendar
import java.text.SimpleDateFormat
import java.util.*

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

fun calendarFromDateString(dateFormat: String): Calendar {
    val cal: java.util.Calendar = java.util.Calendar.getInstance()
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    cal.time = sdf.parse(dateFormat) // all done
    return Calendar.fromLegacy(cal)
}

fun Calendar.formatToDay(locale: Locale): String {
    return SimpleDateFormat.getDateInstance(Calendar.FormatStyle.FULL.ordinal, locale).format(javaDate)
}