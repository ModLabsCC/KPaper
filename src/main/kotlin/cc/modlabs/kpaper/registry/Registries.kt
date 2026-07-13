package cc.modlabs.kpaper.registry

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed

fun <T : Keyed> registry(registryKey: RegistryKey<T>) =
    RegistryAccess.registryAccess().getRegistry(registryKey)

fun <T : Keyed> registryValue(registryKey: RegistryKey<T>, key: Key): T? =
    registry(registryKey).get(key)

fun <T : Keyed> requireRegistryValue(registryKey: RegistryKey<T>, key: Key): T =
    requireNotNull(registryValue(registryKey, key)) { "Unknown ${registryKey.key()} value: $key" }
