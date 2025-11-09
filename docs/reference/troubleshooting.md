# Troubleshooting

This guide helps you resolve common issues when developing with KPaper. If you don't find your issue here, please check our [GitHub Issues](https://github.com/ModLabsCC/KPaper/issues) or create a new one.

## Installation Issues

### KPaper Dependency Not Found

**Error:**
```
Could not resolve dependency: cc.modlabs:KPaper:LATEST
```

**Causes & Solutions:**

1. **Missing Repository**
   ```kotlin
   // Add to build.gradle.kts
   repositories {
       mavenCentral()
       maven("https://nexus.modlabs.cc/repository/maven-mirrors/") // Add this
   }
   ```

2. **Invalid Version**
   ```kotlin
   // Use specific version instead of LATEST
   dependencies {
       implementation("cc.modlabs:KPaper:2025.1.1.1234") // Use actual version
   }
   ```

3. **Network Issues**
   - Check internet connection
   - Try with VPN if corporate firewall blocks access
   - Use `--refresh-dependencies` flag: `./gradlew build --refresh-dependencies`

### Java Version Compatibility

**Error:**
```
Unsupported class file major version 65
```

**Solution:**
KPaper requires Java 21+. Update your JDK:

```kotlin
// In build.gradle.kts
kotlin {
    jvmToolchain(21) // Use Java 21
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}
```

### Paper Version Mismatch

**Error:**
```
NoSuchMethodError or ClassNotFoundException with Paper classes
```

**Solution:**
Ensure your Paper version matches KPaper requirements:

```kotlin
dependencies {
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT") // Match KPaper's target version
    implementation("cc.modlabs:KPaper:LATEST")
}
```

## Plugin Loading Issues

### Plugin Not Loading

**Error:**
```
Could not load 'plugins/YourPlugin.jar' in folder 'plugins'
```

**Troubleshooting Steps:**

1. **Check plugin.yml**
   ```yaml
   name: YourPlugin
   version: '1.0.0'
   main: com.yourpackage.YourPlugin  # Must match your class path
   api-version: '1.21'
   ```

2. **Verify Main Class**
   ```kotlin
   // Must extend KPlugin, not JavaPlugin
   class YourPlugin : KPlugin() {
       override fun startup() {
           // Implementation
       }
   }
   ```

3. **Check Dependencies**
   ```yaml
   # In plugin.yml - if you depend on other plugins
   depend:
     - SomeRequiredPlugin
   softdepend:
     - SomeOptionalPlugin
   ```

4. **Examine Server Logs**
   Look for specific error messages in server console.

### KPlugin Instance Error

**Error:**
```
The main instance has been modified, even though it has already been set by another plugin!
```

**Cause:** Multiple KPaper plugins creating conflicting instances.

**Solution:**
```kotlin
// Ensure only one plugin extends KPlugin, or use proper isolation
class YourPlugin : KPlugin() {
    override fun startup() {
        // Your plugin code
    }
}
```

## Event System Issues

### Events Not Firing

**Problem:** Event listeners not being called

**Troubleshooting:**

1. **Check Event Registration**
   ```kotlin
   // Correct
   listen<PlayerJoinEvent> { event ->
       // Handler code
   }
   
   // Wrong - missing listen call
   { event: PlayerJoinEvent ->
       // This won't work
   }
   ```

2. **Verify Event Priority**
   ```kotlin
   // If other plugins cancel events, use higher priority
   listen<PlayerInteractEvent>(priority = EventPriority.HIGH) { event ->
       // This runs before NORMAL priority listeners
   }
   ```

3. **Check Event Cancellation**
   ```kotlin
   listen<BlockBreakEvent> { event ->
       if (event.isCancelled) {
           // Event was cancelled by another plugin
           return@listen
       }
       // Your logic here
   }
   ```

### Custom Events Not Working

**Problem:** Custom events not being received by listeners

