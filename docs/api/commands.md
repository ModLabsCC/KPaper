# Command Framework

Note: The fluent CommandBuilder DSL used in some examples is not included in this build of KPaper. You can either implement the `CommandBuilder` interface directly (examples below) or install the companion Gradle plugin, KPaperGradle, which auto‑wires command registration for you.

## Using KPaperGradle (recommended)

Project page: https://github.com/ModLabsCC/KPaperGradle

KPaperGradle automates KPaper setup, generates registration bootstrappers, and auto‑registers commands. It also discovers listeners (you call a one‑liner to register them).

1) Add plugin repositories

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://nexus.modlabs.cc/repository/maven-public/")
        mavenLocal()
    }
}
```

2) Apply the plugin and configure

```kotlin
// build.gradle.kts (your Paper plugin)
plugins {
    kotlin("jvm") version "2.0.0"
    id("cc.modlabs.kpaper-gradle") version "LATEST" // replace with latest
}

repositories {
    mavenCentral()
    maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
}

kpaper {
    javaVersion.set(21) // toolchain + compiler --release
    registrationBasePackage.set("com.example.myplugin") // scanned for commands/listeners
    // Optional: deliver extra runtime libs via Paper's loader
    // deliver("com.github.ben-manes.caffeine:caffeine:3.1.8")
}
```

3) Register discovered listeners (commands are auto‑registered)

```kotlin
import cc.modlabs.kpaper.main.KPlugin

class MyPlugin : KPlugin() {
    override fun startup() {
        cc.modlabs.registration.RegisterManager.registerListeners(this)
    }
}
```

4) Implement commands under your `registrationBasePackage`

```kotlin
package com.example.myplugin.commands

import cc.modlabs.kpaper.command.CommandBuilder
import io.papermc.paper.command.brigadier.Commands

class HelloCommand : CommandBuilder {
    override val description = "Say hello"
    override fun register() =
        Commands.literal("hello")
            .executes {
                it.source.sender.sendMessage("Hello from KPaper!")
                com.mojang.brigadier.Command.SINGLE_SUCCESS
            }
            .build()
}
```

That’s it — build as usual. The plugin generates the bootstrap classes, patches `paper-plugin.yml` as needed, and wires resources.

KPaper's command framework provides integration with Paper's Brigadier-based command system through the `CommandBuilder` interface, allowing you to create powerful commands with argument parsing, validation, and sub-command support.

## Basic Command Creation

### Simple Commands

Commands in KPaper are created by implementing the `CommandBuilder` interface:

```kotlin
import cc.modlabs.kpaper.command.CommandBuilder
import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

class HelloCommand : CommandBuilder {
    
    override val description: String = "Say hello to the world"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("hello")
            .executes { ctx ->
                val sender = ctx.source.sender
                sender.sendMessage("Hello, World!")
                Command.SINGLE_SUCCESS
            }
            .build()
    }
}
```

### Command with Arguments

```kotlin
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.ArgumentType
import io.papermc.paper.command.brigadier.argument.ArgumentTypes

class GreetCommand : CommandBuilder {
    
    override val description: String = "Greet a specific player"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("greet")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes { ctx ->
                    val sender = ctx.source.sender
                    val name = StringArgumentType.getString(ctx, "name")
                    sender.sendMessage("Hello, $name!")
                    Command.SINGLE_SUCCESS
                }
            )
            .build()
    }
}

// Command with player argument  
class TeleportCommand : CommandBuilder {
    
    override val description: String = "Teleport to a player"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("teleport")
            .then(Commands.argument("target", ArgumentTypes.player())
                .requires { it.sender is Player }
                .executes { ctx ->
                    val player = ctx.source.sender as Player
                    val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                    
                    player.teleport(target.location)
                    player.sendMessage("Teleported to ${target.name}!")
                    Command.SINGLE_SUCCESS
                }
            )
            .build()
    }
}
```

## Argument Types

### Built-in Argument Types

KPaper leverages Paper's ArgumentTypes for command arguments with automatic validation:

```kotlin
import com.mojang.brigadier.arguments.*
import io.papermc.paper.command.brigadier.argument.ArgumentTypes

class AdminCommand : CommandBuilder {
    
