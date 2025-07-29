# KPaper

KPaper is a utility library designed to simplify plugin development with [Paper](https://papermc.io/) and [Kotlin](https://kotlinlang.org/). It provides Kotlin-friendly APIs and abstractions to make Paper plugin development faster, cleaner, and more enjoyable.

## Installation

```kotlin
repositories {
    maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
}

dependencies {
    implementation("cc.modlabs:KPaper:$version")
}
```

### Example Usage

Here's a basic example of using KPaper in a plugin:

```kotlin
import cc.modlabs.kpaper.event.listen
import cc.modlabs.kpaper.main.KPlugin
import org.bukkit.event.player.PlayerJoinEvent

class MyPlugin : KPlugin() {
    override fun startup() {
        listen<PlayerJoinEvent> {
            it.player.sendMessage("Welcome to the server, ${it.player.name}!")
        }
    }
}
```

## Package Structure

KPaper is organized into several focused packages:

- **`util`** - Utility functions including console output, logging, random number generation, and text processing
- **`extensions`** - Kotlin extension functions for Bukkit/Paper classes
- **`event`** - Event handling system with custom events and listeners
- **`inventory`** - Inventory management, item builders, and GUI systems
- **`command`** - Command framework and argument parsing
- **`main`** - Core plugin functionality and feature configuration
- **`world`** - World generation and manipulation utilities
- **`messages`** - Message formatting and translation support
- **`visuals`** - Visual effects and display systems
- **`game`** - Game mechanics like countdowns and player management
- **`file`** - File I/O and configuration management
- **`coroutines`** - Kotlin coroutines integration for async operations

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests on the [GitHub repository](https://github.com/ModLabsCC/KPaper).

## Acknowledgments

- [Paper](https://papermc.io/) for providing the high-performance server software.
- [Fruxz](https://github.com/TheFruxz) for the inspiration and libraries that helped shape KPaper.

 
