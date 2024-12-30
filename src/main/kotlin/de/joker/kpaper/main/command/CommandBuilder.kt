package de.joker.kpaper.main.command

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack

interface CommandBuilder {

    fun register(): LiteralCommandNode<CommandSourceStack>

    val aliases: List<String>
        get() = listOf()

    val description: String
        get() = ""

}