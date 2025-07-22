package cc.modlabs.kpaper.command.compat

import cc.modlabs.kpaper.command.compat.internal.BrigardierSupport
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack

/**
 * Registers this command at the [CommandDispatcher] of the server.
 *
 * @param sendToPlayers whether the new command tree should be send to
 * all players, this is true by default, but you can disable it if you are
 * calling this function as the server is starting
 */
fun LiteralArgumentBuilder<CommandSourceStack>.register(sendToPlayers: Boolean = true) {
    if (!BrigardierSupport.executedDefaultRegistration)
        BrigardierSupport.commands += this
    else {
        BrigardierSupport.resolveCommandManager().dispatcher.register(this)
        if (sendToPlayers)
            BrigardierSupport.updateCommandTree()
    }
}