**Solution:**
```kotlin
// Ensure your custom event extends KEvent properly
class MyCustomEvent(val player: Player) : KEvent() {
    companion object {
        private val HANDLERS = HandlerList()
        
        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
    
    override fun getHandlers(): HandlerList = HANDLERS
}

// Dispatch the event properly
fun triggerCustomEvent(player: Player) {
    val event = MyCustomEvent(player)
    event.callEvent() // Don't forget this
}

// Listen for the event
listen<MyCustomEvent> { event ->
    // Handle custom event
}
```

## Command Issues

### Commands Not Registering

**Problem:** Commands don't appear in-game or in tab completion

**Troubleshooting:**

1. **Ensure .register() is Called**
   ```kotlin
   CommandBuilder("mycommand")
       .description("My command")
       .execute { sender, args ->
           sender.sendMessage("Hello!")
       }
       .register() // Must call this!
   ```

2. **Check Command Name Conflicts**
   ```kotlin
   // If another plugin uses the same command name
   CommandBuilder("mycommand")
       .aliases("myalias", "mycmd") // Use aliases instead
       .register()
   ```

3. **Verify Permission Setup**
   ```kotlin
   CommandBuilder("admin")
       .permission("myplugin.admin") // Make sure this permission exists
       .execute { sender, args ->
           // Command logic
       }
       .register()
   ```

### Argument Parsing Errors

**Problem:** Arguments not being parsed correctly

**Solutions:**

1. **Check Argument Order**
   ```kotlin
   CommandBuilder("teleport")
       .argument(playerArgument("target")) // Required arguments first
       .argument(worldArgument("world", optional = true)) // Optional arguments last
       .execute { sender, args ->
           val target = args.getPlayer("target")
           val world = args.getWorldOrDefault("world", sender.world)
       }
       .register()
   ```

2. **Handle Missing Arguments**
   ```kotlin
   CommandBuilder("give")
       .argument(materialArgument("item"))
       .argument(intArgument("amount", optional = true))
       .execute { sender, args ->
           val material = args.getMaterial("item")
           val amount = args.getIntOrDefault("amount", 1) // Use default if not provided
       }
       .register()
   ```

3. **Validate Argument Values**
   ```kotlin
   CommandBuilder("damage")
       .argument(intArgument("amount", min = 1, max = 100)) // Add constraints
       .execute { sender, args ->
           val amount = args.getInt("amount") // Will be between 1-100
       }
       .register()
   ```

## GUI Issues

### GUI Not Opening

**Problem:** Inventory GUI doesn't open when expected

**Troubleshooting:**

1. **Check Player State**
   ```kotlin
   fun openGUI(player: Player) {
       if (!player.isOnline) return // Player must be online
       
       // Close existing inventory first
       player.closeInventory()
       
       val gui = simpleGUI("My GUI", 27) {
           // GUI content
       }
       
       player.openInventory(gui)
   }
   ```

2. **Verify GUI Size**
   ```kotlin
   // Size must be multiple of 9, max 54
   val gui = simpleGUI("My GUI", 27) { // Valid: 9, 18, 27, 36, 45, 54
       // Content
   }
   ```

3. **Check for Errors in GUI Building**
   ```kotlin
   val gui = simpleGUI("My GUI", 27) {
       try {
           item(0, ItemBuilder(Material.STONE).name("Test").build()) {
               // Click handler
           }
       } catch (e: Exception) {
           plugin.logger.log(java.util.logging.Level.SEVERE, "Error building GUI", e)
       }
   }
   ```

### Click Events Not Working

**Problem:** GUI click handlers not responding

**Solutions:**

1. **Ensure Click Handler is Set**
   ```kotlin
   val gui = simpleGUI("My GUI", 27) {
       item(0, ItemBuilder(Material.STONE).name("Click me").build()) {
           // This block is the click handler - don't leave empty
           player.sendMessage("Clicked!")
       }
   }
   ```

2. **Check for Event Cancellation**
   ```kotlin
   // If you have global inventory click handlers, they might interfere
   listen<InventoryClickEvent> { event ->
       if (event.view.title.equals("My GUI", ignoreCase = true)) {
           // Don't cancel KPaper GUI events
           return@listen
       }
       event.isCancelled = true
   }
   ```

