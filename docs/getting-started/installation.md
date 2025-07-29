# Installation & Setup

This guide will help you set up KPaper in your Paper plugin project.

## Prerequisites

- **Java 21+** - KPaper requires Java 21 or later
- **Kotlin** - Your project should use Kotlin (though Java interop is supported)
- **Paper** - KPaper is designed specifically for Paper servers (not Bukkit/Spigot)
- **Gradle** - We recommend using Gradle as your build system

## Project Setup

### 1. Gradle Configuration

Add the KPaper repository and dependency to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
    maven("https://repo.papermc.io/repository/maven-public/") // Paper repository
}

dependencies {
    // Paper development bundle
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT") // Use your target version
    
    // KPaper
    implementation("cc.modlabs:KPaper:LATEST") // Replace LATEST with specific version
}
```

### 2. Plugin Configuration

Your plugin needs to use KPaper's base class instead of the standard JavaPlugin. Here's your main plugin class:

```kotlin
import cc.modlabs.kpaper.main.KPlugin

class YourPlugin : KPlugin() {
    
    override fun startup() {
        // Your plugin initialization code goes here
        logger.info("Plugin enabled with KPaper!")
    }
    
    override fun shutdown() {
        // Cleanup code when plugin is disabled
        logger.info("Plugin disabled.")
    }
    
    override fun load() {
        // Called during plugin loading phase (before enable)
        // Use this for early initialization
    }
}
```

### 3. Plugin.yml Configuration

Create or update your `plugin.yml` file:

```yaml
name: YourPlugin
version: '1.0.0'
main: com.yourpackage.YourPlugin
api-version: '1.21'
authors: [YourName]
description: A plugin using KPaper
website: https://yourwebsite.com

# Optional: Specify dependencies
depend:
  - SomeRequiredPlugin
softdepend:
  - SomeOptionalPlugin
```

## Build System Setup

### Gradle Build Configuration

Here's a complete `build.gradle.kts` example:

```kotlin
plugins {
    kotlin("jvm") version "2.0.21"
    id("io.papermc.paperweight.userdev") version "1.7.4"
    id("xyz.jpenilla.run-paper") version "2.3.1" // Optional: for testing
}

group = "com.yourpackage"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
}

dependencies {
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT")
    implementation("cc.modlabs:KPaper:LATEST")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }
}

kotlin {
    jvmToolchain(21)
}
```

## Verification

### Test Your Setup

Create a simple test to verify KPaper is working:

```kotlin
import cc.modlabs.kpaper.main.KPlugin
import cc.modlabs.kpaper.event.listen
import org.bukkit.event.player.PlayerJoinEvent

class TestPlugin : KPlugin() {
    override fun startup() {
        // Test event listening
        listen<PlayerJoinEvent> { event ->
            event.player.sendMessage("KPaper is working!")
        }
        
        logger.info("KPaper test plugin loaded successfully!")
    }
}
```

### Build and Test

1. **Build your plugin:**
   ```bash
   ./gradlew build
   ```

2. **Run with Paper:** (if using run-paper plugin)
   ```bash
   ./gradlew runServer
   ```

3. **Manual Testing:**
   - Copy the generated JAR from `build/libs/` to your Paper server's `plugins/` folder
   - Start your server and verify the plugin loads without errors

## Common Issues

### "Cannot resolve KPaper dependency"
- Ensure you've added the correct repository URL
- Check that you're using a valid version number
- Verify your internet connection can reach the repository

### "Unsupported Java version"
- KPaper requires Java 21+
- Update your JDK and ensure Gradle is using the correct version

### "Paper classes not found"
- Make sure you're using the Paper development bundle
- Verify your Paper version matches the bundle version

### ClassNotFoundException at runtime
- Ensure KPaper is properly shaded into your plugin JAR
- Check that all dependencies are included in your build

## Next Steps

Once you have KPaper set up, check out:
- [Your First Plugin](first-plugin.md) - Create a simple plugin
- [Plugin Development](../core/plugin-development.md) - Learn KPaper fundamentals
- [API Guides](../api/) - Explore KPaper's features

## Version Compatibility

| KPaper Version | Paper Version | Java Version |
|---------------|---------------|--------------|
| 2025.x        | 1.21.6+       | 21+          |
| 2024.x        | 1.20.4+       | 21+          |

Always check the [releases page](https://github.com/ModLabsCC/KPaper/releases) for the latest version and compatibility information.