package dev.fruxz.ascend.extension

val Any?.isNull: Boolean
    get() = this == null

@Suppress("UNCHECKED_CAST")
fun <T> Any?.forceCast(): T = this as T

@Suppress("UNCHECKED_CAST")
fun <T> Any?.forceCastOrNull(): T? = this as? T

fun <T> T.dump(): T = this
