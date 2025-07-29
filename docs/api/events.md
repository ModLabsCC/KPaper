# Event System

KPaper provides a powerful and intuitive event system that simplifies Bukkit event handling while adding advanced features like custom events and event priorities.

## Basic Event Handling

### Simple Event Listening

The most common way to handle events in KPaper is using the `listen` function:

```kotlin
import cc.modlabs.kpaper.event.listen
import org.bukkit.event.player.PlayerJoinEvent

class MyPlugin : KPlugin() {
    override fun startup() {
        // Basic event listener
        listen<PlayerJoinEvent> { event ->
            val player = event.player
            player.sendMessage("Welcome ${player.name}!")
        }
    }
}
```

### Event Priorities

You can specify event priorities to control execution order:

```kotlin
import org.bukkit.event.EventPriority

// High priority - executes early
listen<PlayerJoinEvent>(priority = EventPriority.HIGH) { event ->
    logger.info("${event.player.name} is joining (high priority)")
}

// Low priority - executes late
listen<PlayerJoinEvent>(priority = EventPriority.LOW) { event ->
    logger.info("${event.player.name} joined (low priority)")
}
```

### Conditional Event Handling

Only handle events when certain conditions are met:

```kotlin
// Only handle events for specific players
listen<PlayerJoinEvent> { event ->
    if (event.player.hasPermission("vip.join")) {
        event.player.sendMessage("Welcome VIP player!")
        // Give VIP welcome items
    }
}

// Handle events in specific worlds
listen<BlockBreakEvent> { event ->
    if (event.block.world.name == "mining_world") {
        // Special mining world logic
        event.player.giveExp(1)
    }
}
```

## Advanced Event Features

### Event Cancellation

Easily cancel events based on conditions:

```kotlin
listen<PlayerInteractEvent> { event ->
    if (event.player.world.name == "spawn") {
        if (!event.player.hasPermission("spawn.interact")) {
            event.isCancelled = true
            event.player.sendMessage("You cannot interact in spawn!")
        }
    }
}
```

### Multiple Event Handlers

Register multiple handlers for the same event:

```kotlin
// Handler 1: Logging
listen<PlayerJoinEvent> { event ->
    logger.info("Player ${event.player.name} joined from ${event.player.address?.address}")
}

// Handler 2: Welcome message
listen<PlayerJoinEvent> { event ->
    event.player.sendMessage("Welcome to our server!")
}

// Handler 3: First-time player setup
listen<PlayerJoinEvent> { event ->
    if (!event.player.hasPlayedBefore()) {
        giveStarterKit(event.player)
    }
}
```

## Custom Events

KPaper makes it easy to create and dispatch custom events:

### Creating Custom Events

```kotlin
import cc.modlabs.kpaper.event.KEvent
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class PlayerLevelUpEvent(
    val player: Player,
    val oldLevel: Int,
    val newLevel: Int
) : KEvent() {
    
    companion object {
        private val HANDLERS = HandlerList()
        
        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
    
    override fun getHandlers(): HandlerList = HANDLERS
}
```

### Dispatching Custom Events

```kotlin
import cc.modlabs.kpaper.event.callEvent

fun levelUpPlayer(player: Player, newLevel: Int) {
    val oldLevel = getPlayerLevel(player)
    
    // Dispatch the custom event
    val event = PlayerLevelUpEvent(player, oldLevel, newLevel)
    event.callEvent()
    
    // Continue with level up logic if event wasn't cancelled
    if (!event.isCancelled) {
        setPlayerLevel(player, newLevel)
    }
}
```

### Listening to Custom Events

```kotlin
listen<PlayerLevelUpEvent> { event ->
    val player = event.player
    val newLevel = event.newLevel
    
    player.sendMessage("Congratulations! You reached level $newLevel!")
    
    // Give rewards based on level
    when (newLevel) {
        10 -> player.inventory.addItem(ItemStack(Material.DIAMOND, 5))
        25 -> player.inventory.addItem(ItemStack(Material.NETHERITE_INGOT, 1))
        50 -> {
            player.sendMessage("You've reached level 50! Here's a special reward!")
            // Give special reward
        }
    }
}
```

## Event Handler Classes

For more complex event handling, you can create dedicated event handler classes:

### Creating Event Handlers

```kotlin
import cc.modlabs.kpaper.event.EventHandler

class PlayerEventHandler : EventHandler() {
    
    override fun load() {
        // Register events when handler is loaded
        listen<PlayerJoinEvent> { handlePlayerJoin(it) }
        listen<PlayerQuitEvent> { handlePlayerQuit(it) }
        listen<PlayerDeathEvent> { handlePlayerDeath(it) }
    }
    
    override fun unload() {
        // Cleanup when handler is unloaded
        logger.info("Player event handler unloaded")
    }
    
    private fun handlePlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Update join message
        event.joinMessage(Component.text("${player.name} has entered the realm!")
            .color(NamedTextColor.GREEN))
        
        // First time player setup
        if (!player.hasPlayedBefore()) {
            setupNewPlayer(player)
        }
        
        // Update player data
        updatePlayerData(player)
    }
    
    private fun handlePlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // Update quit message
        event.quitMessage(Component.text("${player.name} has left the realm!")
            .color(NamedTextColor.YELLOW))
        
        // Save player data
        savePlayerData(player)
    }
    
    private fun handlePlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val killer = player.killer
        
        if (killer != null) {
            // PvP death
            event.deathMessage(Component.text("${player.name} was slain by ${killer.name}")
                .color(NamedTextColor.RED))
        } else {
            // Environmental death
            event.deathMessage(Component.text("${player.name} met an unfortunate end")
                .color(NamedTextColor.DARK_RED))
        }
        
        // Custom death handling
        handleDeathRewards(player, killer)
    }
}
```

### Registering Event Handlers

```kotlin
class MyPlugin : KPlugin() {
    private lateinit var playerHandler: PlayerEventHandler
    
    override fun startup() {
        // Register the event handler
        playerHandler = PlayerEventHandler()
        playerHandler.load()
    }
    
    override fun shutdown() {
        // Unload the event handler
        if (::playerHandler.isInitialized) {
            playerHandler.unload()
        }
    }
}
```

## Event Utilities

### Event Filtering

Create reusable event filters:

```kotlin
fun <T : Event> isInWorld(worldName: String): (T) -> Boolean = { event ->
    when (event) {
        is PlayerEvent -> event.player.world.name == worldName
        is BlockEvent -> event.block.world.name == worldName
        is EntityEvent -> event.entity.world.name == worldName
        else -> false
    }
}

// Usage
listen<PlayerInteractEvent>(filter = isInWorld("pvp_arena")) { event ->
    // Only handle interactions in PvP arena
    handlePvPInteraction(event)
}
```

### Delayed Event Handling

Handle events after a delay:

```kotlin
import cc.modlabs.kpaper.coroutines.taskRunLater

listen<PlayerJoinEvent> { event ->
    val player = event.player
    
    // Send welcome message after 3 seconds
    taskRunLater(60) { // 60 ticks = 3 seconds
        if (player.isOnline) {
            player.sendMessage("Hope you're enjoying the server!")
        }
    }
}
```

### Event Metrics

Track event statistics:

```kotlin
class EventMetrics {
    private val eventCounts = mutableMapOf<String, Int>()
    
    fun trackEvent(eventName: String) {
        eventCounts[eventName] = eventCounts.getOrDefault(eventName, 0) + 1
    }
    
    fun getEventCount(eventName: String): Int = eventCounts.getOrDefault(eventName, 0)
    
    fun getAllCounts(): Map<String, Int> = eventCounts.toMap()
}

val metrics = EventMetrics()

listen<PlayerJoinEvent> { event ->
    metrics.trackEvent("player_join")
    // Handle join...
}

listen<PlayerQuitEvent> { event ->
    metrics.trackEvent("player_quit")
    // Handle quit...
}
```

## Common Event Patterns

### Player Management

```kotlin
class PlayerManager {
    private val playerData = mutableMapOf<UUID, PlayerData>()
    
    fun initialize() {
        // Load player on join
        listen<PlayerJoinEvent> { event ->
            val player = event.player
            playerData[player.uniqueId] = loadPlayerData(player.uniqueId)
        }
        
        // Save player on quit
        listen<PlayerQuitEvent> { event ->
            val player = event.player
            playerData[player.uniqueId]?.let { data ->
                savePlayerData(player.uniqueId, data)
            }
            playerData.remove(player.uniqueId)
        }
        
        // Auto-save periodically
        listen<PlayerMoveEvent> { event ->
            if (event.hasChangedBlock()) {
                updateLastLocation(event.player, event.to)
            }
        }
    }
}
```

