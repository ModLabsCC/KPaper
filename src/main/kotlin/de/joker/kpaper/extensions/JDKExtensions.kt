package de.joker.kpaper.extensions

import de.joker.kpaper.main.PluginInstance
import org.slf4j.LoggerFactory

fun getLogger(): org.slf4j.Logger {
    return LoggerFactory.getLogger(PluginInstance::class.java)
}

fun <T : Any> T.nullIf(condition: (T) -> Boolean): T? {
    return if (condition(this)) null else this
}