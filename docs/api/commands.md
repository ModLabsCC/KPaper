# Command Framework

KPaper's command framework provides a powerful, fluent API for creating commands with automatic argument parsing, validation, and sub-command support.

## Basic Command Creation

### Simple Commands

The easiest way to create commands is using the `CommandBuilder`:

```kotlin
import cc.modlabs.kpaper.command.CommandBuilder

class MyPlugin : KPlugin() {
    override fun startup() {
        // Basic command
        CommandBuilder("hello")
            .description("Say hello to the world")
            .execute { sender, args ->
                sender.sendMessage("Hello, World!")
            }
            .register()
    }
}
```

### Command with Arguments

```kotlin
// Command with string argument
CommandBuilder("greet")
    .description("Greet a specific player")
    .argument(stringArgument("name"))
    .execute { sender, args ->
        val name = args.getString("name")
        sender.sendMessage("Hello, $name!")
    }
    .register()

// Command with player argument
CommandBuilder("teleport")
    .description("Teleport to a player")
    .argument(playerArgument("target"))
    .playerOnly(true)
    .execute { sender, args ->
        val player = sender as Player
        val target = args.getPlayer("target")
        
        player.teleport(target.location)
        player.sendMessage("Teleported to ${target.name}!")
    }
    .register()
```

## Argument Types

### Built-in Argument Types

KPaper provides many built-in argument types with automatic validation:

```kotlin
import cc.modlabs.kpaper.command.arguments.*

CommandBuilder("admin")
    .description("Admin command with various arguments")
    
    // String arguments
    .argument(stringArgument("message"))
    
    // Player arguments (with online validation)
    .argument(playerArgument("target"))
    
    // Numeric arguments
    .argument(intArgument("amount", min = 1, max = 100))
    .argument(doubleArgument("multiplier", min = 0.1, max = 10.0))
    
    // Boolean arguments
    .argument(booleanArgument("force", optional = true))
    
    // World arguments
    .argument(worldArgument("world"))
    
    // Material arguments
    .argument(materialArgument("item"))
    
    // Location arguments
    .argument(locationArgument("position"))
    
    .execute { sender, args ->
        val message = args.getString("message")
        val target = args.getPlayer("target")
        val amount = args.getInt("amount")
        val multiplier = args.getDouble("multiplier")
        val force = args.getBoolean("force") ?: false
        val world = args.getWorld("world")
        val material = args.getMaterial("item")
        val location = args.getLocation("position")
        
        // Use the arguments...
    }
    .register()
```

### Optional Arguments

```kotlin
CommandBuilder("give")
    .description("Give items to players")
    .argument(playerArgument("target", optional = true)) // Defaults to sender
    .argument(materialArgument("item"))
    .argument(intArgument("amount", optional = true, default = 1))
    .execute { sender, args ->
        val target = args.getPlayerOrSelf("target", sender)
        val material = args.getMaterial("item")
        val amount = args.getIntOrDefault("amount", 1)
        
        val item = ItemStack(material, amount)
        target.inventory.addItem(item)
        
        sender.sendMessage("Gave ${amount}x ${material.name} to ${target.name}")
    }
    .register()
```

### Custom Argument Types

Create your own argument types for complex validation:

```kotlin
// Custom argument for economy amounts
fun economyAmountArgument(name: String, min: Double = 0.01): ArgumentType<Double> {
    return object : ArgumentType<Double> {
        override val name = name
        override val optional = false
        
        override fun parse(input: String): Double? {
            val amount = input.toDoubleOrNull() ?: return null
            return if (amount >= min) amount else null
        }
        
        override fun complete(input: String): List<String> {
            return listOf("10", "100", "1000").filter { it.startsWith(input, ignoreCase = true) }
        }
        
        override fun getUsage(): String = "<amount>"
        override fun getDescription(): String = "Amount of money (minimum $min)"
    }
}

// Usage
CommandBuilder("pay")
    .argument(playerArgument("target"))
    .argument(economyAmountArgument("amount", min = 1.0))
    .execute { sender, args ->
        val target = args.getPlayer("target")
        val amount = args.get<Double>("amount")!!
        
        // Process payment...
    }
    .register()
```

