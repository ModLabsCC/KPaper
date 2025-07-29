# Your First KPaper Plugin

This guide walks you through creating your first plugin using KPaper, demonstrating the key features that make development faster and more enjoyable.

## Plugin Structure

We'll create a simple "Welcome" plugin that demonstrates core KPaper features:
- Player join messages
- Custom commands
- GUI interactions
- Configuration management

## Step 1: Basic Plugin Setup

Create your main plugin class:

```kotlin
package com.example.welcomeplugin

import cc.modlabs.kpaper.main.KPlugin
import cc.modlabs.kpaper.main.featureConfig

class WelcomePlugin : KPlugin() {
    
    // Configure which KPaper features to enable
    override val featureConfig = featureConfig {
        enableInventoryFeatures = true
        enableCommandFeatures = true
        enableEventFeatures = true
    }
    
    override fun startup() {
        logger.info("Welcome Plugin is starting up!")
        
        // Setup will go here
        setupEvents()
        setupCommands()
        setupGUI()
        
        logger.info("Welcome Plugin enabled successfully!")
    }
    
    override fun shutdown() {
        logger.info("Welcome Plugin is shutting down!")
    }
}
```

## Step 2: Event Handling

KPaper makes event handling much simpler with its `listen` function:

```kotlin
import cc.modlabs.kpaper.event.listen
import cc.modlabs.kpaper.extensions.sendFormattedMessage
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

private fun setupEvents() {
    // Welcome message when players join
    listen<PlayerJoinEvent> { event ->
        val player = event.player
        
        // Send a welcome message
        player.sendFormattedMessage("&aWelcome to the server, &e${player.name}&a!")
        
        // Broadcast join message to all players
        server.onlinePlayers.forEach { onlinePlayer ->
            if (onlinePlayer != player) {
                onlinePlayer.sendFormattedMessage("&7${player.name} joined the server!")
            }
        }
        
        // Give welcome items
        giveWelcomeItems(player)
    }
    
    // Farewell message when players leave
    listen<PlayerQuitEvent> { event ->
        val player = event.player
        
        // Broadcast leave message
        server.onlinePlayers.forEach { onlinePlayer ->
            onlinePlayer.sendFormattedMessage("&7${player.name} left the server!")
        }
    }
}
```

## Step 3: Command Creation

Create commands using KPaper's command builder:

```kotlin
import cc.modlabs.kpaper.command.CommandBuilder
import cc.modlabs.kpaper.command.arguments.playerArgument
import cc.modlabs.kpaper.command.arguments.stringArgument
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

private fun setupCommands() {
    // Commands are registered through Paper's lifecycle system
    // This would be done in a CommandBootstrapper class
}

// Command implementations would look like:
class WelcomeCommand : CommandBuilder {
    
    override val description = "Welcome command with GUI and admin features"
    
    override fun register() = Commands.literal("welcome")
        .requires { it.sender.hasPermission("welcomeplugin.welcome") && it.sender is Player }
        .executes { ctx ->
            val player = ctx.source.sender as Player
            player.sendFormattedMessage("§aWelcome to our server, §e${player.name}§a!")
            player.sendFormattedMessage("§7Use §f/welcome gui §7to open the welcome GUI!")
            Command.SINGLE_SUCCESS
        }
        
        // Welcome GUI sub-command
        .then(Commands.literal("gui")
            .requires { it.sender.hasPermission("welcomeplugin.gui") }
            .executes { ctx ->
                val player = ctx.source.sender as Player
                openWelcomeGUI(player)
                Command.SINGLE_SUCCESS
            }
        )
        
        // Admin sub-command to welcome specific players
        .then(Commands.literal("player")
            .requires { it.sender.hasPermission("welcomeplugin.admin") }
            .then(Commands.argument("target", ArgumentTypes.player())
                .executes { ctx ->
                    val sender = ctx.source.sender
                    val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                    
                    target.sendFormattedMessage("§aWelcome to our server, §e${target.name}§a!")
                    target.sendFormattedMessage("§7An admin has sent you a welcome message!")
                    
                    sender.sendMessage("§aWelcome message sent to ${target.name}!")
                    Command.SINGLE_SUCCESS
                }
            )
        )
        .build()
}
```

