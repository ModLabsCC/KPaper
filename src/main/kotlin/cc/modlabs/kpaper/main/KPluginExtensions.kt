package cc.modlabs.kpaper.main

/**
 * Docs-friendly configuration loader stub. Returns a new instance of T using the no-arg constructor.
 * Replace with your own implementation or bind to your config library.
 */
inline fun <reified T : Any> KPlugin.loadConfiguration(): T =
    T::class.java.getDeclaredConstructor().newInstance()