## Sub-Commands

### Basic Sub-Commands

```kotlin
CommandBuilder("economy")
    .description("Economy management commands")
    
    // /economy balance [player]
    .subcommand("balance")
    .description("Check player balance")
    .argument(playerArgument("target", optional = true))
    .execute { sender, args ->
        val target = args.getPlayerOrSelf("target", sender)
        val balance = economyAPI.getBalance(target)
        sender.sendMessage("${target.name}'s balance: $${balance}")
    }
    
    // /economy pay <player> <amount>
    .subcommand("pay")
    .description("Pay another player")
    .argument(playerArgument("target"))
    .argument(economyAmountArgument("amount"))
    .playerOnly(true)
    .execute { sender, args ->
        // Payment logic...
    }
    
    .register()
```

### Nested Sub-Commands

```kotlin
CommandBuilder("admin")
    .description("Administration commands")
    .permission("admin.use")
    
    // /admin player <subcommand>
    .subcommand("player")
    .description("Player management")
    
        // /admin player ban <player> [reason]
        .subcommand("ban")
        .argument(playerArgument("target"))
        .argument(stringArgument("reason", optional = true))
        .execute { sender, args ->
            val target = args.getPlayer("target")
            val reason = args.getStringOrDefault("reason", "Banned by admin")
            
            target.ban(reason, sender.name)
            sender.sendMessage("Banned ${target.name} for: $reason")
        }
        
        // /admin player unban <player>
        .subcommand("unban")
        .argument(stringArgument("playerName"))
        .execute { sender, args ->
            val playerName = args.getString("playerName")
            val offlinePlayer = server.getOfflinePlayer(playerName)
            
            offlinePlayer.banList?.pardon(offlinePlayer)
            sender.sendMessage("Unbanned $playerName")
        }
    
    // /admin server <subcommand>
    .subcommand("server")
    .description("Server management")
    
        // /admin server restart [delay]
        .subcommand("restart")
        .argument(intArgument("delay", optional = true, default = 10))
        .execute { sender, args ->
            val delay = args.getIntOrDefault("delay", 10)
            scheduleServerRestart(delay)
            sender.sendMessage("Server restart scheduled in ${delay} seconds")
        }
    
    .register()
```

## Command Features

### Permissions and Requirements

```kotlin
CommandBuilder("teleport")
    .description("Teleportation commands")
    .permission("tp.use") // Base permission
    .playerOnly(true) // Only players can use
    
    .subcommand("player")
    .permission("tp.player") // Additional permission for sub-command
    .argument(playerArgument("target"))
    .execute { sender, args ->
        // Teleport logic...
    }
    
    .subcommand("location")
    .permission("tp.location")
    .requirement { sender -> 
        sender is Player && sender.hasPermission("tp.admin")
    } // Custom requirement check
    .argument(locationArgument("position"))
    .execute { sender, args ->
        // Teleport to location...
    }
    
    .register()
```

### Command Aliases

```kotlin
CommandBuilder("gamemode")
    .description("Change game mode")
    .aliases("gm", "mode") // Alternative command names
    .argument(stringArgument("mode"))
    .argument(playerArgument("target", optional = true))
    .execute { sender, args ->
        val mode = when (args.getString("mode").lowercase()) {
            "survival", "s", "0" -> GameMode.SURVIVAL
            "creative", "c", "1" -> GameMode.CREATIVE
            "adventure", "a", "2" -> GameMode.ADVENTURE
            "spectator", "sp", "3" -> GameMode.SPECTATOR
            else -> {
                sender.sendMessage("Invalid game mode!")
                return@execute
            }
        }
        
        val target = args.getPlayerOrSelf("target", sender)
        target.gameMode = mode
        sender.sendMessage("Set ${target.name}'s game mode to ${mode.name}")
    }
    .register()
```