    override val description: String = "Admin command with various arguments"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("admin")
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                        .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.1, 10.0))
                            .then(Commands.argument("force", BoolArgumentType.bool())
                                .then(Commands.argument("world", ArgumentTypes.world())
                                    .then(Commands.argument("item", ArgumentTypes.itemStack())
                                        .executes { ctx ->
                                            val sender = ctx.source.sender
                                            val message = StringArgumentType.getString(ctx, "message")
                                            val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                                            val amount = IntegerArgumentType.getInteger(ctx, "amount")
                                            val multiplier = DoubleArgumentType.getDouble(ctx, "multiplier")
                                            val force = BoolArgumentType.getBool(ctx, "force")
                                            val world = ArgumentTypes.world().parse(ctx.input).resolve(ctx.source)
                                            val itemStack = ArgumentTypes.itemStack().parse(ctx.input).resolve(ctx.source)
                                            
                                            // Use the arguments...
                                            sender.sendMessage("Processing admin command with all arguments")
                                            Command.SINGLE_SUCCESS
                                        }
                                    )
                                )
                            )
                        )
                    )
                )
            )
            .build()
    }
}
```

### Optional Arguments with Defaults

```kotlin
class GiveCommand : CommandBuilder {
    
    override val description: String = "Give items to players"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("give")
            .then(Commands.argument("item", ArgumentTypes.itemStack())
                .executes { ctx ->
                    // Default: give to sender, amount 1
                    val sender = ctx.source.sender as Player
                    val itemStack = ArgumentTypes.itemStack().parse(ctx.input).resolve(ctx.source)
                    
                    sender.inventory.addItem(itemStack)
                    sender.sendMessage("Gave ${itemStack.amount}x ${itemStack.type.name} to yourself")
                    Command.SINGLE_SUCCESS
                }
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes { ctx ->
                        // Give to specific player, amount 1
                        val sender = ctx.source.sender
                        val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                        val itemStack = ArgumentTypes.itemStack().parse(ctx.input).resolve(ctx.source)
                        
                        target.inventory.addItem(itemStack)
                        sender.sendMessage("Gave ${itemStack.amount}x ${itemStack.type.name} to ${target.name}")
                        Command.SINGLE_SUCCESS
                    }
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes { ctx ->
                            // Give specific amount to specific player
                            val sender = ctx.source.sender
                            val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                            val baseItem = ArgumentTypes.itemStack().parse(ctx.input).resolve(ctx.source)
                            val amount = IntegerArgumentType.getInteger(ctx, "amount")
                            
                            val itemStack = baseItem.clone()
                            itemStack.amount = amount
                            
                            target.inventory.addItem(itemStack)
                            sender.sendMessage("Gave ${amount}x ${itemStack.type.name} to ${target.name}")
                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
            .build()
    }
}
```

## Sub-Commands

### Basic Sub-Commands

```kotlin
class EconomyCommand : CommandBuilder {
    
    override val description: String = "Economy management commands"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("economy")
            // /economy balance [player]
            .then(Commands.literal("balance")
                .executes { ctx ->
                    val sender = ctx.source.sender as Player
                    val balance = economyAPI.getBalance(sender)
                    sender.sendMessage("Your balance: $$balance")
                    Command.SINGLE_SUCCESS
                }
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes { ctx ->
                        val sender = ctx.source.sender
                        val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                        val balance = economyAPI.getBalance(target)
                        sender.sendMessage("${target.name}'s balance: $$balance")
                        Command.SINGLE_SUCCESS
                    }
                )
            )
            // /economy pay <player> <amount>
            .then(Commands.literal("pay")
                .requires { it.sender is Player }
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes { ctx ->
                            val sender = ctx.source.sender as Player
                            val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                            val amount = DoubleArgumentType.getDouble(ctx, "amount")
                            
                            // Payment logic...
                            economyAPI.transfer(sender, target, amount)
                            sender.sendMessage("Paid $$amount to ${target.name}")
                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
            .build()
    }
}
```

### Nested Sub-Commands

```kotlin
class AdminCommand : CommandBuilder {
    
    override val description: String = "Administration commands"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("admin")
            .requires { it.sender.hasPermission("admin.use") }
            
