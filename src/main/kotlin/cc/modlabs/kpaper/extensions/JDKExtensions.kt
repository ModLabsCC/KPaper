package cc.modlabs.kpaper.extensions

import cc.modlabs.kpaper.util.getInternalKPaperLogger
import cc.modlabs.kpaper.util.getLogger

@Deprecated("Moved into util folder", replaceWith = ReplaceWith("getLogger()", "cc.modlabs.kpaper.util"))
fun getLogger(): org.slf4j.Logger = getLogger()

@Deprecated("Moved into util folder", replaceWith = ReplaceWith("getInternalKPaperLogger()", "cc.modlabs.kpaper.util"))
fun getInternalKPaperLogger(): org.slf4j.Logger = getInternalKPaperLogger()