### Tab Completion

KPaper automatically provides tab completion for built-in argument types:

```kotlin
// Tab completion is automatic for:
CommandBuilder("give")
    .argument(playerArgument("target")) // Completes online player names
    .argument(materialArgument("item")) // Completes material names
    .argument(worldArgument("world")) // Completes world names
    .register()

// Custom completion
CommandBuilder("team")
    .argument(object : ArgumentType<String> {
        override val name = "team"
        override val optional = false
        
        override fun parse(input: String): String? {
            return if (isValidTeam(input)) input else null
        }
        
        override fun complete(input: String): List<String> {
            return getAvailableTeams().filter { 
                it.startsWith(input, ignoreCase = true) 
            }
        }
        
        override fun getUsage() = "<team>"
        override fun getDescription() = "Team name"
    })
    .execute { sender, args ->
        // Team command logic...
    }
    .register()
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

val cooldownManager = CooldownManager()

CommandBuilder("heal")
    .description("Heal yourself")
    .playerOnly(true)
    .requirement { sender ->
        val player = sender as Player
        if (cooldownManager.hasCooldown(player, "heal")) {
            val remaining = cooldownManager.getCooldown(player, "heal") / 1000
            player.sendMessage("&cHeal is on cooldown for ${remaining} seconds!")
            false
        } else {
            true
        }
    }
    .execute { sender, _ ->
        val player = sender as Player
        player.health = player.maxHealth
        player.sendMessage("&aYou have been healed!")
        
        // Set 30 second cooldown
        cooldownManager.setCooldown(player, "heal", 30)
    }
    .register()
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

val stats = CommandStats()

CommandBuilder("stats")
    .description("View command usage statistics")
    .permission("admin.stats")
    .execute { sender, _ ->
        sender.sendMessage("&6Command Usage Statistics:")
        stats.getAllStats().entries
            .sortedByDescending { it.value }
            .take(10)
            .forEach { (command, count) ->
                sender.sendMessage("&e$command: &f$count uses")
            }
    }
    .register()

// Add usage tracking to other commands
CommandBuilder("teleport")
    .execute { sender, args ->
        stats.recordUsage("teleport")
        // Command logic...
    }
    .register()
```

### Dynamic Command Registration

```kotlin
class DynamicCommands {
    
    fun registerWarpCommands(warps: List<Warp>) {
        warps.forEach { warp ->
            CommandBuilder("warp-${warp.name}")
                .description("Teleport to ${warp.name}")
                .playerOnly(true)
                .execute { sender, _ ->
                    val player = sender as Player
                    player.teleport(warp.location)
                    player.sendMessage("Teleported to ${warp.name}!")
                }
                .register()
        }
    }
    
    fun registerKitCommands(kits: List<Kit>) {
        kits.forEach { kit ->
            CommandBuilder("kit-${kit.name}")
                .description("Get the ${kit.name} kit")
                .permission("kits.${kit.name}")
                .playerOnly(true)
                .execute { sender, _ ->
                    val player = sender as Player
                    giveKit(player, kit)
                }
                .register()
        }
    }
}

// Register commands dynamically
val dynamicCommands = DynamicCommands()
dynamicCommands.registerWarpCommands(loadWarps())
dynamicCommands.registerKitCommands(loadKits())
```

## Error Handling

### Validation and Error Messages

