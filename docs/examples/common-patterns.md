# Common Patterns

This guide covers frequently used patterns and best practices when developing with KPaper. These patterns will help you write more maintainable, efficient, and robust plugins.

## Plugin Architecture Patterns

### 1. Feature-Based Organization

Organize your plugin into focused feature modules:

```kotlin
class MyPlugin : KPlugin() {
    
    // Feature modules
    private lateinit var playerManager: PlayerManager
    private lateinit var economySystem: EconomySystem
    private lateinit var shopSystem: ShopSystem
    private lateinit var pvpManager: PvPManager
    
    override val featureConfig = featureConfig {
        enableEventFeatures = true
        enableCommandFeatures = true
        enableInventoryFeatures = true
        enableCoroutineFeatures = true
    }
    
    override fun startup() {
        // Initialize core systems first
        playerManager = PlayerManager(this)
        economySystem = EconomySystem(this, playerManager)
        
        // Initialize dependent systems
        shopSystem = ShopSystem(this, economySystem)
        pvpManager = PvPManager(this, playerManager)
        
        // Load all features
        listOf(playerManager, economySystem, shopSystem, pvpManager)
            .forEach { it.initialize() }
    }
    
    override fun shutdown() {
        // Shutdown in reverse order
        listOf(pvpManager, shopSystem, economySystem, playerManager)
            .forEach { it.shutdown() }
    }
}

abstract class PluginFeature(protected val plugin: MyPlugin) {
    abstract fun initialize()
    abstract fun shutdown()
}

class PlayerManager(plugin: MyPlugin) : PluginFeature(plugin) {
    private val playerData = mutableMapOf<UUID, PlayerData>()
    
    override fun initialize() {
        // Register events, commands, etc.
        listen<PlayerJoinEvent> { handlePlayerJoin(it) }
        listen<PlayerQuitEvent> { handlePlayerQuit(it) }
    }
    
    override fun shutdown() {
        // Save all player data
        playerData.values.forEach { savePlayerData(it) }
    }
}
```

### 2. Configuration-Driven Features

Make features configurable and toggleable:

```kotlin
class ConfigurablePlugin : KPlugin() {
    
    private lateinit var config: PluginConfig
    private val features = mutableListOf<ConditionalFeature>()
    
    override fun startup() {
        config = loadConfiguration()
        
        // Register features based on configuration
        if (config.economyEnabled) {
            features.add(EconomyFeature(this, config.economyConfig))
        }
        
        if (config.pvpEnabled) {
            features.add(PvPFeature(this, config.pvpConfig))
        }
        
        if (config.shopEnabled && config.economyEnabled) {
            features.add(ShopFeature(this, config.shopConfig))
        }
        
        // Initialize all enabled features
        features.forEach { it.initialize() }
    }
}

abstract class ConditionalFeature(
    protected val plugin: ConfigurablePlugin,
    protected val config: FeatureConfig
) {
    abstract fun initialize()
    abstract fun shutdown()
    abstract val name: String
    
    protected fun log(message: String) {
        plugin.logger.info("[$name] $message")
    }
}
```

## Event Handling Patterns

### 1. Event Manager Pattern

Centralize event handling with dedicated managers:

```kotlin
class EventManager(private val plugin: MyPlugin) {
    
    private val handlers = mutableListOf<EventHandler>()
    
    fun initialize() {
        // Register all event handlers
        handlers.addAll(listOf(
            PlayerEventHandler(plugin),
            BlockEventHandler(plugin),
            InventoryEventHandler(plugin),
            CombatEventHandler(plugin)
        ))
        
        handlers.forEach { it.load() }
    }
    
    fun shutdown() {
        handlers.forEach { it.unload() }
        handlers.clear()
    }
}

class PlayerEventHandler(private val plugin: MyPlugin) : EventHandler() {
    
    override fun load() {
        listen<PlayerJoinEvent> { handleJoin(it) }
        listen<PlayerQuitEvent> { handleQuit(it) }
        listen<PlayerMoveEvent> { handleMove(it) }
    }
    
    override fun unload() {
        // Cleanup if needed
    }
    
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Load player data
        launch {
            val data = loadPlayerData(player.uniqueId)
            withContext(Dispatchers.Main) {
                applyPlayerData(player, data)
            }
        }
        
        // Send welcome message
        event.joinMessage(
            Component.text("${player.name} joined the game!")
                .color(NamedTextColor.GREEN)
        )
        
        // First-time player setup
        if (!player.hasPlayedBefore()) {
            setupNewPlayer(player)
        }
    }
}
```

