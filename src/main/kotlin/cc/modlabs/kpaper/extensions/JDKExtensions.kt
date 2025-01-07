package cc.modlabs.kpaper.extensions

import cc.modlabs.kpaper.main.PluginInstance
import org.slf4j.LoggerFactory
import java.util.OptionalInt
import kotlin.math.pow

fun getLogger(): org.slf4j.Logger {
    return LoggerFactory.getLogger(PluginInstance::class.java)
}

fun <T : Any> T.nullIf(condition: (T) -> Boolean): T? {
    return if (condition(this)) null else this
}


val Int.to3Digits: String
    get() = "%03d".format(this)

fun Int.toDigits(length: Int): String {
    return "%0${length}d".format(this)
}

fun Int.toDigitsReversed(length: Int): String {
    val max = 10.0.pow(length.toDouble()).toInt() - 1
    return "%0${length}d".format(max - this)
}

val OptionalInt.to3Digits: String
    get() = if (this.isPresent) {
        "%03d".format(this.getAsInt())
    } else {
        "%03d".format(0)
    }

val OptionalInt.to3DigitsReversed: String
    get() = if (this.isPresent) {
        "%03d".format(999 - this.getAsInt())
    } else {
        "%03d".format(999)
    }

fun Int.toFixedString(digits: Int = 3): String {
    return this.toString().padStart(digits, '0')
}