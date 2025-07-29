# KPaper

<div align="center">

[![Build Status](https://img.shields.io/github/actions/workflow/status/ModLabsCC/KPaper/build.yml?branch=main)](https://github.com/ModLabsCC/KPaper/actions)
[![License](https://img.shields.io/github/license/ModLabsCC/KPaper)](LICENSE)
[![Version](https://img.shields.io/maven-central/v/cc.modlabs/KPaper)](https://central.sonatype.com/artifact/cc.modlabs/KPaper)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/paper-1.21.6-green.svg)](https://papermc.io)

*A comprehensive Kotlin utility library that revolutionizes Paper plugin development*

[📚 Documentation](docs/README.md) • [🚀 Quick Start](#quick-start) • [💡 Examples](#examples) • [🤝 Contributing](#contributing)

</div>

---

KPaper is a powerful utility library designed to simplify plugin development with [Paper](https://papermc.io/) and [Kotlin](https://kotlinlang.org/). It provides Kotlin-friendly APIs, intuitive abstractions, and comprehensive tools to make Paper plugin development **faster**, **cleaner**, and **more enjoyable**.

## ✨ Key Features

- **🎯 Kotlin-First Design** - Built specifically for Kotlin with idiomatic APIs
- **⚡ Event System** - Simplified event handling with powerful filtering and custom events
- **🎮 GUI Framework** - Intuitive inventory and GUI creation with built-in interactions
- **⌨️ Command Builder** - Fluent command creation with automatic argument parsing
- **🔧 Rich Extensions** - Extensive Kotlin extensions for Bukkit/Paper classes
- **🔄 Coroutines Support** - Full async/await support with Kotlin coroutines
- **🌍 World Management** - Advanced world generation and manipulation tools
- **💬 Message System** - Internationalization and rich text formatting
- **🎨 Visual Effects** - Particle systems and display management
- **🎯 Game Mechanics** - Timers, countdowns, and player management systems

## 🚀 Quick Start

### Installation

```kotlin
repositories {
    mavenCentral()
    maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
}

dependencies {
    implementation("cc.modlabs:KPaper:LATEST") // Replace with latest version
}
```

### Your First Plugin

```kotlin
import cc.modlabs.kpaper.main.KPlugin
import cc.modlabs.kpaper.event.listen
import cc.modlabs.kpaper.command.CommandBuilder
import cc.modlabs.kpaper.inventory.ItemBuilder
import cc.modlabs.kpaper.inventory.simple.simpleGUI
import org.bukkit.Material
import org.bukkit.event.player.PlayerJoinEvent

class MyPlugin : KPlugin() {
    override fun startup() {
        // 🎉 Event handling made simple
        listen<PlayerJoinEvent> { event ->
            val player = event.player
            player.sendMessage("Welcome to the server, ${player.name}!")
            
            // Give welcome items
            val welcomeItem = ItemBuilder(Material.DIAMOND)
                .name("&6Welcome Gift!")
                .lore("&7Thanks for joining our server!")
                .glowing(true)
                .build()
            
            player.inventory.addItem(welcomeItem)
        }
        
        // ⌨️ Commands with KPaper integration
        class ShopCommand : CommandBuilder {
            override val description = "Open the server shop"
            
            override fun register() = Commands.literal("shop")
                .requires { it.sender.hasPermission("shop.use") && it.sender is Player }
                .executes { ctx ->
                    openShopGUI(ctx.source.sender as Player)
                    Command.SINGLE_SUCCESS
                }
                .build()
        }
        
        // Register in lifecycle event
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val shopCommand = ShopCommand()
            event.registrar().register(shopCommand.register(), shopCommand.description)
        }
    }
    
    private fun openShopGUI(player: Player) {
        // 🎮 Beautiful GUIs in seconds
        val gui = simpleGUI("Server Shop", 27) {
            
            item(13, ItemBuilder(Material.DIAMOND_SWORD)
                .name("&cWeapons")
                .lore("&7Click to browse weapons!")
                .build()) {
                player.sendMessage("&aOpening weapons shop...")
                // Open weapons submenu
            }
            
            item(22, ItemBuilder(Material.BARRIER)
                .name("&cClose")
                .build()) {
                player.closeInventory()
            }
        }
        
        player.openInventory(gui)
    }
}
```

## 💡 Examples

<details>
<summary><b>Advanced Event Handling</b></summary>

```kotlin
// Multi-event listener with conditions
listen<BlockBreakEvent> { event ->
    val player = event.player
    val block = event.block
    
    // Only in mining world
    if (block.world.name != "mining") return@listen
    
    // Give experience based on block type
    when (block.type) {
        Material.DIAMOND_ORE -> player.giveExp(50)
        Material.GOLD_ORE -> player.giveExp(25)
        Material.IRON_ORE -> player.giveExp(10)
        else -> player.giveExp(1)
    }
    
    // Custom drop with chance
    if (Random.nextDouble() < 0.1) { // 10% chance
        val bonus = ItemBuilder(Material.DIAMOND)
            .name("&bBonus Diamond!")
            .build()
        block.world.dropItem(block.location, bonus)
    }
}

// Custom event creation and handling
class PlayerLevelUpEvent(val player: Player, val newLevel: Int) : KEvent()

fun levelUpPlayer(player: Player, level: Int) {
    PlayerLevelUpEvent(player, level).callEvent()
}

listen<PlayerLevelUpEvent> { event ->
    val rewards = mapOf(
        10 to listOf(ItemStack(Material.DIAMOND, 5)),
        25 to listOf(ItemStack(Material.NETHERITE_INGOT, 1)),
        50 to listOf(ItemStack(Material.DRAGON_EGG, 1))
    )
    
    rewards[event.newLevel]?.forEach { reward ->
        event.player.inventory.addItem(reward)
    }
}
```
</details>

<details>
<summary><b>Complex GUI Systems</b></summary>

```kotlin
// Paginated shop with categories
class ShopGUI(private val category: ShopCategory) : KGUI() {
    override val title = "Shop - ${category.name}"
    override val size = 54
    
    private var page = 0
    private val itemsPerPage = 28
    
    override fun build(player: Player): GUI {
        return GUI(title, size) {
            // Category tabs
            ShopCategory.values().forEachIndexed { index, cat ->
                item(index, ItemBuilder(cat.icon)
                    .name(if (cat == category) "&a${cat.name}" else "&7${cat.name}")
                    .glowing(cat == category)
                    .build()) {
                    ShopGUI(cat).open(player)
                }
            }
            
            // Items grid
            val items = category.getItems()
            val startIndex = page * itemsPerPage
            
            items.drop(startIndex).take(itemsPerPage).forEachIndexed { index, item ->
                val slot = 18 + (index % 7) + (index / 7) * 9
                
                item(slot, ItemBuilder(item.material)
                    .name("&e${item.name}")
                    .lore(
                        "&7Price: &a$${item.price}",
                        "&7Stock: &b${item.stock}",
                        "",
                        "&eLeft-click to buy 1",
                        "&eRight-click to buy stack"
                    )
                    .build()) { clickEvent ->
                    
                    val amount = if (clickEvent.isRightClick) item.maxStackSize else 1
                    purchaseItem(player, item, amount)
                }
            }
            
            // Navigation
            if (page > 0) {
                item(48, ItemBuilder(Material.ARROW)
                    .name("&7← Previous Page")
                    .build()) {
                    page--
                    refresh(player)
                }
            }
            
            if (startIndex + itemsPerPage < items.size) {
                item(50, ItemBuilder(Material.ARROW)
                    .name("&7Next Page →")
                    .build()) {
                    page++
                    refresh(player)
                }
            }
        }
    }
}
```
</details>

<details>
<summary><b>Advanced Commands</b></summary>

```kotlin
// Complex command with sub-commands and validation  
class EconomyCommand : CommandBuilder {
    override val description = "Economy management commands"
    
    override fun register() = Commands.literal("economy")
        .requires { it.sender.hasPermission("economy.admin") }
        
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
                        val player = ctx.source.sender as Player
                        val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                        val amount = DoubleArgumentType.getDouble(ctx, "amount")
                        
                        if (economyAPI.getBalance(player) < amount) {
                            player.sendMessage("§cInsufficient funds!")
                            return@executes Command.SINGLE_SUCCESS
                        }
                        
                        economyAPI.transfer(player, target, amount)
                        player.sendMessage("§aSent $$amount to ${target.name}")
                        target.sendMessage("§aReceived $$amount from ${player.name}")
                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
        .build()
}
```
    
    // /economy set <player> <amount>
    .subcommand("set")
    .permission("economy.admin.set")
    .argument(playerArgument("target"))
    .argument(doubleArgument("amount", min = 0.0))
    .execute { sender, args ->
        val target = args.getPlayer("target")
        val amount = args.getDouble("amount")
        
        economyAPI.setBalance(target, amount)
        sender.sendMessage("&aSet ${target.name}'s balance to $${amount}")
    }
    .register()
```
</details>

<details>
<summary><b>Async Operations & Coroutines</b></summary>

```kotlin
// Database operations with coroutines
class PlayerDataManager {
    
    suspend fun loadPlayerData(player: Player): PlayerData {
        return withContext(Dispatchers.IO) {
            database.getPlayerData(player.uniqueId)
        }
    }
    
    fun setupPlayer(player: Player) {
        // Load data asynchronously
        launch {
            val data = loadPlayerData(player)
            
            // Switch back to main thread for Bukkit operations
            withContext(Dispatchers.Main) {
                applyPlayerData(player, data)
                player.sendMessage("&aData loaded successfully!")
            }
        }
    }
    
    // Delayed operations
    fun startWelcomeSequence(player: Player) {
        launch {
            player.sendMessage("&eWelcome to the server!")
            
            delay(2000) // 2 seconds
            player.sendMessage("&eLoading your data...")
            
            val data = loadPlayerData(player)
            
            delay(1000) // 1 second
            withContext(Dispatchers.Main) {
                player.sendMessage("&aReady to play!")
                teleportToSpawn(player)
            }
        }
    }
}
```
</details>

## 📚 Documentation

### 🏁 Getting Started
- **[Installation & Setup](docs/getting-started/installation.md)** - Get up and running quickly
- **[Your First Plugin](docs/getting-started/first-plugin.md)** - Step-by-step tutorial  
- **[Migration Guide](docs/getting-started/migration.md)** - Move from vanilla Bukkit/Paper

### 🔧 Core Concepts  
- **[Plugin Development](docs/core/plugin-development.md)** - Understanding KPlugin and features
- **[Feature Configuration](docs/core/feature-config.md)** - Managing plugin capabilities

### 📖 API Reference
- **[Event System](docs/api/events.md)** - Event handling and custom events
- **[Command Framework](docs/api/commands.md)** - Creating powerful commands
- **[Inventory & GUI](docs/api/inventory.md)** - Interactive menus and item builders
- **[Extensions](docs/api/extensions.md)** - Kotlin extensions for Bukkit classes
- **[Utilities](docs/api/utilities.md)** - Helper functions and tools
- **[Coroutines](docs/api/coroutines.md)** - Async programming support
- **[World Management](docs/api/world.md)** - World generation and manipulation
- **[Messages & I18n](docs/api/messages.md)** - Internationalization support
- **[Visual Effects](docs/api/visuals.md)** - Particles and displays
- **[Game Mechanics](docs/api/game.md)** - Timers and game systems

### 💡 Examples & Guides
- **[Common Patterns](docs/examples/common-patterns.md)** - Frequently used patterns
- **[Complete Plugins](docs/examples/plugins.md)** - Full plugin examples
- **[Best Practices](docs/examples/best-practices.md)** - Recommended approaches

## 🏗️ Architecture

KPaper is organized into focused, cohesive packages:

| Package | Purpose | Key Features |
|---------|---------|--------------|
| **`main`** | Core plugin functionality | KPlugin base class, feature management |
| **`event`** | Event handling system | Simplified listeners, custom events |
| **`command`** | Command framework | Fluent builders, argument parsing |
| **`inventory`** | GUI and item systems | Item builders, interactive menus |
| **`extensions`** | Kotlin extensions | Enhanced Bukkit/Paper APIs |
| **`coroutines`** | Async operations | Kotlin coroutines integration |
| **`util`** | Utility functions | Logging, text processing, helpers |
| **`world`** | World management | Generation, manipulation tools |
| **`messages`** | Messaging system | I18n, formatting, components |
| **`visuals`** | Visual effects | Particles, displays, animations |
| **`game`** | Game mechanics | Timers, countdowns, player systems |
| **`file`** | File management | Configuration, serialization |

## 🎯 Why Choose KPaper?

### Before KPaper (Vanilla Paper/Bukkit)
```kotlin
class OldPlugin : JavaPlugin() {
    override fun onEnable() {
        // Verbose event registration
        server.pluginManager.registerEvents(object : Listener {
            @EventHandler
            fun onPlayerJoin(event: PlayerJoinEvent) {
                event.player.sendMessage("Welcome!")
            }
        }, this)
        
        // Manual command registration
        getCommand("test")?.setExecutor { sender, _, _, args ->
            if (sender !is Player) {
                sender.sendMessage("Players only!")
                return@setExecutor false
            }
            
            if (args.isEmpty()) {
                sender.sendMessage("Usage: /test <message>")
                return@setExecutor false
            }
            
            sender.sendMessage("You said: ${args.joinToString(" ")}")
            true
        }
        
        // Complex inventory creation
        val inventory = server.createInventory(null, 27, "My GUI")
        val item = ItemStack(Material.DIAMOND_SWORD)
        val meta = item.itemMeta
        meta.displayName(Component.text("Click me!"))
        item.itemMeta = meta
        inventory.setItem(13, item)
        // ... more boilerplate
    }
}
```

### With KPaper
```kotlin
class NewPlugin : KPlugin() {
    override fun startup() {
        // Clean, concise event handling
        listen<PlayerJoinEvent> { 
            it.player.sendMessage("Welcome!") 
        }
        
        // Fluent command building
        command("test") {
            description = "Test command"
            playerOnly = true
            execute { sender, args ->
                if (args.isEmpty()) {
                    sender.sendMessage("Usage: /test <message>")
                    return@execute
                }
                sender.sendMessage("You said: ${args.joinToString(" ")}")
            }
        }
        
        // Intuitive GUI creation
        val gui = simpleGUI("My GUI", 27) {
            item(13, ItemBuilder(Material.DIAMOND_SWORD)
                .name("Click me!")
                .build()) {
                player.sendMessage("Button clicked!")
            }
        }
    }
}
```

**Result:** 60% less boilerplate, 90% more readable, 100% more maintainable!

## 🤝 Contributing

We welcome contributions from the community! Whether you're fixing bugs, adding features, or improving documentation, your help makes KPaper better for everyone.

### How to Contribute

1. **Fork the repository** on GitHub
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Make your changes** following our coding standards
4. **Add tests** for new functionality
5. **Update documentation** as needed
6. **Commit your changes** (`git commit -m 'Add amazing feature'`)
7. **Push to your branch** (`git push origin feature/amazing-feature`)
8. **Open a Pull Request** with a clear description

### Development Setup

```bash
git clone https://github.com/ModLabsCC/KPaper.git
cd KPaper
./gradlew build
```

### Contribution Guidelines

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Add KDoc comments for public APIs
- Include unit tests for new features  
- Update documentation for user-facing changes
- Keep commits focused and atomic

## 📄 License

KPaper is licensed under the **GPL-3.0 License**. See [LICENSE](LICENSE) for details.

## 🙏 Acknowledgments

- **[Paper](https://papermc.io/)** - For providing exceptional server software that makes modern Minecraft development possible
- **[Fruxz](https://github.com/TheFruxz)** - For inspiration and foundational libraries that helped shape KPaper's design
- **[Kotlin](https://kotlinlang.org/)** - For creating a language that makes JVM development enjoyable
- **Our Contributors** - Thank you to everyone who has contributed code, documentation, and feedback!

---

<div align="center">

**Made with ❤️ by the ModLabs Team**

[Website](https://modlabs.cc) • [Discord](https://discord.gg/modlabs) • [GitHub](https://github.com/ModLabsCC)

</div>