### 2. Event Priority Chain

Handle events with different priorities for different purposes:

```kotlin
class LayeredEventSystem {
    
    fun initialize() {
        // HIGHEST: Security and anti-cheat (can cancel everything)
        listen<PlayerInteractEvent>(priority = EventPriority.HIGHEST) { event ->
            if (isSecurityViolation(event)) {
                event.isCancelled = true
                handleSecurityViolation(event.player)
                return@listen
            }
        }
        
        // HIGH: Region protection
        listen<PlayerInteractEvent>(priority = EventPriority.HIGH) { event ->
            val region = getRegion(event.interactionPoint ?: return@listen)
            if (!region.canInteract(event.player)) {
                event.isCancelled = true
                event.player.sendMessage("You cannot interact here!")
                return@listen
            }
        }
        
        // NORMAL: Feature functionality
        listen<PlayerInteractEvent>(priority = EventPriority.NORMAL) { event ->
            handleCustomItems(event)
            handleInteractables(event)
        }
        
        // LOW: Statistics and logging
        listen<PlayerInteractEvent>(priority = EventPriority.LOW) { event ->
            if (!event.isCancelled) {
                recordInteraction(event)
                updateStatistics(event.player)
            }
        }
    }
}
```

## Data Management Patterns

### 1. Repository Pattern

Abstract data access with repository interfaces:

```kotlin
interface PlayerRepository {
    suspend fun getPlayer(uuid: UUID): PlayerData?
    suspend fun savePlayer(data: PlayerData)
    suspend fun getAllPlayers(): List<PlayerData>
    suspend fun deletePlayer(uuid: UUID)
}

class DatabasePlayerRepository(
    private val database: Database
) : PlayerRepository {
    
    override suspend fun getPlayer(uuid: UUID): PlayerData? = withContext(Dispatchers.IO) {
        database.selectFrom("players")
            .where("uuid", uuid.toString())
            .executeQuery()
            ?.let { PlayerData.fromResultSet(it) }
    }
    
    override suspend fun savePlayer(data: PlayerData) = withContext(Dispatchers.IO) {
        database.insertOrUpdate("players", data.toMap())
    }
}

class FilePlayerRepository(
    private val dataFolder: File
) : PlayerRepository {
    
    override suspend fun getPlayer(uuid: UUID): PlayerData? = withContext(Dispatchers.IO) {
        val file = File(dataFolder, "players/$uuid.json")
        if (file.exists()) {
            gson.fromJson(file.readText(), PlayerData::class.java)
        } else null
    }
    
    override suspend fun savePlayer(data: PlayerData) = withContext(Dispatchers.IO) {
        val file = File(dataFolder, "players/${data.uuid}.json")
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(data))
    }
}

// Usage in plugin
class PlayerDataManager(
    private val repository: PlayerRepository
) {
    private val cache = mutableMapOf<UUID, PlayerData>()
    
    suspend fun getPlayerData(uuid: UUID): PlayerData {
        return cache[uuid] ?: run {
            val data = repository.getPlayer(uuid) ?: PlayerData.createDefault(uuid)
            cache[uuid] = data
            data
        }
    }
    
    suspend fun savePlayerData(data: PlayerData) {
        cache[data.uuid] = data
        repository.savePlayer(data)
    }
}
```

