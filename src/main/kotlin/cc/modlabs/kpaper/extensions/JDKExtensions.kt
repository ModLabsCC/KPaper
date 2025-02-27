package cc.modlabs.kpaper.extensions

import cc.modlabs.kpaper.main.PluginInstance
import org.slf4j.LoggerFactory

fun getLogger(): org.slf4j.Logger {
    return LoggerFactory.getLogger(PluginInstance::class.java)
}

fun getInternalKPaperLogger(): org.slf4j.Logger {
    return LoggerFactory.getLogger("cc.modlabs.kpaper")
}
