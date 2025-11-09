# Plugin Development

This guide covers the core concepts of KPaper plugin development, helping you understand the framework's architecture and best practices.

## Core Architecture

### KPlugin Base Class

KPaper plugins extend the `KPlugin` class instead of the traditional `JavaPlugin`:

```kotlin
import cc.modlabs.kpaper.main.KPlugin

class MyPlugin : KPlugin() {
    
    override fun startup() {
        // Called when plugin is enabled
        logger.info("Plugin starting up!")
    }
    
    override fun shutdown() {
        // Called when plugin is disabled
        logger.info("Plugin shutting down!")
    }
    
    override fun load() {
        // Called during plugin loading phase (before startup)
        logger.info("Plugin loading...")
    }
}
```

### Lifecycle Methods

KPaper provides three lifecycle methods:

| Method | When Called | Purpose |
|--------|-------------|---------|
| `load()` | Plugin loading phase | Early initialization, before dependencies |
| `startup()` | Plugin enabling phase | Main initialization, register features |
| `shutdown()` | Plugin disabling phase | Cleanup, save data |

## Feature Configuration

### Enabling Features

KPaper uses a feature system to enable only the components you need:

```kotlin
class MyPlugin : KPlugin() {
    
    override val featureConfig = featureConfig {
        // Core features
        enableEventFeatures = true      // Event system
        enableCommandFeatures = true    // Command framework
        enableInventoryFeatures = true  // GUI and inventory system
        
        // Advanced features
        enableCoroutineFeatures = true  // Async operations
        enableFileFeatures = true       // File I/O utilities
        enableWorldFeatures = true      // World management
        enableMessageFeatures = true    // Message system
        enableVisualFeatures = true     // Visual effects
        enableGameFeatures = true       // Game mechanics
        enableUtilFeatures = true       // Utility functions
    }
    
    override fun startup() {
        // Only enabled features are available
        if (isFeatureEnabled(Feature.INVENTORY)) {
            // Inventory features are available
            setupGUIs()
        }
        
        if (isFeatureEnabled(Feature.COMMANDS)) {
            // Command features are available
            setupCommands()
        }
    }
}
```

### Feature Dependencies

Some features depend on others:

```kotlin
// This configuration automatically enables dependencies
override val featureConfig = featureConfig {
    enableInventoryFeatures = true  // Requires event features
    enableCommandFeatures = true    // Requires util features
    enableVisualFeatures = true     // Requires world features
    
    // Dependencies are automatically enabled:
    // - Event features (for inventory)
    // - Util features (for commands)
    // - World features (for visuals)
}
```

## Plugin Structure Patterns

### Simple Plugin Structure

For basic plugins:

```kotlin
class SimplePlugin : KPlugin() {
    
    override val featureConfig = featureConfig {
        enableEventFeatures = true
        enableCommandFeatures = true
    }
    
    override fun startup() {
        setupEvents()
        setupCommands()
    }
    
    private fun setupEvents() {
        listen<PlayerJoinEvent> { event ->
            event.player.sendMessage("Welcome ${event.player.name}!")
        }
    }
    
    private fun setupCommands() {
        CommandBuilder("hello")
            .description("Say hello")
            .execute { sender, _ ->
                sender.sendMessage("Hello from ${description.name}!")
            }
            .register()
    }
}
```

### Complex Plugin Structure

For larger plugins with multiple systems:

```kotlin
class ComplexPlugin : KPlugin() {
    
    // Core managers
    private lateinit var configManager: ConfigurationManager
    private lateinit var dataManager: DataManager
    private lateinit var playerManager: PlayerManager
    
    // Feature systems
    private lateinit var economySystem: EconomySystem
    private lateinit var shopSystem: ShopSystem
    private lateinit var questSystem: QuestSystem
    
    override val featureConfig = featureConfig {
        enableEventFeatures = true
        enableCommandFeatures = true
        enableInventoryFeatures = true
        enableCoroutineFeatures = true
        enableFileFeatures = true
    }
    
    override fun load() {
        // Initialize core managers first
        configManager = ConfigurationManager(this)
        dataManager = DataManager(this, configManager.databaseConfig)
    }
    
    override fun startup() {
        // Initialize managers
        playerManager = PlayerManager(this, dataManager)
        
        // Initialize systems
        economySystem = EconomySystem(this, playerManager)
        shopSystem = ShopSystem(this, economySystem)
        questSystem = QuestSystem(this, playerManager, economySystem)
        
        // Load all systems
        val systems = listOf(
            playerManager,
            economySystem, 
            shopSystem,
            questSystem
        )
        
        systems.forEach { system ->
            try {
                system.initialize()
                logger.info("Initialized ${system.javaClass.simpleName}")
            } catch (e: Exception) {
                logger.severe("Failed to initialize ${system.javaClass.simpleName}: ${e.message}")
            }
        }
        
        logger.info("Plugin fully loaded with ${systems.size} systems")
    }
    
    override fun shutdown() {
        // Shutdown in reverse order
        listOf(questSystem, shopSystem, economySystem, playerManager)
            .reversed()
            .forEach { system ->
                try {
                    system.shutdown()
                } catch (e: Exception) {
                    logger.severe("Error shutting down ${system.javaClass.simpleName}: ${e.message}")
                }
            }
        
        dataManager.close()
    }
}

// Base class for plugin systems
abstract class PluginSystem(protected val plugin: ComplexPlugin) {
    abstract fun initialize()
    abstract fun shutdown()
    
    protected fun log(message: String) {
        plugin.logger.info("[${javaClass.simpleName}] $message")
    }
    
    protected fun logError(message: String, exception: Throwable? = null) {
        if (exception != null) plugin.logger.log(java.util.logging.Level.SEVERE, "[${javaClass.simpleName}] $message", exception)
        else plugin.logger.severe("[${javaClass.simpleName}] $message")
    }
}
```

## Configuration Management

### Type-Safe Configuration

Define configuration classes for type safety:

```kotlin
// Configuration data classes
data class PluginConfig(
    val database: DatabaseConfig = DatabaseConfig(),
    val economy: EconomyConfig = EconomyConfig(),
    val messages: MessageConfig = MessageConfig(),
    val features: FeatureConfig = FeatureConfig()
)

data class DatabaseConfig(
    val type: String = "sqlite",
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "minecraft",
    val username: String = "root",
    val password: String = "",
    val connectionTimeout: Int = 30000
)

data class EconomyConfig(
    val startingBalance: Double = 100.0,
    val maxBalance: Double = 1000000.0,
    val enableBanking: Boolean = true,
    val transactionFee: Double = 0.05
)

data class MessageConfig(
    val prefix: String = "&8[&6MyPlugin&8] &r",
    val noPermission: String = "&cYou don't have permission to do that!",
    val playerNotFound: String = "&cPlayer not found!",
    val economyInsufficientFunds: String = "&cInsufficient funds!"
)

data class FeatureConfig(
    val enableShop: Boolean = true,
    val enableQuests: Boolean = true,
    val enablePvP: Boolean = false
)

// Loading configuration
class ConfigurationManager(private val plugin: KPlugin) {
    
    lateinit var config: PluginConfig
        private set
    
    fun load() {
        config = plugin.loadConfiguration<PluginConfig>()
        
        // Validate configuration
        validateConfig()
        
        // Log configuration summary
        logConfigSummary()
    }
    
    fun reload() {
        plugin.reloadConfig()
        load()
    }
    
    private fun validateConfig() {
        require(config.economy.startingBalance >= 0) {
            "Starting balance cannot be negative"
        }
        
        require(config.economy.maxBalance > config.economy.startingBalance) {
            "Max balance must be greater than starting balance"
        }
        
        if (config.database.type == "mysql") {
            require(config.database.host.isNotBlank()) {
                "MySQL host cannot be empty"
            }
        }
    }
    
    private fun logConfigSummary() {
        plugin.logger.info("Configuration loaded:")
        plugin.logger.info("  Database: ${config.database.type}")
        plugin.logger.info("  Economy enabled: ${config.features.enableShop}")
        plugin.logger.info("  Starting balance: ${config.economy.startingBalance}")
    }
}
```

### Configuration Files

Create default configuration files:

```kotlin
// resources/config.yml
database:
  type: "sqlite"
  host: "localhost"
  port: 3306
  database: "minecraft"
  username: "root"
  password: ""
  connection-timeout: 30000

economy:
  starting-balance: 100.0
  max-balance: 1000000.0
  enable-banking: true
  transaction-fee: 0.05

messages:
  prefix: "&8[&6MyPlugin&8] &r"
  no-permission: "&cYou don't have permission to do that!"
  player-not-found: "&cPlayer not found!"
  economy-insufficient-funds: "&cInsufficient funds!"

features:
  enable-shop: true
  enable-quests: true
  enable-pvp: false
```

## Error Handling and Logging

### Structured Error Handling

Implement comprehensive error handling:

```kotlin
class RobustPlugin : KPlugin() {
    
    override fun startup() {
        try {
            initializePlugin()
        } catch (e: ConfigurationException) {
            logger.severe("Configuration error: ${e.message}")
            logger.severe("Plugin will be disabled. Please fix your configuration.")
            server.pluginManager.disablePlugin(this)
        } catch (e: DatabaseException) {
            logger.severe("Database connection failed: ${e.message}")
            logger.severe("Running in offline mode with limited functionality.")
            initializeOfflineMode()
        } catch (e: Exception) {
            logger.log(java.util.logging.Level.SEVERE, "Unexpected error during startup", e)
            server.pluginManager.disablePlugin(this)
        }
    }
    
    private fun initializePlugin() {
        // Phase 1: Load configuration
        logPhase("Loading configuration...")
        val config = loadConfiguration<PluginConfig>()
        
        // Phase 2: Initialize database
        logPhase("Connecting to database...")
        val database = connectToDatabase(config.database)
        
        // Phase 3: Load player data
        logPhase("Loading player data...")
        val playerManager = PlayerManager(database)
        
        // Phase 4: Initialize systems
        logPhase("Initializing game systems...")
        initializeSystems(config, playerManager)
        
        logger.info("Plugin initialization completed successfully!")
    }
    
    private fun logPhase(message: String) {
        logger.info("=== $message ===")
    }
    
    private fun connectToDatabase(config: DatabaseConfig): Database {
        return try {
            when (config.type.lowercase()) {
                "mysql" -> MySQLDatabase(config)
                "sqlite" -> SQLiteDatabase(config) 
                else -> throw ConfigurationException("Unsupported database type: ${config.type}")
            }
        } catch (e: SQLException) {
            throw DatabaseException("Failed to connect to ${config.type} database", e)
        }
    }
}

// Custom exceptions
class ConfigurationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

### Logging Best Practices

Use structured logging for better debugging:

```kotlin
class LoggingExamples : KPlugin() {
    
    override fun startup() {
        // Use appropriate log levels
        logger.info("[DEBUG] Debug information for developers")
        logger.info("General information")
        logger.warning("Warning about potential issues")
        logger.severe("Error that needs attention")
        
        // Log with context
        logPlayerAction("Steve", "joined the server")
        logSystemEvent("Economy", "player balance updated", mapOf(
            "player" to "Steve",
            "oldBalance" to 100.0,
            "newBalance" to 150.0,
            "reason" to "quest_completion"
        ))
        
        // Log performance metrics
        val startTime = System.currentTimeMillis()
        performExpensiveOperation()
        val duration = System.currentTimeMillis() - startTime
        logger.info("Expensive operation completed in ${duration}ms")
    }
    
    private fun logPlayerAction(player: String, action: String) {
        logger.info("Player action: $player $action")
    }
    
    private fun logSystemEvent(system: String, event: String, data: Map<String, Any>) {
        val dataString = data.entries.joinToString(", ") { "${it.key}=${it.value}" }
        logger.info("[$system] $event ($dataString)")
    }
}
```

## Performance Optimization

### Async Operations

Use coroutines for expensive operations:

```kotlin
class OptimizedPlugin : KPlugin() {
    
    private val databaseScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun startup() {
        // Initialize async systems
        initializeAsyncSystems()
    }
    
    override fun shutdown() {
        // Cancel all async operations
        databaseScope.cancel()
    }
    
    private fun initializeAsyncSystems() {
        // Load data asynchronously
        launch {
            val playerData = withContext(Dispatchers.IO) {
                loadAllPlayerData() // Heavy database operation
            }
            
            // Switch back to main thread for Bukkit operations
            withContext(Dispatchers.Main) {
                applyPlayerData(playerData)
                logger.info("Loaded ${playerData.size} player records")
            }
        }
    }
    
    fun savePlayerDataAsync(player: Player, data: PlayerData) {
        databaseScope.launch {
            try {
                database.savePlayerData(player.uniqueId, data)
                logger.info("[DEBUG] Saved data for ${player.name}")
            } catch (e: Exception) {
                logger.log(java.util.logging.Level.SEVERE, "Failed to save data for ${player.name}", e)
            }
        }
    }
}
```

### Caching Strategies

Implement efficient caching:

```kotlin
class CachedDataManager {
    
    // Player data cache with expiration
    private val playerCache = ExpiringCache<UUID, PlayerData>(
        expireAfter = Duration.ofMinutes(30),
        maxSize = 1000
    )
    
    // Configuration cache (rarely changes)
    private val configCache = mutableMapOf<String, Any>()
    
    // World data cache (per-world data)
    private val worldCache = ConcurrentHashMap<String, WorldData>()
    
    suspend fun getPlayerData(uuid: UUID): PlayerData {
        return playerCache.get(uuid) {
            // Load from database if not cached
            database.loadPlayerData(uuid) ?: PlayerData.createDefault(uuid)
        }
    }
    