## Step 4: GUI Creation

Create an interactive GUI using KPaper's inventory system:

```kotlin
import cc.modlabs.kpaper.inventory.ItemBuilder
import cc.modlabs.kpaper.inventory.simple.simpleGUI
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

private fun openWelcomeGUI(player: Player) {
    val gui = simpleGUI("Welcome Center", 27) {
        
        // Welcome info item
        item(4, ItemBuilder(Material.NETHER_STAR)
            .name("&6Welcome to Our Server!")
            .lore(
                "&7",
                "&aServer Features:",
                "&8• &7Friendly community",
                "&8• &7Custom plugins",
                "&8• &7Regular events",
                "&7",
                "&eClick for more info!"
            )
            .build()) {
            player.closeInventory()
            player.sendFormattedMessage("&aThank you for visiting our server!")
            player.sendFormattedMessage("&7Join our Discord: &bdiscord.gg/example")
        }
        
        // Rules book
        item(10, ItemBuilder(Material.BOOK)
            .name("&cServer Rules")
            .lore(
                "&7",
                "&71. Be respectful to all players",
                "&72. No griefing or stealing", 
                "&73. No spam or advertising",
                "&74. Have fun!",
                "&7",
                "&eClick to acknowledge rules"
            )
            .build()) {
            player.sendFormattedMessage("&aThank you for reading the rules!")
            player.giveExp(10)
        }
        
        // Free starter kit
        item(16, ItemBuilder(Material.CHEST)
            .name("&aFree Starter Kit")
            .lore(
                "&7",
                "&aContains:",
                "&8• &7Basic tools",
                "&8• &7Some food",
                "&8• &7Building blocks",
                "&7",
                "&eClick to claim! &7(One time only)"
            )
            .build()) {
            if (player.hasPermission("welcomeplugin.kit.claimed")) {
                player.sendFormattedMessage("&cYou have already claimed your starter kit!")
                return@item
            }
            
            giveStarterKit(player)
            player.addAttachment(this@WelcomePlugin, "welcomeplugin.kit.claimed", true)
            player.sendFormattedMessage("&aStarter kit claimed! Check your inventory.")
        }
        
        // Close button
        item(22, ItemBuilder(Material.BARRIER)
            .name("&cClose")
            .lore("&7Click to close this menu")
            .build()) {
            player.closeInventory()
        }
    }
    
    player.openInventory(gui)
}
```

## Step 5: Helper Functions

Add utility functions to complete the plugin:

```kotlin
import cc.modlabs.kpaper.inventory.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player

private fun giveWelcomeItems(player: Player) {
    // Give a welcome book
    val welcomeBook = ItemBuilder(Material.BOOK)
        .name("&6Welcome Guide")
        .lore(
            "&7",
            "&aWelcome to our server!",
            "&7This book contains important",
            "&7information for new players.",
            "&7",
            "&eKeep this safe!"
        )
        .build()
    
    player.inventory.addItem(welcomeBook)
    player.sendFormattedMessage("&aYou received a welcome guide!")
}

private fun giveStarterKit(player: Player) {
    val items = listOf(
        ItemBuilder(Material.WOODEN_SWORD).name("&7Starter Sword").build(),
        ItemBuilder(Material.WOODEN_PICKAXE).name("&7Starter Pickaxe").build(),
        ItemBuilder(Material.WOODEN_AXE).name("&7Starter Axe").build(),
        ItemBuilder(Material.BREAD).amount(16).build(),
        ItemBuilder(Material.OAK_PLANKS).amount(32).build(),
        ItemBuilder(Material.TORCH).amount(16).build()
    )
    
    items.forEach { item ->
        player.inventory.addItem(item)
    }
}
```

## Step 6: Configuration (Optional)

Add configuration support:

```kotlin
import cc.modlabs.kpaper.file.configurationFile
import org.bukkit.configuration.file.FileConfiguration

class WelcomePlugin : KPlugin() {
    private lateinit var config: FileConfiguration
    
    override fun startup() {
        // Load configuration
        config = configurationFile("config.yml", saveDefault = true)
        
        // ... rest of startup code
    }
    
    private fun getWelcomeMessage(): String {
        return config.getString("messages.welcome", "&aWelcome to the server!")!!
    }
}
```

Create `resources/config.yml`:

```yaml
messages:
  welcome: "&aWelcome to our amazing server, &e%player%&a!"
  farewell: "&7See you later, &e%player%&7!"

features:
  starter-kit-enabled: true
  welcome-gui-enabled: true

kit:
  items:
    - "WOODEN_SWORD:1"
    - "WOODEN_PICKAXE:1"
    - "BREAD:16"
```

## Complete Plugin Code

Here's the complete plugin in one file:

```kotlin
package com.example.welcomeplugin

import cc.modlabs.kpaper.main.KPlugin
import cc.modlabs.kpaper.main.featureConfig
import cc.modlabs.kpaper.event.listen
import cc.modlabs.kpaper.extensions.sendFormattedMessage
import cc.modlabs.kpaper.command.CommandBuilder
import cc.modlabs.kpaper.inventory.ItemBuilder
import cc.modlabs.kpaper.inventory.simple.simpleGUI
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class WelcomePlugin : KPlugin() {
    
    override val featureConfig = featureConfig {
        enableInventoryFeatures = true
        enableCommandFeatures = true
        enableEventFeatures = true
    }
    
    override fun startup() {
        logger.info("Welcome Plugin starting up!")
        
        setupEvents()
        setupCommands()
        
        logger.info("Welcome Plugin enabled!")
    }
    
    private fun setupEvents() {
        listen<PlayerJoinEvent> { event ->
            val player = event.player
            player.sendFormattedMessage("&aWelcome to the server, &e${player.name}&a!")
            giveWelcomeItems(player)
        }
        
        listen<PlayerQuitEvent> { event ->
            server.broadcast(
                Component.text("${event.player.name} left the server!")
                    .color(NamedTextColor.GRAY)
            )
        }
    }
    
    private fun setupCommands() {
        // In a real implementation, this would be registered in a CommandBootstrapper
        // Example: WelcomeCommand class as shown earlier
    }
    
    private fun giveWelcomeItems(player: Player) {
        val welcomeBook = ItemBuilder(Material.BOOK)
            .name("&6Welcome Guide")
            .lore("&7Your guide to the server!")
            .build()
        
        player.inventory.addItem(welcomeBook)
    }
    
    private fun openWelcomeGUI(player: Player) {
        val gui = simpleGUI("Welcome Center", 9) {
            item(4, ItemBuilder(Material.NETHER_STAR)
                .name("&6Welcome!")
                .lore("&7Thanks for joining our server!")
                .build()) {
                player.sendFormattedMessage("&aEnjoy your stay!")
                player.closeInventory()
            }
        }
        
        player.openInventory(gui)
    }
}
```

## Testing Your Plugin

1. **Build the plugin:**
   ```bash
   ./gradlew build
   ```

2. **Copy to server:**
   - Copy the JAR from `build/libs/` to your Paper server's `plugins/` folder

3. **Test features:**
   - Join the server to see welcome messages
   - Run `/welcome` to open the GUI
   - Check console for startup messages

## Next Steps

Now that you've created your first KPaper plugin, explore more advanced features:

- [Event System](../api/events.md) - Learn about custom events and advanced handlers
- [Command Framework](../api/commands.md) - Create complex commands with validation
- [Inventory System](../api/inventory.md) - Build sophisticated GUIs and item systems
- [Plugin Development](../core/plugin-development.md) - Understand KPaper's core concepts

## Common Patterns

This example demonstrates several KPaper patterns you'll use frequently:

- **Event Handling:** Using `listen<Event>` for clean event registration
- **Command Building:** Fluent API for creating commands with arguments
- **GUI Creation:** Simple inventory GUIs with click handlers
- **Item Building:** Fluent API for creating custom items
- **Message Formatting:** Built-in color code support
- **Feature Configuration:** Enabling specific KPaper features

These patterns form the foundation of most KPaper plugins!