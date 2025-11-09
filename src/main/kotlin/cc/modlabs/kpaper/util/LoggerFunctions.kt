package cc.modlabs.kpaper.util

import cc.modlabs.kpaper.main.PluginInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.logging.Level
import java.util.logging.Logger as JULLogger

fun getLogger(): Logger {
    return LoggerFactory.getLogger(PluginInstance::class.java)
}

fun getInternalKPaperLogger(): Logger {
    return LoggerFactory.getLogger("cc.modlabs.kpaper")
}

// Docs-friendly console + logging helpers
fun consoleOutput(message: String) {
    PluginInstance.logger.info(message)
}

fun logInfo(message: String) = PluginInstance.logger.info(message)
fun logWarning(message: String) = PluginInstance.logger.warning(message)
fun logError(message: String, exception: Throwable? = null) {
    if (exception != null) PluginInstance.logger.log(Level.SEVERE, message, exception) else PluginInstance.logger.severe(message)
}
fun logDebug(message: String) = PluginInstance.logger.info("[DEBUG] $message")
fun logSuccess(message: String) = PluginInstance.logger.info("[SUCCESS] $message")
fun logFailure(message: String) = PluginInstance.logger.severe("[FAILURE] $message")

// Extension helpers on java.util.logging.Logger to allow logger.error/warn/debug in examples
fun JULLogger.error(message: String, exception: Throwable? = null) {
    if (exception != null) this.log(Level.SEVERE, message, exception) else this.severe(message)
}

fun JULLogger.warn(message: String) = this.warning(message)
fun JULLogger.debug(message: String) = this.info("[DEBUG] $message")