3. **Verify Slot Numbers**
   ```kotlin
   val gui = simpleGUI("My GUI", 27) {
       // Slots are 0-indexed: 0-26 for size 27
       item(26, item) { /* handler */ } // Last slot in 27-slot GUI
       // item(27, item) { } // This would be invalid!
   }
   ```

## Performance Issues

### High Memory Usage

**Problem:** Plugin consuming excessive memory

**Solutions:**

1. **Clean Up Event Listeners**
   ```kotlin
   class MyFeature : EventHandler() {
       override fun load() {
           listen<PlayerJoinEvent> { /* handler */ }
       }
       
       override fun unload() {
           // Proper cleanup - KPaper handles this automatically
           // but you can add custom cleanup here
       }
   }
   ```

2. **Avoid Memory Leaks in GUIs**
   ```kotlin
   // Don't store player references in static collections
   class MyGUI {
       companion object {
           private val playerData = mutableMapOf<UUID, PlayerData>() // Bad - can leak
       }
   }
   
   // Instead, use plugin-scoped collections
   class MyPlugin : KPlugin() {
       private val playerData = mutableMapOf<UUID, PlayerData>() // Good
       
       override fun shutdown() {
           playerData.clear() // Clean up on shutdown
       }
   }
   ```

3. **Optimize Data Caching**
   ```kotlin
   class DataCache<K, V>(
       private val maxSize: Int = 1000,
       private val expireAfter: Duration = Duration.ofMinutes(30)
   ) {
       private val cache = LinkedHashMap<K, CacheEntry<V>>()
       
       fun get(key: K, loader: () -> V): V {
           // Implement LRU cache with expiration
           cleanupExpired()
           
           val entry = cache[key]
           if (entry != null && !entry.isExpired()) {
               return entry.value
           }
           
           val value = loader()
           cache[key] = CacheEntry(value, System.currentTimeMillis())
           
           // Evict old entries
           while (cache.size > maxSize) {
               cache.remove(cache.keys.first())
           }
           
           return value
       }
   }
   ```

### Slow GUI Performance

**Problem:** GUIs take long time to open or update

**Solutions:**

1. **Use Async Loading**
   ```kotlin
   fun openShop(player: Player) {
       // Show loading GUI first
       val loadingGUI = simpleGUI("Loading...", 9) {
           item(4, ItemBuilder(Material.CLOCK).name("&eLoading...").build())
       }
       player.openInventory(loadingGUI)
       
       // Load data asynchronously
       launch {
           val items = loadShopItems() // Heavy operation
           
           withContext(Dispatchers.Main) {
               // Update GUI on main thread
               openShopWithItems(player, items)
           }
       }
   }
   ```

2. **Cache GUI Elements**
   ```kotlin
   object GUICache {
       private val itemCache = mutableMapOf<String, ItemStack>()
       
       fun getCachedItem(key: String, builder: () -> ItemStack): ItemStack {
           return itemCache.computeIfAbsent(key) { builder() }
       }
   }
   
   // Usage
   val item = GUICache.getCachedItem("shop_sword") {
       ItemBuilder(Material.DIAMOND_SWORD)
           .name("&cWeapons")
           .lore("&7Click to browse weapons!")
           .build()
   }
   ```

## Database and File Issues

### Configuration Not Loading

**Problem:** Configuration values returning defaults

**Solutions:**

1. **Check File Location**
   ```kotlin
   override fun startup() {
       // Ensure config file exists
       if (!File(dataFolder, "config.yml").exists()) {
           saveDefaultConfig() // Create default config
       }
       
       val config = loadConfiguration<MyConfig>()
   }
   ```

2. **Verify Configuration Class**
   ```kotlin
   // Ensure your config class has proper defaults
   data class MyConfig(
       val database: DatabaseConfig = DatabaseConfig(),
       val messages: Messages = Messages()
   ) {
       data class DatabaseConfig(
           val host: String = "localhost",
           val port: Int = 3306,
           val database: String = "minecraft"
       )
   }
   ```

