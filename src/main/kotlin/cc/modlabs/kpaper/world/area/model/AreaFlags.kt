package cc.modlabs.kpaper.world.area.model

import java.util.concurrent.ConcurrentHashMap

class AreaFlag<T>(
    val key: String,
    private val decode: (Any?) -> T?,
    private val encode: (T) -> Any = { it as Any },
) {
    fun decodeValue(rawValue: Any?): T? = decode(rawValue)
    fun encodeValue(value: T): Any = encode(value)
}

object AreaFlags {
    private val registry = ConcurrentHashMap<String, AreaFlag<*>>()

    val PVP = register(booleanFlag("PVP"))
    val PVE = register(booleanFlag("PVE"))
    val SURVIVAL = register(booleanFlag("SURVIVAL"))

    fun all(): Collection<AreaFlag<*>> = registry.values.sortedBy { it.key }

    fun get(key: String): AreaFlag<*>? = registry[key.uppercase()]

    fun getOrCreateBoolean(key: String): AreaFlag<Boolean> {
        val normalized = key.uppercase()
        val existing = registry[normalized]
        if (existing != null) {
            @Suppress("UNCHECKED_CAST")
            return existing as? AreaFlag<Boolean> ?: booleanFlag(normalized)
        }

        val flag = booleanFlag(normalized)
        registry[normalized] = flag
        return flag
    }

    fun <T> register(flag: AreaFlag<T>): AreaFlag<T> {
        registry[flag.key.uppercase()] = flag
        return flag
    }

    private fun booleanFlag(key: String) = AreaFlag(
        key = key.uppercase(),
        decode = { raw ->
            when (raw) {
                is Boolean -> raw
                is Number -> raw.toInt() != 0
                is String -> raw.equals("true", ignoreCase = true)
                else -> null
            }
        },
        encode = { it }
    )
}