            // /admin player <subcommand>
            .then(Commands.literal("player")
                
                // /admin player ban <player> [reason]
                .then(Commands.literal("ban")
                    .then(Commands.argument("target", ArgumentTypes.player())
                        .executes { ctx ->
                            val sender = ctx.source.sender
                            val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                            val reason = "Banned by admin"
                            
                            target.ban(reason, sender.name)
                            sender.sendMessage("Banned ${target.name} for: $reason")
                            Command.SINGLE_SUCCESS
                        }
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val sender = ctx.source.sender
                                val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                                val reason = StringArgumentType.getString(ctx, "reason")
                                
                                target.ban(reason, sender.name)
                                sender.sendMessage("Banned ${target.name} for: $reason")
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
                )
                
                // /admin player unban <player>
                .then(Commands.literal("unban")
                    .then(Commands.argument("playerName", StringArgumentType.word())
                        .executes { ctx ->
                            val sender = ctx.source.sender
                            val playerName = StringArgumentType.getString(ctx, "playerName")
                            val offlinePlayer = sender.server.getOfflinePlayer(playerName)
                            
                            offlinePlayer.banList?.pardon(offlinePlayer)
                            sender.sendMessage("Unbanned $playerName")
                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
            
            // /admin server <subcommand>
            .then(Commands.literal("server")
                
                // /admin server restart [delay]
                .then(Commands.literal("restart")
                    .executes { ctx ->
                        val sender = ctx.source.sender
                        val delay = 10
                        scheduleServerRestart(delay)
                        sender.sendMessage("Server restart scheduled in $delay seconds")
                        Command.SINGLE_SUCCESS
                    }
                    .then(Commands.argument("delay", IntegerArgumentType.integer(1))
                        .executes { ctx ->
                            val sender = ctx.source.sender
                            val delay = IntegerArgumentType.getInteger(ctx, "delay")
                            scheduleServerRestart(delay)
                            sender.sendMessage("Server restart scheduled in $delay seconds")
                            Command.SINGLE_SUCCESS
                        }
                    )
                )
            )
            .build()
    }
}
```

## Command Features

### Permissions and Requirements

```kotlin
class TeleportCommand : CommandBuilder {
    
    override val description: String = "Teleportation commands"
    override val aliases: List<String> = listOf("tp", "tele")
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("teleport")
            .requires { it.sender.hasPermission("tp.use") && it.sender is Player }
            
            .then(Commands.literal("player")
                .requires { it.sender.hasPermission("tp.player") }
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                        
                        player.teleport(target.location)
                        player.sendMessage("Teleported to ${target.name}!")
                        Command.SINGLE_SUCCESS
                    }
                )
            )
            
            .then(Commands.literal("location")
                .requires { 
                    it.sender.hasPermission("tp.location") && 
                    it.sender is Player && 
                    it.sender.hasPermission("tp.admin") 
                }
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                            .executes { ctx ->
                                val player = ctx.source.sender as Player
                                val x = DoubleArgumentType.getDouble(ctx, "x")
                                val y = DoubleArgumentType.getDouble(ctx, "y") 
                                val z = DoubleArgumentType.getDouble(ctx, "z")
                                
                                val location = Location(player.world, x, y, z)
                                player.teleport(location)
                                player.sendMessage("Teleported to $x, $y, $z")
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
                )
            )
            .build()
    }
}
```

### Command Aliases

Command aliases are handled through the `aliases` property:

```kotlin
class GameModeCommand : CommandBuilder {
    
