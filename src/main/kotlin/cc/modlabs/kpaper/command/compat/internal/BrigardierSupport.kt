@file:Suppress("MemberVisibilityCanBePrivate")

package cc.modlabs.kpaper.command.compat.internal

import cc.modlabs.kpaper.event.listen
import cc.modlabs.kpaper.extensions.onlinePlayers
import cc.modlabs.kpaper.extensions.server
import cc.modlabs.kpaper.main.PluginInstance
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import org.bukkit.event.player.PlayerJoinEvent

/**
 * This class provides Brigardier support. It does that
 * by using reflection once. Additionally, this class is
 * using some obfuscated functions.
 */
object BrigardierSupport {
    @PublishedApi
    internal val commands = LinkedHashSet<LiteralArgumentBuilder<CommandSourceStack>>()

    internal var executedDefaultRegistration = false
        private set

    init {
        listen<PlayerJoinEvent> { event ->
            val player = event.player
            val permAttachment = player.addAttachment(PluginInstance)
            commands.forEach {
                permAttachment.setPermission("minecraft.command.${it.literal}", true)
            }
        }
    }

    @Suppress("HasPlatformType")
    fun resolveCommandManager() = (server as org.bukkit.craftbukkit.CraftServer)
        .server.commands

    internal fun registerAll() {
        executedDefaultRegistration = true

        // TODO unregister commands which are now missing due to a possible reload
        if (commands.isNotEmpty()) {
            commands.forEach {
                resolveCommandManager().dispatcher.register(it)
            }
            if (onlinePlayers.isNotEmpty())
                updateCommandTree()
        }
    }

    fun updateCommandTree() {
        onlinePlayers.forEach {
            // send the command tree
            resolveCommandManager().sendCommands((it as org.bukkit.craftbukkit.entity.CraftPlayer).handle)
        }
    }
}