### World Protection

```kotlin
fun setupWorldProtection() {
    val protectedWorlds = setOf("spawn", "hub", "tutorial")
    
    // Prevent block breaking in protected worlds
    listen<BlockBreakEvent> { event ->
        if (event.block.world.name in protectedWorlds) {
            if (!event.player.hasPermission("worldprotect.bypass")) {
                event.isCancelled = true
                event.player.sendMessage("You cannot break blocks in this world!")
            }
        }
    }
    
    // Prevent block placing in protected worlds
    listen<BlockPlaceEvent> { event ->
        if (event.block.world.name in protectedWorlds) {
            if (!event.player.hasPermission("worldprotect.bypass")) {
                event.isCancelled = true
                event.player.sendMessage("You cannot place blocks in this world!")
            }
        }
    }
}
```

### Anti-Grief System

```kotlin
class AntiGriefSystem {
    private val recentBreaks = mutableMapOf<UUID, MutableList<BlockBreakRecord>>()
    
    fun initialize() {
        listen<BlockBreakEvent> { event ->
            val player = event.player
            val record = BlockBreakRecord(
                location = event.block.location,
                material = event.block.type,
                timestamp = System.currentTimeMillis()
            )
            
            recentBreaks.computeIfAbsent(player.uniqueId) { mutableListOf() }
                .add(record)
            
            // Check for suspicious activity
            if (isSuspiciousActivity(player.uniqueId)) {
                handleSuspiciousActivity(player, event)
            }
            
            // Cleanup old records
            cleanupOldRecords(player.uniqueId)
        }
    }
    
    private fun isSuspiciousActivity(playerId: UUID): Boolean {
        val breaks = recentBreaks[playerId] ?: return false
        val recentBreaks = breaks.filter { 
            System.currentTimeMillis() - it.timestamp < 60000 // Last minute
        }
        
        return recentBreaks.size > 50 // More than 50 blocks per minute
    }
}
```

## Best Practices

### 1. Use Specific Event Types
```kotlin
// Good - specific event
listen<PlayerInteractEntityEvent> { event ->
    if (event.rightClicked is Villager) {
        // Handle villager interaction
    }
}

// Avoid - too general
listen<PlayerInteractEvent> { event ->
    // Handles all interactions, less efficient
}
```

### 2. Handle Null Cases
```kotlin
listen<EntityDamageByEntityEvent> { event ->
    val damager = event.damager
    val victim = event.entity
    
    // Safe casting
    if (damager is Player && victim is Player) {
        handlePvPDamage(damager, victim, event.damage)
    }
}
```

### 3. Avoid Heavy Operations
```kotlin
listen<PlayerMoveEvent> { event ->
    // Avoid heavy operations in frequently called events
    if (event.hasChangedBlock()) { // Only check when player changes blocks
        checkPlayerRegion(event.player)
    }
}
```

### 4. Use Event Cancellation Wisely
```kotlin
listen<PlayerInteractEvent> { event ->
    if (shouldCancelInteraction(event)) {
        event.isCancelled = true
        event.player.sendMessage("Interaction cancelled!")
        return@listen // Exit early after cancellation
    }
    
    // Continue with normal logic
    handleInteraction(event)
}
```

## Troubleshooting

### Events Not Firing
- Ensure the event class is imported correctly
- Check that the plugin is properly registered
- Verify event priority conflicts

### Performance Issues
- Avoid heavy operations in frequently called events (like PlayerMoveEvent)
- Use event filtering to reduce unnecessary processing
- Profile your event handlers to identify bottlenecks

### Memory Leaks
- Clean up collections in event handlers
- Remove event listeners when no longer needed
- Avoid storing references to event objects

## Related Topics

- [Plugin Development](../core/plugin-development.md) - Core KPaper concepts
- [Extensions](extensions.md) - Kotlin extensions for events
- [Coroutines](coroutines.md) - Async event handling
- [Examples](../examples/common-patterns.md) - Common event patterns