    override val description: String = "Change game mode"
    override val aliases: List<String> = listOf("gm", "mode")
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("gamemode")
            .then(Commands.argument("mode", StringArgumentType.word())
                .suggests { _, builder ->
                    listOf("survival", "creative", "adventure", "spectator", "s", "c", "a", "sp", "0", "1", "2", "3")
                        .forEach { builder.suggest(it) }
                    builder.buildFuture()
                }
                .executes { ctx ->
                    val sender = ctx.source.sender as Player
                    val modeInput = StringArgumentType.getString(ctx, "mode").lowercase()
                    
                    val mode = when (modeInput) {
                        "survival", "s", "0" -> GameMode.SURVIVAL
                        "creative", "c", "1" -> GameMode.CREATIVE
                        "adventure", "a", "2" -> GameMode.ADVENTURE
                        "spectator", "sp", "3" -> GameMode.SPECTATOR
                        else -> {
                            sender.sendMessage("Invalid game mode!")
                            return@executes Command.SINGLE_SUCCESS
                        }
                    }
                    
                    sender.gameMode = mode
                    sender.sendMessage("Set your game mode to ${mode.name}")
                    Command.SINGLE_SUCCESS
                }
                .then(Commands.argument("target", ArgumentTypes.player())
                    .requires { it.sender.hasPermission("gamemode.others") }
                    .executes { ctx ->
                        val sender = ctx.source.sender
                        val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                        val modeInput = StringArgumentType.getString(ctx, "mode").lowercase()
                        
                        val mode = when (modeInput) {
                            "survival", "s", "0" -> GameMode.SURVIVAL
                            "creative", "c", "1" -> GameMode.CREATIVE
                            "adventure", "a", "2" -> GameMode.ADVENTURE
                            "spectator", "sp", "3" -> GameMode.SPECTATOR
                            else -> {
                                sender.sendMessage("Invalid game mode!")
                                return@executes Command.SINGLE_SUCCESS
                            }
                        }
                        
                        target.gameMode = mode
                        sender.sendMessage("Set ${target.name}'s game mode to ${mode.name}")
                        Command.SINGLE_SUCCESS
                    }
                )
            )
            .build()
    }
}
```

### Tab Completion

Tab completion is provided through Brigadier's suggestion system:

```kotlin
class TeamCommand : CommandBuilder {
    
    override val description: String = "Team management commands"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("team")
            .then(Commands.argument("team", StringArgumentType.word())
                .suggests { _, builder ->
                    getAvailableTeams().forEach { team ->
                        builder.suggest(team)
                    }
                    builder.buildFuture()
                }
                .executes { ctx ->
                    val sender = ctx.source.sender
                    val teamName = StringArgumentType.getString(ctx, "team")
                    
                    if (!isValidTeam(teamName)) {
                        sender.sendMessage("Invalid team: $teamName")
                        return@executes Command.SINGLE_SUCCESS
                    }
                    
                    // Team command logic...
                    sender.sendMessage("Selected team: $teamName")
                    Command.SINGLE_SUCCESS
                }
            )
            .build()
    }
    
    private fun getAvailableTeams(): List<String> {
        return listOf("red", "blue", "green", "yellow")
    }
    
    private fun isValidTeam(teamName: String): Boolean {
        return getAvailableTeams().contains(teamName.lowercase())
    }
}
```

Built-in argument types automatically provide appropriate tab completion:

```kotlin
class GiveCommand : CommandBuilder {
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("give")
            .then(Commands.argument("target", ArgumentTypes.player()) // Auto-completes player names
                .then(Commands.argument("item", ArgumentTypes.itemStack()) // Auto-completes items
                    .executes { ctx ->
                        // Command logic...
                        Command.SINGLE_SUCCESS
                    }
                )
            )
            .build()
    }
}
```

## Command Registration

### Using Paper's Lifecycle System

Commands should be registered during the `COMMANDS` lifecycle event:

```kotlin
// Command Bootstrapper
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

class CommandBootstrapper : PluginBootstrap {
    
    override fun bootstrap(context: BootstrapContext) {
        val manager = context.lifecycleManager
        
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            // Register individual commands
            val helloCommand = HelloCommand()
            event.registrar().register(
                helloCommand.register(),
                helloCommand.description,
                helloCommand.aliases
            )
            
            val teleportCommand = TeleportCommand()
            event.registrar().register(
                teleportCommand.register(),
                teleportCommand.description,
                teleportCommand.aliases
            )
        }
    }
}
```

### Bulk Registration with Reflection

For larger projects, you can use reflection to auto-register commands:

```kotlin
import com.google.common.reflect.ClassPath
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object CommandRegistration {
    
    fun registerCommands(commands: Commands) {
        val commandClasses = loadCommandClasses("com.yourplugin.commands")
        
        commandClasses.forEach { clazz ->
            try {
                val command = clazz.primaryConstructor?.call() as CommandBuilder
                
                commands.register(
                    command.register(),
                    command.description,
                    command.aliases
                )
                
                logger.info("Registered command: ${clazz.simpleName}")
            } catch (e: Exception) {
                cc.modlabs.kpaper.main.PluginInstance.logger.log(java.util.logging.Level.SEVERE, "Failed to register command: ${clazz.simpleName}", e)
            }
        }
        
        logger.info("Registered ${commandClasses.size} commands")
    }
    
    private fun loadCommandClasses(packageName: String): List<KClass<out CommandBuilder>> {
        val classLoader = this.javaClass.classLoader
        val allClasses = ClassPath.from(classLoader).allClasses
        val commandClasses = mutableListOf<KClass<out CommandBuilder>>()
        
        for (classInfo in allClasses) {
            if (classInfo.packageName.startsWith(packageName) && !classInfo.name.contains('$')) {
                try {
                    val loadedClass = classInfo.load().kotlin
                    if (CommandBuilder::class.isInstance(loadedClass.javaObjectType.getDeclaredConstructor().newInstance())) {
                        commandClasses.add(loadedClass as KClass<out CommandBuilder>)
                    }
                } catch (_: Exception) {
                    // Ignore classes that can't be instantiated
                }
            }
        }
        
        return commandClasses
    }
}
```

## Advanced Features

### Command Cooldowns

```kotlin
class CooldownManager {
    private val cooldowns = mutableMapOf<UUID, MutableMap<String, Long>>()
    