    fun invalidatePlayerData(uuid: UUID) {
        playerCache.invalidate(uuid)
    }
    
    fun getWorldData(worldName: String): WorldData {
        return worldCache.computeIfAbsent(worldName) { name ->
            loadWorldData(name)
        }
    }
}

class ExpiringCache<K, V>(
    private val expireAfter: Duration,
    private val maxSize: Int
) {
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    
    suspend fun get(key: K, loader: suspend (K) -> V): V {
        val entry = cache[key]
        
        // Check if cached and not expired
        if (entry != null && !entry.isExpired()) {
            return entry.value
        }
        
        // Load new value
        val value = loader(key)
        cache[key] = CacheEntry(value, System.currentTimeMillis())
        
        // Cleanup if needed
        if (cache.size > maxSize) {
            cleanup()
        }
        
        return value
    }
    
    fun invalidate(key: K) {
        cache.remove(key)
    }
    
    private fun cleanup() {
        val now = System.currentTimeMillis()
        val expireTime = expireAfter.toMillis()
        
        cache.entries.removeIf { entry ->
            now - entry.value.timestamp > expireTime
        }
    }
    
    private data class CacheEntry<V>(val value: V, val timestamp: Long) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > Duration.ofMinutes(30).toMillis()
        }
    }
}
```

## Testing and Debugging

### Unit Testing

Write tests for your plugin logic:

```kotlin
class PluginTest {
    
    @Test
    fun testEconomySystem() {
        val economy = EconomySystem()
        val player = mockPlayer("TestPlayer")
        
        // Test initial balance
        assertEquals(100.0, economy.getBalance(player))
        
        // Test deposit
        economy.deposit(player, 50.0)
        assertEquals(150.0, economy.getBalance(player))
        
        // Test withdrawal
        assertTrue(economy.withdraw(player, 25.0))
        assertEquals(125.0, economy.getBalance(player))
        
        // Test insufficient funds
        assertFalse(economy.withdraw(player, 200.0))
        assertEquals(125.0, economy.getBalance(player))
    }
    
    @Test
    fun testQuestCompletion() {
        val questSystem = QuestSystem()
        val player = mockPlayer("TestPlayer")
        val quest = Quest("test_quest", "Test Quest", listOf(
            KillObjective("zombie", 10),
            CollectObjective(Material.DIAMOND, 5)
        ))
        
        questSystem.startQuest(player, quest)
        
        // Test progress tracking
        questSystem.updateProgress(player, quest, "zombie_kill")
        assertEquals(1, questSystem.getProgress(player, quest, "zombie"))
        
        // Test completion
        questSystem.setProgress(player, quest, "zombie", 10)
        questSystem.setProgress(player, quest, "diamond", 5)
        
        assertTrue(questSystem.isQuestCompleted(player, quest))
    }
    
    private fun mockPlayer(name: String): Player {
        // Create mock player for testing
        return mock<Player>().apply {
            whenever(this.name).thenReturn(name)
            whenever(this.uniqueId).thenReturn(UUID.randomUUID())
        }
    }
}
```

### Debug Tools

Create debugging utilities:

```kotlin
class DebugTools(private val plugin: KPlugin) {
    
    fun dumpPlayerData(player: Player) {
        plugin.logger.info("=== Player Data Dump: ${player.name} ===")
        plugin.logger.info("UUID: ${player.uniqueId}")
        plugin.logger.info("Location: ${player.location}")
        plugin.logger.info("Health: ${player.health}/${player.maxHealth}")
        plugin.logger.info("Food: ${player.foodLevel}")
        plugin.logger.info("Level: ${player.level} (${player.exp} exp)")
        plugin.logger.info("Game Mode: ${player.gameMode}")
        plugin.logger.info("Permissions: ${player.effectivePermissions.map { it.permission }}")
    }
    
    fun dumpSystemStatus() {
        plugin.logger.info("=== System Status ===")
        plugin.logger.info("Online Players: ${plugin.server.onlinePlayers.size}")
        plugin.logger.info("Loaded Worlds: ${plugin.server.worlds.size}")
        plugin.logger.info("TPS: ${plugin.server.tps.joinToString(", ")}")
        plugin.logger.info("Memory: ${getMemoryUsage()}")
    }
    
    private fun getMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        val max = runtime.maxMemory()
        return "${used / 1024 / 1024}MB / ${max / 1024 / 1024}MB"
    }
}
```

These patterns and practices will help you build robust, maintainable KPaper plugins that scale well and provide a great user experience.
