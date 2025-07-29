package cc.modlabs.kpaper.extensions

import cc.modlabs.kpaper.functions.getInternalKPaperLogger
import cc.modlabs.kpaper.functions.getLogger

@Deprecated("Moved into functions folder", replaceWith = ReplaceWith("getLogger()", "cc.modlabs.kpaper.functions"))
fun getLogger(): org.slf4j.Logger = getLogger()

@Deprecated("Moved into functions folder", replaceWith = ReplaceWith("getInternalKPaperLogger()", "cc.modlabs.kpaper.functions"))
fun getInternalKPaperLogger(): org.slf4j.Logger = getInternalKPaperLogger()
