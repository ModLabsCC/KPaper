package cc.modlabs.kpaper.command.arguments

import cc.modlabs.kpaper.extensions.onlinePlayers
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class PlayerArgument : CustomArgumentType<Player, String> {

    override fun parse(reader: StringReader): Player {
        val stringNotParsed = reader.readString()
        val player = Bukkit.getPlayer(stringNotParsed)
        if (player != null) {
            return player
        }

        throw IllegalArgumentException("Player $stringNotParsed not found")
    }

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val currentArg = context.input.lastOrNull() ?: run {
            onlinePlayers.forEach {
                builder.suggest(it.name)
            }
            return builder.buildFuture()
        }

        onlinePlayers.filter { player -> player.name.startsWith(currentArg) }.forEach {
            builder.suggest(it.name)
        }

        return builder.buildFuture()
    }

}