```kotlin
CommandBuilder("economy")
    .subcommand("transfer")
    .argument(playerArgument("from"))
    .argument(playerArgument("to"))
    .argument(economyAmountArgument("amount"))
    .execute { sender, args ->
        val from = args.getPlayer("from")
        val to = args.getPlayer("to")
        val amount = args.get<Double>("amount")!!
        
        // Validation
        if (from == to) {
            sender.sendMessage("&cCannot transfer money to yourself!")
            return@execute
        }
        
        val fromBalance = economyAPI.getBalance(from)
        if (fromBalance < amount) {
            sender.sendMessage("&c${from.name} doesn't have enough money! (Has: $$fromBalance, Needs: $$amount)")
            return@execute
        }
        
        // Execute transfer
        try {
            economyAPI.transfer(from, to, amount)
            sender.sendMessage("&aTransferred $$amount from ${from.name} to ${to.name}")
            
            // Notify players
            from.sendMessage("&c-$$amount (transferred to ${to.name})")
            to.sendMessage("&a+$$amount (from ${from.name})")
            
        } catch (e: Exception) {
            sender.sendMessage("&cTransfer failed: ${e.message}")
            logger.error("Economy transfer failed", e)
        }
    }
    .register()
```

### Command Exception Handling

```kotlin
// Global command error handler
abstract class SafeCommandBuilder(name: String) : CommandBuilder(name) {
    
    override fun execute(handler: (CommandSender, CommandArguments) -> Unit): CommandBuilder {
        return super.execute { sender, args ->
            try {
                handler(sender, args)
            } catch (e: PlayerNotFoundException) {
                sender.sendMessage("&cPlayer not found: ${e.playerName}")
            } catch (e: InsufficientPermissionException) {
                sender.sendMessage("&cYou don't have permission to do that!")
            } catch (e: InvalidArgumentException) {
                sender.sendMessage("&cInvalid argument: ${e.message}")
            } catch (e: Exception) {
                sender.sendMessage("&cAn error occurred while executing the command.")
                logger.error("Command execution failed", e)
            }
        }
    }
}

// Usage
SafeCommandBuilder("risky-command")
    .execute { sender, args ->
        // Code that might throw exceptions
        performRiskyOperation()
    }
    .register()
```

## Best Practices

### 1. Use Descriptive Names and Help

```kotlin
CommandBuilder("player-management")
    .description("Comprehensive player management system")
    .usage("/player-management <action> <player> [options...]")
    .examples(
        "/player-management ban PlayerName Griefing",
        "/player-management mute PlayerName 10m Spamming",
        "/player-management info PlayerName"
    )
    .register()
```

### 2. Validate Input Early

```kotlin
CommandBuilder("set-spawn")
    .playerOnly(true)
    .requirement { sender ->
        val player = sender as Player
        val world = player.world
        
        if (world.name == "nether" || world.name == "the_end") {
            player.sendMessage("&cCannot set spawn in ${world.name}!")
            false
        } else {
            true
        }
    }
    .execute { sender, _ ->
        val player = sender as Player
        world.spawnLocation = player.location
        player.sendMessage("&aSpawn location set!")
    }
    .register()
```

### 3. Provide Helpful Feedback

```kotlin
CommandBuilder("teleport")
    .argument(playerArgument("target"))
    .playerOnly(true)
    .execute { sender, args ->
        val player = sender as Player
        val target = args.getPlayer("target")
        
        // Pre-teleport validation
        if (target.world != player.world) {
            player.sendMessage("&e${target.name} is in a different world. Teleporting anyway...")
        }
        
        val oldLocation = player.location
        player.teleport(target.location)
        
        // Success feedback with context
        player.sendMessage("&aTeleported to ${target.name} at ${target.location.blockX}, ${target.location.blockY}, ${target.location.blockZ}")
        
        // Allow quick return
        player.sendMessage("&7Use /back to return to your previous location")
        storeLocation(player, oldLocation)
    }
    .register()
```

## Related Topics

- [Events](events.md) - Handling command-related events
- [Inventory](inventory.md) - Creating command-triggered GUIs
- [Utilities](utilities.md) - Helper functions for commands
- [Examples](../examples/common-patterns.md) - Command patterns and examples