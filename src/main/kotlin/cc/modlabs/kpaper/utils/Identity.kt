package cc.modlabs.kpaper.utils

import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.bukkit.NamespacedKey

interface Identity<T> : Key, Keyed {

    val identityKey: Key

    override fun namespace(): String = identityKey.namespace()
    override fun key(): Key {
        return identityKey.key()
    }

    override fun value(): String = identityKey.value()

    override fun asString(): String = identityKey.asString()

    override fun getKey() = NamespacedKey.fromString(asString())!!

}