    fun setCooldown(player: Player, command: String, seconds: Int) {
        val playerCooldowns = cooldowns.computeIfAbsent(player.uniqueId) { mutableMapOf() }
        playerCooldowns[command] = System.currentTimeMillis() + (seconds * 1000)
    }
    
    fun getCooldown(player: Player, command: String): Long {
        val playerCooldowns = cooldowns[player.uniqueId] ?: return 0
        val cooldownEnd = playerCooldowns[command] ?: return 0
        return maxOf(0, cooldownEnd - System.currentTimeMillis())
    }
    
    fun hasCooldown(player: Player, command: String): Boolean {
        return getCooldown(player, command) > 0
    }
}

class HealCommand : CommandBuilder {
    
    private val cooldownManager = CooldownManager()
    override val description: String = "Heal yourself"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("heal")
            .requires { sender ->
                val player = sender.sender as? Player ?: return@requires false
                
                if (cooldownManager.hasCooldown(player, "heal")) {
                    val remaining = cooldownManager.getCooldown(player, "heal") / 1000
                    player.sendMessage("§cHeal is on cooldown for ${remaining} seconds!")
                    false
                } else {
                    true
                }
            }
            .executes { ctx ->
                val player = ctx.source.sender as Player
                player.health = player.maxHealth
                player.sendMessage("§aYou have been healed!")
                
                // Set 30 second cooldown
                cooldownManager.setCooldown(player, "heal", 30)
                Command.SINGLE_SUCCESS
            }
            .build()
    }
}
```

### Command Usage Tracking

```kotlin
class CommandStats {
    private val usage = mutableMapOf<String, Int>()
    
    fun recordUsage(command: String) {
        usage[command] = usage.getOrDefault(command, 0) + 1
    }
    
    fun getUsage(command: String): Int = usage.getOrDefault(command, 0)
    fun getAllStats(): Map<String, Int> = usage.toMap()
}

class StatsCommand : CommandBuilder {
    
    override val description: String = "View command usage statistics"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("stats")
            .requires { it.sender.hasPermission("admin.stats") }
            .executes { ctx ->
                val sender = ctx.source.sender
                sender.sendMessage("§6Command Usage Statistics:")
                
                stats.getAllStats().entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .forEach { (command, count) ->
                        sender.sendMessage("§e$command: §f$count uses")
                    }
                    
                Command.SINGLE_SUCCESS
            }
            .build()
    }
}

// Add usage tracking to other commands
class TeleportCommandWithStats : CommandBuilder {
    
    override val description: String = "Teleport to a player"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("teleport")
            .then(Commands.argument("target", ArgumentTypes.player())
                .executes { ctx ->
                    stats.recordUsage("teleport")
                    // Command logic...
                    Command.SINGLE_SUCCESS
                }
            )
            .build()
    }
}
```

### Dynamic Command Registration

```kotlin
class DynamicCommands {
    
    fun registerWarpCommands(commands: Commands, warps: List<Warp>) {
        warps.forEach { warp ->
            val warpCommand = object : CommandBuilder {
                override val description: String = "Teleport to ${warp.name}"
                
                override fun register(): LiteralCommandNode<CommandSourceStack> {
                    return Commands.literal("warp-${warp.name}")
                        .requires { it.sender is Player }
                        .executes { ctx ->
                            val player = ctx.source.sender as Player
                            player.teleport(warp.location)
                            player.sendMessage("Teleported to ${warp.name}!")
                            Command.SINGLE_SUCCESS
                        }
                        .build()
                }
            }
            
            commands.register(
                warpCommand.register(),
                warpCommand.description,
                warpCommand.aliases
            )
        }
    }
    
