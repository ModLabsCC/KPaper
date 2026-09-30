package cc.modlabs.kpaper.skins

import cc.modlabs.kpaper.command.CommandBuilder
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class SkinCommand(private val plugin: JavaPlugin, private val library: () -> SkinLibraryService) : CommandBuilder {
    override val description = "Manage the shared skin library"
    override fun register(): LiteralCommandNode<CommandSourceStack> = Commands.literal("skins")
        .requires { it.sender.hasPermission(PERMISSION) }
        .then(Commands.literal("list").executes { ctx -> list(ctx, 1) }
            .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes { ctx -> list(ctx, IntegerArgumentType.getInteger(ctx, "page")) }))
        .then(Commands.literal("reload").executes { ctx -> run(ctx) { it.reload() } })
        .then(Commands.literal("rename").then(skinId().then(Commands.argument("name", StringArgumentType.greedyString())
            .executes { ctx -> run(ctx) { it.rename(string(ctx, "skin"), string(ctx, "name")) } })))
        .then(Commands.literal("rename-id").then(skinId().then(Commands.argument("id", StringArgumentType.word())
            .executes { ctx -> run(ctx) { it.renameId(string(ctx, "skin"), string(ctx, "id")) } })))
        .then(Commands.literal("player").then(Commands.argument("player", StringArgumentType.word()).executes { ctx ->
            import(ctx) { library().importPlayerSkin(string(ctx, "player")) }
        }))
        .then(Commands.literal("mineskin").then(Commands.argument("mineskin", StringArgumentType.word())
            .then(Commands.argument("name", StringArgumentType.greedyString()).executes { ctx ->
                import(ctx) { library().importMineSkin(string(ctx, "name"), string(ctx, "mineskin")) }
            })))
        .build()

    private fun skinId() = Commands.argument("skin", StringArgumentType.word()).suggests { _, builder ->
        library().keys().filter { it.lowercase(Locale.ROOT).startsWith(builder.remaining.lowercase(Locale.ROOT)) }.forEach(builder::suggest)
        builder.buildFuture()
    }
    private fun string(ctx: CommandContext<CommandSourceStack>, name: String) = StringArgumentType.getString(ctx, name)
    private fun list(ctx: CommandContext<CommandSourceStack>, requested: Int) = run(ctx) {
        val entries = it.all()
        val pages = maxOf(1, (entries.size + 6) / 7)
        val page = requested.coerceAtMost(pages)
        ctx.source.sender.sendMessage(Component.text("Skins ${entries.size}, page $page/$pages: " +
            entries.drop((page - 1) * 7).take(7).joinToString { skin -> "${skin.id} (${skin.displayName})" }.ifEmpty { "none" }))
    }
    private fun run(ctx: CommandContext<CommandSourceStack>, action: (SkinLibraryService) -> Unit): Int = try {
        action(library())
        ctx.source.sender.sendMessage(Component.text("Skin command completed."))
        1
    } catch (failure: Exception) { error(ctx, failure); 0 }

    private fun import(ctx: CommandContext<CommandSourceStack>, action: () -> CompletableFuture<SkinEntry>): Int = try {
        action().whenComplete { skin, failure ->
            if (plugin.isEnabled) Bukkit.getScheduler().runTask(plugin, Runnable {
                if (failure != null) error(ctx, failure)
                else ctx.source.sender.sendMessage(Component.text("Imported skin '${skin.id}'."))
            })
        }
        ctx.source.sender.sendMessage(Component.text("Importing skin..."))
        1
    } catch (failure: Exception) { error(ctx, failure); 0 }

    private fun error(ctx: CommandContext<CommandSourceStack>, failure: Throwable) {
        val cause = if (failure is CompletionException) failure.cause ?: failure else failure
        ctx.source.sender.sendMessage(Component.text(cause.message ?: "Skin command failed"))
    }

    companion object { const val PERMISSION = "kpaper.skins.admin" }
}