3. **Handle Configuration Errors**
   ```kotlin
   override fun startup() {
       try {
           val config = loadConfiguration<MyConfig>()
           // Use config
       } catch (e: Exception) {
           cc.modlabs.kpaper.main.PluginInstance.logger.log(java.util.logging.Level.SEVERE, "Failed to load configuration", e)
           // Use defaults or disable plugin
           server.pluginManager.disablePlugin(this)
           return
       }
   }
   ```

### Async Operation Errors

**Problem:** Database operations causing server lag or errors

**Solutions:**

1. **Use Proper Coroutine Context**
   ```kotlin
   class DatabaseManager {
       suspend fun savePlayerData(data: PlayerData) {
           withContext(Dispatchers.IO) { // Use IO dispatcher for database operations
               // Database save operation
               database.save(data)
           }
       }
       
       fun savePlayerDataAsync(data: PlayerData) {
           launch { // This uses the plugin's coroutine scope
               savePlayerData(data)
           }
       }
   }
   ```

2. **Handle Database Connection Errors**
   ```kotlin
   class DatabaseManager {
       private var isConnected = false
       
       suspend fun ensureConnection() {
           if (!isConnected) {
               try {
                   database.connect()
                   isConnected = true
               } catch (e: SQLException) {
                   cc.modlabs.kpaper.main.PluginInstance.logger.log(java.util.logging.Level.SEVERE, "Failed to connect to database", e)
                   throw e
               }
           }
       }
       
       suspend fun saveData(data: Any) {
           try {
               ensureConnection()
               database.save(data)
           } catch (e: SQLException) {
               cc.modlabs.kpaper.main.PluginInstance.logger.log(java.util.logging.Level.SEVERE, "Failed to save data", e)
               // Implement retry logic or fallback
           }
       }
   }
   ```

## Common Error Messages

### "Failed to bind to server"

**Cause:** Another instance of the server is already running

**Solution:** Stop the existing server or use a different port

### "Plugin X tried to register command Y but it is already registered"

**Cause:** Command name conflict with another plugin

**Solution:** Use aliases or different command names
```kotlin
CommandBuilder("mycommand")
    .aliases("mycmd", "mc") // Use aliases instead of conflicting name
    .register()
```

### "Attempted to place a tile entity where there was no entity tile!"

**Cause:** Trying to access block state on wrong block type

**Solution:** Check block type before accessing state
```kotlin
val block = location.block
if (block.type == Material.CHEST) {
    val chest = block.state as Chest
    // Safe to use chest
}
```

### "Server thread/WARN]: Plugin attempted to register listener after being disabled"

**Cause:** Trying to register events after plugin shutdown

**Solution:** Check plugin state before registering events
```kotlin
override fun startup() {
    if (!isEnabled) return // Don't register if plugin is disabled
    
    listen<PlayerJoinEvent> { /* handler */ }
}
```

## Getting Help

### Debug Mode

Enable debug logging to get more information:

```kotlin
class MyPlugin : KPlugin() {
    override fun startup() {
        // Enable debug logging
        logger.level = Level.DEBUG
        
        logDebug("Plugin started in debug mode")
    }
}
```

### Collecting Information

When reporting issues, include:

1. **KPaper Version**
2. **Paper Version**  
3. **Java Version**
4. **Full Error Stack Trace**
5. **Minimal Reproducible Example**
6. **Server Log Excerpt**

### Community Support

- **GitHub Issues**: [https://github.com/ModLabsCC/KPaper/issues](https://github.com/ModLabsCC/KPaper/issues)
- **Discord**: Join the ModLabs Discord server
- **Documentation**: Check this documentation for examples

### Creating Bug Reports

Use this template for bug reports:

```markdown
**KPaper Version:** 2025.1.1.1234
**Paper Version:** 1.21.6-R0.1-SNAPSHOT  
**Java Version:** 21.0.1

**Description:**
Brief description of the issue

**Steps to Reproduce:**
1. Step one
2. Step two
3. Expected vs actual behavior

**Code Sample:**
```kotlin
// Minimal code that reproduces the issue
```

**Error Log:**
```
// Full stack trace here
```
```

Remember: Most issues are configuration-related or due to conflicts with other plugins. Always test with minimal setup first!
