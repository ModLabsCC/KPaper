package cc.modlabs.kpaper.util

import cc.modlabs.kpaper.main.PluginInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun getLogger(): Logger {
    return LoggerFactory.getLogger(PluginInstance::class.java)
}

fun getInternalKPaperLogger(): Logger {
    return LoggerFactory.getLogger("cc.modlabs.kpaper")
}