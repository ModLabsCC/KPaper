package cc.modlabs.kpaper.command

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

interface CommandBuilder {

    fun register(): LiteralCommandNode<CommandSourceStack>

    val aliases: List<String>
        get() = listOf()

    val description: String
        get() = ""

    fun handleRegister(commands: Commands)  {
        commands.register(
            register(),
            description,
            aliases
        )
    }

}