### 2. Caching Strategies

Implement efficient caching for frequently accessed data:

```kotlin
class CacheManager<K, V>(
    private val loader: suspend (K) -> V,
    private val maxSize: Int = 1000,
    private val expireAfter: Duration = Duration.ofMinutes(30)
) {
    private val cache = mutableMapOf<K, CacheEntry<V>>()
    private val accessOrder = LinkedHashMap<K, Long>()
    
    suspend fun get(key: K): V {
        val now = System.currentTimeMillis()
        val entry = cache[key]
        
        // Check if cached and not expired
        if (entry != null && (now - entry.timestamp) < expireAfter.toMillis()) {
            accessOrder[key] = now
            return entry.value
        }
        
        // Load new value
        val value = loader(key)
        cache[key] = CacheEntry(value, now)
        accessOrder[key] = now
        
        // Evict old entries if needed
        evictIfNeeded()
        
        return value
    }
    
    private fun evictIfNeeded() {
        while (cache.size > maxSize) {
            val oldestKey = accessOrder.keys.first()
            cache.remove(oldestKey)
            accessOrder.remove(oldestKey)
        }
    }
    
    private data class CacheEntry<V>(val value: V, val timestamp: Long)
}

// Usage
class OptimizedPlayerManager {
    private val playerCache = CacheManager<UUID, PlayerData>(
        loader = { uuid -> repository.getPlayer(uuid) ?: PlayerData.createDefault(uuid) },
        maxSize = 500,
        expireAfter = Duration.ofMinutes(15)
    )
    
    suspend fun getPlayer(uuid: UUID): PlayerData {
        return playerCache.get(uuid)
    }
}
```

## GUI Patterns

### 1. Menu Navigation System

Create a hierarchical menu system:

```kotlin
abstract class NavigableGUI(
    protected val player: Player,
    protected val parent: NavigableGUI? = null
) : KGUI() {
    
    protected fun addBackButton(slot: Int = 45) {
        if (parent != null) {
            item(slot, ItemBuilder(Material.ARROW)
                .name("&7← Back")
                .lore("&7Return to previous menu")
                .build()) {
                parent.open(player)
            }
        }
    }
    
    protected fun addCloseButton(slot: Int = 49) {
        item(slot, ItemBuilder(Material.BARRIER)
            .name("&cClose")
            .lore("&7Close this menu")
            .build()) {
            player.closeInventory()
        }
    }
}

class MainMenuGUI(player: Player) : NavigableGUI(player) {
    override val title = "Main Menu"
    override val size = 27
    
    override fun build(player: Player): GUI {
        return GUI(title, size) {
            
            item(11, ItemBuilder(Material.DIAMOND_SWORD)
                .name("&cPvP Arena")
                .lore("&7Join the PvP arena!")
                .build()) {
                PvPMenuGUI(player, this@MainMenuGUI).open(player)
            }
            
            item(13, ItemBuilder(Material.CHEST)
                .name("&eShop")
                .lore("&7Buy and sell items!")
                .build()) {
                ShopMenuGUI(player, this@MainMenuGUI).open(player)
            }
            
            item(15, ItemBuilder(Material.PLAYER_HEAD)
                .name("&aProfile")
                .lore("&7View your profile!")
                .skullOwner(player)
                .build()) {
                ProfileGUI(player, this@MainMenuGUI).open(player)
            }
            
            addCloseButton()
        }
    }
}

class ShopMenuGUI(
    player: Player, 
    parent: NavigableGUI
) : NavigableGUI(player, parent) {
    
    override val title = "Shop"
    override val size = 54
    
    override fun build(player: Player): GUI {
        return GUI(title, size) {
            // Shop categories...
            addBackButton()
            addCloseButton()
        }
    }
}
```

### 2. Dynamic Content Loading

Load GUI content asynchronously:

```kotlin
class AsyncShopGUI(player: Player) : KGUI() {
    override val title = "Shop (Loading...)"
    override val size = 54
    
    private var items: List<ShopItem> = emptyList()
    private var isLoading = true
    
    override fun build(player: Player): GUI {
        return GUI(title, size) {
            if (isLoading) {
                // Show loading animation
                showLoadingAnimation()
                
                // Load items asynchronously
                launch {
                    items = loadShopItems()
                    isLoading = false
                    
                    withContext(Dispatchers.Main) {
                        refresh(player) // Rebuild GUI with loaded content
                    }
                }
            } else {
                // Show actual shop content
                showShopItems()
            }
        }
    }
    
    private fun GUI.showLoadingAnimation() {
        val loadingSlots = listOf(19, 20, 21, 22, 23, 24, 25)
        val frame = (System.currentTimeMillis() / 200) % loadingSlots.size
        
        loadingSlots.forEachIndexed { index, slot ->
            val isActive = index == frame.toInt()
            item(slot, ItemBuilder(
                if (isActive) Material.LIME_STAINED_GLASS_PANE 
                else Material.GRAY_STAINED_GLASS_PANE
            ).name(" ").build())
        }
        
        // Continue animation if still loading
        if (isLoading) {
            taskRunLater(10) { refresh(player) }
        }
    }
    
    private fun GUI.showShopItems() {
        items.forEachIndexed { index, item ->
            item(index + 9, ItemBuilder(item.material)
                .name("&e${item.name}")
                .lore("&7Price: &a$${item.price}")
                .build()) {
                purchaseItem(player, item)
            }
        }
    }
}
```

## Command Patterns

### 1. Command Router Pattern

Route commands to appropriate handlers:

```kotlin
class CommandRouter(private val plugin: MyPlugin) {
    
    private val handlers = mutableMapOf<String, CommandHandler>()
    
    fun initialize() {
        // Register command handlers
        handlers["economy"] = EconomyCommandHandler(plugin)
        handlers["admin"] = AdminCommandHandler(plugin)
        handlers["player"] = PlayerCommandHandler(plugin)
        
        // Create router command
        CommandBuilder("game")
            .description("Game command router")
            .argument(stringArgument("category"))
            .argument(stringArgument("action"))
            .execute { sender, args ->
                val category = args.getString("category")
                val action = args.getString("action")
                
                val handler = handlers[category]
                if (handler == null) {
                    sender.sendMessage("Unknown category: $category")
                    return@execute
                }
                
                handler.handle(sender, action, args)
            }
            .register()
    }
}

abstract class CommandHandler(protected val plugin: MyPlugin) {
    abstract fun handle(sender: CommandSender, action: String, args: CommandArguments)
    
    protected fun requirePlayer(sender: CommandSender): Player? {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players!")
            return null
        }
        return sender
    }
    
    protected fun requirePermission(sender: CommandSender, permission: String): Boolean {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage("You don't have permission to do that!")
            return false
        }
        return true
    }
}

class EconomyCommandHandler(plugin: MyPlugin) : CommandHandler(plugin) {
    override fun handle(sender: CommandSender, action: String, args: CommandArguments) {
        when (action.lowercase()) {
            "balance" -> handleBalance(sender, args)
            "pay" -> handlePay(sender, args)
            "top" -> handleTop(sender, args)
            else -> sender.sendMessage("Unknown economy action: $action")
        }
    }
    
    private fun handleBalance(sender: CommandSender, args: CommandArguments) {
        // Balance command logic
    }
}
```

### 2. Command Validation Pipeline

Create reusable validation chains:

```kotlin
abstract class CommandValidator {
    abstract fun validate(sender: CommandSender, args: CommandArguments): ValidationResult
    
    companion object {
        fun chain(vararg validators: CommandValidator): CommandValidator {
            return ChainValidator(validators.toList())
        }
    }
}

class ChainValidator(private val validators: List<CommandValidator>) : CommandValidator() {
    override fun validate(sender: CommandSender, args: CommandArguments): ValidationResult {
        validators.forEach { validator ->
            val result = validator.validate(sender, args)
            if (!result.isValid) {
                return result
            }
        }
        return ValidationResult.success()
    }
}

class PlayerOnlyValidator : CommandValidator() {
    override fun validate(sender: CommandSender, args: CommandArguments): ValidationResult {
        return if (sender is Player) {
            ValidationResult.success()
        } else {
            ValidationResult.failure("This command can only be used by players!")
        }
    }
}

class PermissionValidator(private val permission: String) : CommandValidator() {
    override fun validate(sender: CommandSender, args: CommandArguments): ValidationResult {
        return if (sender.hasPermission(permission)) {
            ValidationResult.success()
        } else {
            ValidationResult.failure("You don't have permission to use this command!")
        }
    }
}

class CooldownValidator(
    private val cooldownManager: CooldownManager,
    private val commandName: String,
    private val cooldownSeconds: Int
) : CommandValidator() {
    
    override fun validate(sender: CommandSender, args: CommandArguments): ValidationResult {
        if (sender !is Player) return ValidationResult.success()
        
        if (cooldownManager.hasCooldown(sender, commandName)) {
            val remaining = cooldownManager.getCooldown(sender, commandName) / 1000
            return ValidationResult.failure("Command is on cooldown for ${remaining} seconds!")
        }
        
        return ValidationResult.success()
    }
}

// Usage
CommandBuilder("heal")
    .validator(CommandValidator.chain(
        PlayerOnlyValidator(),
        PermissionValidator("heal.use"),
        CooldownValidator(cooldownManager, "heal", 30)
    ))
    .execute { sender, args ->
        val player = sender as Player
        player.health = player.maxHealth
        player.sendMessage("You have been healed!")
        cooldownManager.setCooldown(player, "heal", 30)
    }
    .register()
```

## Async Patterns

### 1. Task Queue System

Process tasks asynchronously with proper ordering:

```kotlin
class TaskQueue<T>(
    private val processor: suspend (T) -> Unit,
    private val maxConcurrent: Int = 3
) {
    private val queue = Channel<T>(Channel.UNLIMITED)
    private val workers = mutableListOf<Job>()
    
    fun start() {
        repeat(maxConcurrent) { workerId ->
            val worker = launch {
                for (task in queue) {
                    try {
                        processor(task)
                    } catch (e: Exception) {
                        logger.error("Task processing failed in worker $workerId", e)
                    }
                }
            }
            workers.add(worker)
        }
    }
    
    suspend fun submit(task: T) {
        queue.send(task)
    }
    
    fun stop() {
        queue.close()
        runBlocking {
            workers.joinAll()
        }
    }
}

// Usage
class PlayerDataProcessor(private val plugin: MyPlugin) {
    
    private val saveQueue = TaskQueue<PlayerSaveTask>(
        processor = { task -> savePlayerData(task) },
        maxConcurrent = 2
    )
    
    fun initialize() {
        saveQueue.start()
        
        // Auto-save every 5 minutes
        taskRunTimer(0, 6000) { // 5 minutes in ticks
            launch {
                plugin.server.onlinePlayers.forEach { player ->
                    saveQueue.submit(PlayerSaveTask(player.uniqueId, getCurrentPlayerData(player)))
                }
            }
        }
    }
    
    private suspend fun savePlayerData(task: PlayerSaveTask) {
        // Perform actual save operation
        withContext(Dispatchers.IO) {
            database.savePlayer(task.uuid, task.data)
        }
    }
}
```

### 2. Resource Management

Properly manage resources and cleanup:

```kotlin
class ResourceManager {
    private val resources = mutableListOf<AutoCloseable>()
    
    fun <T : AutoCloseable> register(resource: T): T {
        resources.add(resource)
        return resource
    }
    
    fun cleanup() {
        resources.reversed().forEach { resource ->
            try {
                resource.close()
            } catch (e: Exception) {
                logger.error("Failed to close resource", e)
            }
        }
        resources.clear()
    }
}

class DatabaseConnection : AutoCloseable {
    private val connection = DriverManager.getConnection(url, user, password)
    
    override fun close() {
        connection.close()
    }
}

class MyPlugin : KPlugin() {
    private val resourceManager = ResourceManager()
    
    override fun startup() {
        // Register resources for automatic cleanup
        val database = resourceManager.register(DatabaseConnection())
        val httpClient = resourceManager.register(OkHttpClient())
        val scheduler = resourceManager.register(ScheduledExecutorService())
        
        // Use resources...
    }
    
    override fun shutdown() {
        resourceManager.cleanup()
    }
}
```

## Error Handling Patterns

### 1. Result Pattern

Handle operations that can fail gracefully:

```kotlin
sealed class Result<T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure<T>(val error: String, val exception: Throwable? = null) : Result<T>()
    
    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure
    
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
    
    fun getOrDefault(default: T): T = when (this) {
        is Success -> value
        is Failure -> default
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(value)
        return this
    }
    
    inline fun onFailure(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Failure) action(error, exception)
        return this
    }
}

class PlayerService {
    
    suspend fun loadPlayerData(uuid: UUID): Result<PlayerData> {
        return try {
            val data = withContext(Dispatchers.IO) {
                database.getPlayerData(uuid)
            }
            
            if (data != null) {
                Result.Success(data)
            } else {
                Result.Failure("Player data not found")
            }
        } catch (e: Exception) {
            Result.Failure("Failed to load player data", e)
        }
    }
    
    fun handlePlayerJoin(player: Player) {
        launch {
            loadPlayerData(player.uniqueId)
                .onSuccess { data ->
                    withContext(Dispatchers.Main) {
                        applyPlayerData(player, data)
                        player.sendMessage("Welcome back!")
                    }
                }
                .onFailure { error, exception ->
                    logger.error("Failed to load player data for ${player.name}: $error", exception)
                    withContext(Dispatchers.Main) {
                        player.sendMessage("Failed to load your data. Using defaults.")
                        applyDefaultData(player)
                    }
                }
        }
    }
}
```

### 2. Circuit Breaker Pattern

Prevent cascading failures:

```kotlin
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val recoveryTimeout: Duration = Duration.ofMinutes(1)
) {
    private var failures = 0
    private var lastFailureTime = 0L
    private var state = State.CLOSED
    
    enum class State { CLOSED, OPEN, HALF_OPEN }
    
    suspend fun <T> execute(operation: suspend () -> T): Result<T> {
        when (state) {
            State.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime >= recoveryTimeout.toMillis()) {
                    state = State.HALF_OPEN
                } else {
                    return Result.Failure("Circuit breaker is open")
                }
            }
            State.HALF_OPEN -> {
                // Allow one test request
            }
            State.CLOSED -> {
                // Normal operation
            }
        }
        
        return try {
            val result = operation()
            onSuccess()
            Result.Success(result)
        } catch (e: Exception) {
            onFailure()
            Result.Failure("Operation failed", e)
        }
    }
    
    private fun onSuccess() {
        failures = 0
        state = State.CLOSED
    }
    
    private fun onFailure() {
        failures++
        lastFailureTime = System.currentTimeMillis()
        
        if (failures >= failureThreshold) {
            state = State.OPEN
        }
    }
}

// Usage
class ExternalAPIService {
    private val circuitBreaker = CircuitBreaker(failureThreshold = 3)
    
    suspend fun fetchPlayerStats(playerName: String): Result<PlayerStats> {
        return circuitBreaker.execute {
            httpClient.get("$apiUrl/player/$playerName")
                .body<PlayerStats>()
        }
    }
}
```

These patterns provide a solid foundation for building robust, maintainable KPaper plugins. Mix and match them as needed for your specific use case!