    fun registerKitCommands(commands: Commands, kits: List<Kit>) {
        kits.forEach { kit ->
            val kitCommand = object : CommandBuilder {
                override val description: String = "Get the ${kit.name} kit"
                
                override fun register(): LiteralCommandNode<CommandSourceStack> {
                    return Commands.literal("kit-${kit.name}")
                        .requires { 
                            it.sender is Player && 
                            it.sender.hasPermission("kits.${kit.name}") 
                        }
                        .executes { ctx ->
                            val player = ctx.source.sender as Player
                            giveKit(player, kit)
                            Command.SINGLE_SUCCESS
                        }
                        .build()
                }
            }
            
            commands.register(
                kitCommand.register(),
                kitCommand.description,
                kitCommand.aliases
            )
        }
    }
}

// Register commands dynamically in the lifecycle event
manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
    val dynamicCommands = DynamicCommands()
    dynamicCommands.registerWarpCommands(event.registrar(), loadWarps())
    dynamicCommands.registerKitCommands(event.registrar(), loadKits())
}
```

## Error Handling

### Validation and Error Messages

```kotlin
class EconomyTransferCommand : CommandBuilder {
    
    override val description: String = "Transfer money between players"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("economy")
            .then(Commands.literal("transfer")
                .then(Commands.argument("from", ArgumentTypes.player())
                    .then(Commands.argument("to", ArgumentTypes.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes { ctx ->
                                val sender = ctx.source.sender
                                val from = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                                val to = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                                val amount = DoubleArgumentType.getDouble(ctx, "amount")
                                
                                // Validation
                                if (from == to) {
                                    sender.sendMessage("§cCannot transfer money to yourself!")
                                    return@executes Command.SINGLE_SUCCESS
                                }
                                
                                val fromBalance = economyAPI.getBalance(from)
                                if (fromBalance < amount) {
                                    sender.sendMessage("§c${from.name} doesn't have enough money! (Has: $$fromBalance, Needs: $$amount)")
                                    return@executes Command.SINGLE_SUCCESS
                                }
                                
                                // Execute transfer
                                try {
                                    economyAPI.transfer(from, to, amount)
                                    sender.sendMessage("§aTransferred $$amount from ${from.name} to ${to.name}")
                                    
                                    // Notify players
                                    from.sendMessage("§c-$$amount (transferred to ${to.name})")
                                    to.sendMessage("§a+$$amount (from ${from.name})")
                                    
                                } catch (e: Exception) {
                                    sender.sendMessage("§cTransfer failed: ${e.message}")
                                    cc.modlabs.kpaper.main.PluginInstance.logger.log(java.util.logging.Level.SEVERE, "Economy transfer failed", e)
                                }
                                
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
                )
            )
            .build()
    }
}
```

### Command Exception Handling

```kotlin
// Create a base class for safe command execution
abstract class SafeCommandBuilder : CommandBuilder {
    
    protected fun safeExecute(
        ctx: CommandContext<CommandSourceStack>,
        handler: (CommandContext<CommandSourceStack>) -> Int
    ): Int {
        return try {
            handler(ctx)
        } catch (e: PlayerNotFoundException) {
            ctx.source.sender.sendMessage("§cPlayer not found: ${e.playerName}")
            Command.SINGLE_SUCCESS
        } catch (e: InsufficientPermissionException) {
            ctx.source.sender.sendMessage("§cYou don't have permission to do that!")
            Command.SINGLE_SUCCESS
        } catch (e: InvalidArgumentException) {
            ctx.source.sender.sendMessage("§cInvalid argument: ${e.message}")
            Command.SINGLE_SUCCESS
        } catch (e: Exception) {
            ctx.source.sender.sendMessage("§cAn error occurred while executing the command.")
            cc.modlabs.kpaper.main.PluginInstance.logger.log(java.util.logging.Level.SEVERE, "Command execution failed", e)
            Command.SINGLE_SUCCESS
        }
    }
}

// Usage
class RiskyCommand : SafeCommandBuilder() {
    
    override val description: String = "A command that might fail"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("risky-command")
            .executes { ctx ->
                safeExecute(ctx) {
                    // Code that might throw exceptions
                    performRiskyOperation()
                    ctx.source.sender.sendMessage("Operation completed successfully!")
                    Command.SINGLE_SUCCESS
                }
            }
            .build()
    }
    
    private fun performRiskyOperation() {
        // Risky code here
    }
}
```

## Best Practices

### 1. Use Descriptive Names and Help

```kotlin
class PlayerManagementCommand : CommandBuilder {
    
