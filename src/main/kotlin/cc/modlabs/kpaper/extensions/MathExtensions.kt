package cc.modlabs.kpaper.extensions

import cc.modlabs.kpaper.utils.Base64
import kotlin.math.abs


private fun getNextId(x: Int, y: Int): Pair<Int, Int> {
    val absX: Int = abs(x)
    val absY: Int = abs(y)
    return if (absX > absY) {
        if (x > 0) {
            Pair(x, y + 1)
        } else {
            Pair(x, y - 1)
        }
    } else if (absY > absX) {
        if (y > 0) {
            Pair(x - 1, y)
        } else {
            Pair(x + 1, y)
        }
    } else {
        if (x == y && x > 0) {
            return Pair(x, y + 1)
        }
        if (x == absX) {
            return Pair(x, y + 1)
        }
        if (y == absY) {
            Pair(x, y - 1)
        } else Pair(x + 1, y)
    }
}

fun getPlotId(id: Int): Pair<Int, Int> {
    var x = 0
    var y = 0
    var i = 0
    while (i < id) {
        val next = getNextId(x, y)
        x = next.first
        y = next.second
        i++
    }
    return Pair(x, y)
}

fun getPlotIdAsHash(id: Int): String {
    val pair = getPlotId(id)
    return Base64.encodeToString("${pair.first}:${pair.second}").replace("=", "")
}