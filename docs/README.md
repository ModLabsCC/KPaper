# KPaper Documentation

Welcome to the comprehensive documentation for KPaper - a Kotlin utility library that simplifies Paper plugin development.

## 📚 Documentation Overview

### Getting Started
- [Installation & Setup](getting-started/installation.md) - Set up KPaper in your project
- [Your First Plugin](getting-started/first-plugin.md) - Create your first KPaper plugin
- [Migration Guide](getting-started/migration.md) - Migrate from vanilla Bukkit/Paper

### Core Concepts
- [Plugin Development](core/plugin-development.md) - Understanding KPlugin and core concepts
- [Feature Configuration](core/feature-config.md) - Managing plugin features
- [Dependency Injection](core/dependency-injection.md) - Working with dependencies

### API Guides
- [Event System](api/events.md) - Event handling and custom events
- [Command Framework](api/commands.md) - Creating commands with arguments
- [Inventory & GUI](api/inventory.md) - Item builders and GUI systems
- [Extensions](api/extensions.md) - Kotlin extensions for Bukkit classes
- [Utilities](api/utilities.md) - Helper functions and utilities
- [Coroutines](api/coroutines.md) - Async operations with Kotlin coroutines
- [World Management](api/world.md) - World generation and manipulation
- [Messages & I18n](api/messages.md) - Message formatting and translations
- [Visual Effects](api/visuals.md) - Particle effects and displays
- [Game Mechanics](api/game.md) - Countdowns, timers, and game systems
- [File Management](api/file.md) - Configuration and file handling

### Examples & Recipes
- [Common Patterns](examples/common-patterns.md) - Frequently used code patterns
- [Plugin Examples](examples/plugins.md) - Complete plugin examples
- [Best Practices](examples/best-practices.md) - Recommended practices and conventions

### Reference
- [API Reference](reference/api.md) - Complete API documentation
- [Configuration Reference](reference/config.md) - All configuration options
- [Troubleshooting](reference/troubleshooting.md) - Common issues and solutions

## 🚀 Quick Start

```kotlin
class MyPlugin : KPlugin() {
    override fun startup() {
        // Event handling
        listen<PlayerJoinEvent> { 
            it.player.sendMessage("Welcome ${it.player.name}!") 
        }
        
        // Commands
        command("hello") {
            description = "Say hello to a player"
            execute { sender, args ->
                sender.sendMessage("Hello ${args.getOrNull(0) ?: "World"}!")
            }
        }
        
        // Custom GUIs
        val gui = simpleGUI("My GUI", 9) {
            item(0, ItemBuilder(Material.DIAMOND).name("Click me!").build()) {
                player.sendMessage("You clicked the diamond!")
            }
        }
    }
}
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](../CONTRIBUTING.md) for details on how to contribute to KPaper.

## 📝 License

KPaper is licensed under the GPL-3.0 License. See [LICENSE](../LICENSE) for details.