    override val description: String = "Comprehensive player management system"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("player-management")
            .executes { ctx ->
                val sender = ctx.source.sender
                sender.sendMessage("§6Player Management Commands:")
                sender.sendMessage("§e/player-management ban <player> [reason] - Ban a player")
                sender.sendMessage("§e/player-management mute <player> <time> [reason] - Mute a player")  
                sender.sendMessage("§e/player-management info <player> - Get player information")
                Command.SINGLE_SUCCESS
            }
            // Add sub-commands here...
            .build()
    }
}
```

### 2. Validate Input Early

```kotlin
class SetSpawnCommand : CommandBuilder {
    
    override val description: String = "Set the world spawn location"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("set-spawn")
            .requires { sender ->
                val player = sender.sender as? Player ?: return@requires false
                val world = player.world
                
                if (world.name == "world_nether" || world.name == "world_the_end") {
                    player.sendMessage("§cCannot set spawn in ${world.name}!")
                    false
                } else {
                    true
                }
            }
            .executes { ctx ->
                val player = ctx.source.sender as Player
                player.world.spawnLocation = player.location
                player.sendMessage("§aSpawn location set!")
                Command.SINGLE_SUCCESS
            }
            .build()
    }
}
```

### 3. Provide Helpful Feedback

```kotlin
class TeleportCommandWithFeedback : CommandBuilder {
    
    override val description: String = "Teleport to another player"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("teleport")
            .then(Commands.argument("target", ArgumentTypes.player())
                .requires { it.sender is Player }
                .executes { ctx ->
                    val player = ctx.source.sender as Player
                    val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                    
                    // Pre-teleport validation
                    if (target.world != player.world) {
                        player.sendMessage("§e${target.name} is in a different world. Teleporting anyway...")
                    }
                    
                    val oldLocation = player.location.clone()
                    player.teleport(target.location)
                    
                    // Success feedback with context
                    val loc = target.location
                    player.sendMessage("§aTeleported to ${target.name} at ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}")
                    
                    // Allow quick return
                    player.sendMessage("§7Use /back to return to your previous location")
                    storeLocation(player, oldLocation)
                    
                    Command.SINGLE_SUCCESS
                }
            )
            .build()
    }
    
    private fun storeLocation(player: Player, location: Location) {
        // Store location for /back command
    }
}
```

### 4. Structure Complex Commands

For commands with many sub-commands, consider splitting them into separate classes:

```kotlin
// Base admin command
class AdminCommand : CommandBuilder {
    
    override val description: String = "Administrative commands"
    
    override fun register(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("admin")
            .requires { it.sender.hasPermission("admin.use") }
            .executes { ctx ->
                ctx.source.sender.sendMessage("§6Available admin commands: player, server, world")
                Command.SINGLE_SUCCESS
            }
            .then(PlayerAdminSubcommand().buildSubcommand())
            .then(ServerAdminSubcommand().buildSubcommand())
            .then(WorldAdminSubcommand().buildSubcommand())
            .build()
    }
}

// Separate subcommand classes
class PlayerAdminSubcommand {
    
    fun buildSubcommand(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("player")
            .then(Commands.literal("ban")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes { ctx ->
                        // Ban logic
                        Command.SINGLE_SUCCESS
                    }
                )
            )
            .then(Commands.literal("unban")
                .then(Commands.argument("playerName", StringArgumentType.word())
                    .executes { ctx ->
                        // Unban logic  
                        Command.SINGLE_SUCCESS
                    }
                )
            )
    }
}
```

## Related Topics

- [Events](events.md) - Handling command-related events
- [Inventory](inventory.md) - Creating command-triggered GUIs
- [Utilities](utilities.md) - Helper functions for commands
- [Examples](../examples/common-patterns.md) - Command patterns and examples