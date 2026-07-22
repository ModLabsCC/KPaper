package cc.modlabs.kpaper.world.area

import cc.modlabs.kpaper.command.CommandBuilder
import cc.modlabs.kpaper.extensions.send
import cc.modlabs.kpaper.extensions.sendSuccessSound
import cc.modlabs.kpaper.world.area.model.AreaFlags
import cc.modlabs.kpaper.world.area.model.areas
import cc.modlabs.kpaper.world.area.model.getArea
import cc.modlabs.kpaper.world.toStringLocation
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.entity.Player
import java.util.*

class AreaCommand : CommandBuilder {

    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("area")
            .requires { it.sender.hasPermission("commands.build") }
            .executes { ctx ->
                val sender = ctx.source.sender as Player

                val standingIn = sender.location.areas()
                if (standingIn.isEmpty()) {
                    sender.send("No area here")
                } else if (standingIn.size == 1) {
                    sender.send("You are standing in ${standingIn.first().name}")
                } else {
                    sender.send("You are standing in ${standingIn.size} areas:")
                    standingIn.forEach { area ->
                        sender.send("- ${area.name}")
                    }
                }

                Command.SINGLE_SUCCESS
            }
            .then(Commands.literal("list")
                .executes { ctx ->
                    val sender = ctx.source.sender as Player

                    if (AreaCache.getAreas().isEmpty()) {
                        sender.send("<red>No areas found")
                        return@executes Command.SINGLE_SUCCESS
                    }

                    sender.send("<green>Areas:")
                    AreaCache.getAreas().forEach { area ->
                        sender.send("- ${area.name}")
                    }
                    Command.SINGLE_SUCCESS
                }
            )
            .then(Commands.literal("set")
                .then(Commands.argument("area", StringArgumentType.word())
                    .suggests { _, builder ->
                        AreaCache.getAreas().forEach {
                            builder.suggest(it.name)
                        }
                        builder.buildFuture()
                    }
                    .then(Commands.literal("p1")
                        .executes { ctx ->
                            val sender = ctx.source.sender as Player
                            val areaName = StringArgumentType.getString(ctx, "area").replace(" ", "_").lowercase(Locale.getDefault())

                            savePoint(sender, areaName, "p1")

                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(Commands.literal("p2")
                        .executes { ctx ->
                            val sender = ctx.source.sender as Player
                            val areaName = StringArgumentType.getString(ctx, "area").replace(" ", "_").lowercase(Locale.getDefault())

                            savePoint(sender, areaName, "p2")

                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(Commands.literal("entrysound").then(soundCommandLiteral("entry")))
                    .then(Commands.literal("exitsound").then(soundCommandLiteral("exit")))
                )
            )
            .then(Commands.literal("flag")
                .then(Commands.literal("add")
                    .then(Commands.argument("area", StringArgumentType.word())
                        .suggests { _, builder ->
                            AreaCache.getAreas().forEach {
                                builder.suggest(it.name)
                            }
                            builder.buildFuture()
                        }
                        .then(Commands.argument("flag", StringArgumentType.word())
                            .suggests { ctx, builder ->
                                AreaFlags.all().forEach { builder.suggest(it.key) }
                                builder.buildFuture()
                            }
                            .executes { ctx ->
                                val sender = ctx.source.sender as Player
                                val areaName = StringArgumentType.getString(ctx, "area")
                                val flag = StringArgumentType.getString(ctx, "flag")
                                addFlag(sender, areaName, flag)
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("area", StringArgumentType.word())
                        .suggests { _, builder ->
                            AreaCache.getAreas().forEach {
                                builder.suggest(it.name)
                            }
                            builder.buildFuture()
                        }
                        .then(Commands.argument("flag", StringArgumentType.word())
                            .suggests { ctx, builder ->
                                AreaFlags.all().forEach { builder.suggest(it.key) }
                                builder.buildFuture()
                            }
                            .executes { ctx ->
                                val sender = ctx.source.sender as Player
                                val areaName = StringArgumentType.getString(ctx, "area")
                                val flag = StringArgumentType.getString(ctx, "flag")
                                removeFlag(sender, areaName, flag)
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
                )
                .then(Commands.literal("list")
                    .then(Commands.argument("area", StringArgumentType.word())
                        .suggests { _, builder ->
                            AreaCache.getAreas().forEach {
                                builder.suggest(it.name)
                            }
                            builder.buildFuture()
                        }
                        .executes { ctx ->
                            val sender = ctx.source.sender as Player
                            val areaName = StringArgumentType.getString(ctx, "area")
                            val area = AreaCache.getArea(areaName) ?: return@executes Command.SINGLE_SUCCESS

                            sender.send("Flags for ${area.name}")
                            area.flags.forEach { (flag, value) ->
                                sender.send("- ${flag.key} = $value")
                            }
                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
            .then(Commands.literal("reload")
                .executes { ctx ->
                    val sender = ctx.source.sender as Player

                    AreaCache.reloadAreas()
                    sender.send("Reloaded areas")
                    Command.SINGLE_SUCCESS
                }
            )
            .build()
    }

    private fun soundCommandLiteral(type: String): RequiredArgumentBuilder<CommandSourceStack?, String?>? =
        Commands.argument("sound", StringArgumentType.word())
            .suggests { ctx, builder ->
                RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).forEach { sound ->
                    builder.suggest(sound.toString())
                }
                builder.buildFuture()
            }
            .executes { ctx ->
                executeSound(ctx, type)
                Command.SINGLE_SUCCESS
            }
            .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f, 2.0f))
                .executes { ctx ->
                    executeSound(ctx, type, FloatArgumentType.getFloat(ctx, "volume"))
                    Command.SINGLE_SUCCESS
                }
                .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.0f, 2.0f))
                    .executes { ctx ->
                        executeSound(ctx, type, FloatArgumentType.getFloat(ctx, "volume"), FloatArgumentType.getFloat(ctx, "pitch"))
                        Command.SINGLE_SUCCESS
                    }
                )
            )

    private fun executeSound(
        ctx: CommandContext<CommandSourceStack>,
        type: String,
        volume: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        val sender = ctx.source.sender as Player
        val areaName =
            StringArgumentType.getString(ctx, "area").replace(" ", "_").lowercase(Locale.getDefault())
        val soundKey = StringArgumentType.getString(ctx, "sound")

        saveSound(sender, areaName, soundKey, type, volume, pitch)
    }


    private fun saveSound(player: Player, areaName: String, sound: String, type: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val oldSound = AreaConfigService.saveSound(player.world.name, areaName, type, sound, volume, pitch)

        if (oldSound != null) {
            player.send("Replaced old sound $oldSound in $areaName")
        }

        player.send("Set new sound $sound in $areaName with type $type, volume $volume and pitch $pitch")
        player.sendSuccessSound()
    }

    private fun addFlag(player: Player, areaName: String, flag: String) {
        if (!AreaConfigService.addFlag(player.world.name, areaName, flag)) {
            player.send("<red>Flag $flag already enabled")
            return
        }
        player.sendSuccessSound()
        player.send("<green>Enabled Flag $flag in $areaName")
    }

    private fun removeFlag(player: Player, areaName: String, flag: String) {
        if (!AreaConfigService.removeFlag(player.world.name, areaName, flag)) {
            player.send("<red>Flag $flag not enabled")
            return
        }
        player.sendSuccessSound()
        player.send("<green>Removed Flag $flag in $areaName")
    }

    private fun savePoint(player: Player, areaName: String, point: String) {
        val oldArea = AreaConfigService.savePoint(
            player.world.name,
            areaName,
            point,
            player.location.toStringLocation().toString()
        )

        if (oldArea != null) {
            player.send("Replaced old area $oldArea with new point $point for $areaName")
        }

        player.send("Set point: $point for $areaName")
        player.sendSuccessSound()
    }
}