# Utilities & Extensions

KPaper provides extensive utility functions and Kotlin extensions that make common Minecraft development tasks more intuitive and efficient.

## Utility Functions

### Console and Logging

KPaper enhances console output and logging with color support and structured formatting:

```kotlin
import cc.modlabs.kpaper.util.*

class MyPlugin : KPlugin() {
    override fun startup() {
        // Colored console output
        consoleOutput("&aPlugin started successfully!")
        consoleOutput("&eLoading configuration...")
        consoleOutput("&cWarning: Development mode enabled!")
        
        // Structured logging
        logInfo("Player management system initialized")
        logWarning("Configuration file is missing some values")
        logError("Failed to connect to database", exception)
        
        // Debug logging (only shown when debug is enabled)
        logDebug("Player data: $playerData")
        
        // Success/failure logging
        logSuccess("Successfully connected to database")
        logFailure("Failed to load player data")
    }
}
```

### Text Processing

Powerful text manipulation utilities:

```kotlin
import cc.modlabs.kpaper.util.*

// Color code processing
val coloredText = "&aHello &bWorld&r!".translateColorCodes()
val cleanText = "§aHello §bWorld§r!".stripColorCodes()

// Component building
val component = textComponent {
    text("Welcome ") {
        color = NamedTextColor.GREEN
    }
    text(player.name) {
        color = NamedTextColor.YELLOW
        bold = true
    }
    text("!") {
        color = NamedTextColor.GREEN
    }
}

// Text formatting
val centeredText = "Welcome to the Server".center(50, '=')
// Output: ===========Welcome to the Server===========

val truncatedText = "This is a very long message".truncate(15)
// Output: "This is a ve..."

// List formatting
val items = listOf("apple", "banana", "cherry")
val formatted = items.formatList()
// Output: "apple, banana, and cherry"

// Time formatting
val duration = Duration.ofMinutes(75)
val formatted = duration.formatDuration()
// Output: "1 hour, 15 minutes"
```

### Random Utilities

Enhanced random number generation and selection:

```kotlin
import cc.modlabs.kpaper.util.*

// Random numbers with ranges
val randomInt = randomInt(1, 100) // 1-100 inclusive
val randomDouble = randomDouble(0.5, 1.5) // 0.5-1.5
val randomFloat = randomFloat(0.0f, 10.0f)

// Boolean with probability
val shouldTrigger = randomBoolean(0.25) // 25% chance of true

// Collection utilities
val items = listOf("common", "rare", "legendary")
val randomItem = items.randomElement()
val randomItems = items.randomElements(2) // Get 2 random items

// Weighted selection  
val weightedItems = mapOf(
    "common" to 70,    // 70% chance
    "rare" to 25,      // 25% chance  
    "legendary" to 5   // 5% chance
)
val selectedItem = weightedItems.randomWeighted()

// Shuffling
val shuffled = items.shuffled()
val mutableList = items.toMutableList()
mutableList.shuffle()
```

### Identity and UUID Utilities

Work with player identities more easily:

```kotlin
import cc.modlabs.kpaper.util.*

// Player lookup utilities
val player = findPlayerByName("Steve") // Case-insensitive
val players = findPlayersStartingWith("St") // Returns list
val exactPlayer = getPlayerExact("Steve") // Exact match only

// UUID utilities
val uuid = parseUUID("550e8400-e29b-41d4-a716-446655440000")
val shortUUID = uuid.toShortString() // "550e8400"
val isValidUUID = "invalid-uuid".isValidUUID() // false

// Offline player utilities  
val offlinePlayer = getOfflinePlayerSafe("PlayerName")
val hasPlayed = offlinePlayer?.hasPlayedBefore() ?: false
```

## Bukkit Extensions

### Player Extensions

KPaper adds many convenient extensions to the Player class:

```kotlin
import cc.modlabs.kpaper.extensions.*

fun handlePlayer(player: Player) {
    // Location utilities
    player.teleportSafely(location) // Finds safe spot if needed
    player.teleportToSpawn()
    player.teleportToWorld("world_nether")
    
    // Message sending with formatting
    player.sendFormattedMessage("&aWelcome &e${player.name}&a!")
    player.sendCenteredMessage("=== WELCOME ===")
    player.sendActionBar("&bHealth: ${player.health}/${player.maxHealth}")
    player.sendTitle("&6Welcome!", "&7Enjoy your stay", 10, 70, 20)
    
    // Inventory utilities
    if (player.hasSpace()) {
        player.giveItem(ItemStack(Material.DIAMOND))
    }
    
    player.clearInventory()
    player.removeItem(Material.DIRT, 32) // Remove 32 dirt
    player.hasItem(Material.GOLD_INGOT, 5) // Check if has 5 gold
    
    // Experience utilities
    player.giveExp(100)
    player.setLevel(50)
    player.resetExp()
    
    // Effect utilities
    player.addPotionEffect(PotionEffectType.SPEED, 2, 200) // Speed II for 10 seconds
    player.removePotionEffect(PotionEffectType.POISON)
    player.clearPotionEffects()
    
    // Health and food utilities
    player.heal() // Full heal
    player.heal(5.0) // Heal 5 hearts
    player.feed() // Full hunger
    player.kill() // Kill player
    
    // Permission utilities
    player.hasAnyPermission("admin.use", "mod.use") // Check multiple permissions
    player.addTempPermission("fly.temp", Duration.ofMinutes(5))
    
    // Sound utilities
    player.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
    player.playSound(Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f) // Volume and pitch
    
    // World utilities
    val isInOverworld = player.isInWorld("world")
    val isInNether = player.isInNether()
    val isInEnd = player.isInEnd()
}
```

### Location Extensions

Enhanced location manipulation:

```kotlin
import cc.modlabs.kpaper.extensions.*

fun handleLocation(location: Location) {
    // Distance utilities
    val distance = location.distanceTo(otherLocation)
    val distance2D = location.distance2D(otherLocation) // Ignore Y axis
    val isNear = location.isNear(otherLocation, 5.0) // Within 5 blocks
    
    // Direction utilities
    val direction = location.directionTo(otherLocation)
    val facing = location.getCardinalDirection() // N, S, E, W, NE, etc.
    
    // Block utilities
    val block = location.getBlockSafely() // Null if chunk not loaded
    val topBlock = location.getHighestBlock()
    val safeLocation = location.findSafeSpot() // No lava, void, etc.
    
    // Relative positioning
    val above = location.above(3) // 3 blocks up
    val below = location.below(2) // 2 blocks down
    val forward = location.forward(5) // 5 blocks forward (based on yaw)
    val right = location.right(2) // 2 blocks to the right
    
    // Area utilities
    val nearbyPlayers = location.getNearbyPlayers(10.0)
    val nearbyEntities = location.getNearbyEntities(5.0)
    val nearbyBlocks = location.getNearbyBlocks(3)
    
    // Chunk utilities
    val chunk = location.chunk
    val isChunkLoaded = location.isChunkLoaded()
    val chunkCoords = location.getChunkCoordinates()
    
    // Serialization
    val serialized = location.serialize()
    val deserialized = Location.deserialize(serialized)
    val string = location.toSimpleString() // "world:100,64,200"
}
```

### ItemStack Extensions

More intuitive item manipulation:

```kotlin
import cc.modlabs.kpaper.extensions.*

fun handleItem(item: ItemStack) {
    // Display name utilities
    item.setDisplayName("&6Golden Sword")
    val name = item.getDisplayName() // Returns null if no custom name
    val nameOrType = item.getDisplayNameOrType() // Falls back to material name
    
    // Lore management
    item.setLore("&7A powerful weapon", "&7Forged by ancient smiths")
    item.addLore("&eBonus: +5 Damage")
    item.clearLore()
    val lore = item.getLore() // Returns list of strings
    
    // Enchantment utilities
    item.addEnchant(Enchantment.SHARPNESS, 5)
    item.removeEnchant(Enchantment.UNBREAKING)
    val hasSharpness = item.hasEnchant(Enchantment.SHARPNESS)
    val sharpnessLevel = item.getEnchantLevel(Enchantment.SHARPNESS)
    
    // Durability
    item.setDurability(50)
    item.repair() // Full repair
    item.damage(10) // Damage by 10 points
    val isNearlyBroken = item.getDurabilityPercent() < 0.1
    
    // Comparison utilities
    val isSimilar = item.isSimilarTo(otherItem) // Same type and meta
    val isExact = item.isExactlyEqual(otherItem) // Including amount
    
    // NBT utilities (if supported)
    item.setNBT("custom_id", "legendary_sword")
    val customId = item.getNBT("custom_id")
    val hasNBT = item.hasNBT("custom_id")
    
    // Cloning and modification
    val copy = item.cloneWithAmount(64)
    val renamed = item.cloneWithName("&bIce Sword")
    val enchanted = item.cloneWithEnchant(Enchantment.FIRE_ASPECT, 2)
}
```

### Inventory Extensions

Simplified inventory management:

```kotlin
import cc.modlabs.kpaper.extensions.*

fun handleInventory(inventory: Inventory) {
    // Space checking
    val hasSpace = inventory.hasSpace()
    val spaceFor = inventory.getSpaceFor(ItemStack(Material.STONE))
    val availableSlots = inventory.getAvailableSlots()
    
    // Item management
    inventory.addItemSafely(item) // Returns items that couldn't fit
    inventory.removeItem(Material.DIRT, 32)
    inventory.removeAllItems(Material.COBBLESTONE)
    val count = inventory.countItems(Material.DIAMOND)
    val hasItems = inventory.hasItems(Material.GOLD_INGOT, 5)
    
    // Bulk operations
    inventory.clearItems(Material.DIRT, Material.COBBLESTONE, Material.GRAVEL)
    inventory.replaceItems(Material.DIRT, Material.GRASS_BLOCK)
    
    // Organization
    inventory.sort() // Sort by material and amount
    inventory.compress() // Combine similar stacks
    inventory.shuffle() // Randomize positions
    
    // Filling and patterns
    inventory.fillEmpty(ItemStack(Material.GRAY_STAINED_GLASS_PANE))
    inventory.fillBorder(ItemStack(Material.BLACK_STAINED_GLASS_PANE))
    inventory.fillSlots(listOf(0, 8, 45, 53), borderItem)
    
    // Searching
    val diamondSlots = inventory.findItems(Material.DIAMOND)
    val firstEmpty = inventory.firstEmpty()
    val lastEmpty = inventory.lastEmpty()
    
    // Serialization
    val contents = inventory.serializeContents()
    inventory.deserializeContents(contents)
}
```

### Block Extensions

Enhanced block manipulation:

```kotlin
import cc.modlabs.kpaper.extensions.*

fun handleBlock(block: Block) {
    // Safe state management
    val state = block.getStateSafely<Chest>() // Returns null if not a chest
    block.setType(Material.DIAMOND_BLOCK, updatePhysics = false)
    
    // Placement utilities
    block.placeAgainst(otherBlock) // Place against the side of another block
    val canPlace = block.canPlaceAgainst(Material.STONE)
    
    // Breaking simulation
    val drops = block.getDrops(tool) // Get what items would drop
    val breakTime = block.getBreakTime(player) // How long to break
    val canBreak = block.canBreak(player)
    
    // Direction utilities
    val face = block.getFace(otherBlock) // Which face is touching
    val adjacent = block.getAdjacent() // All 6 adjacent blocks
    val adjacentSame = block.getAdjacentSameType() // Adjacent blocks of same type
    
    // Relative positioning
    val above = block.above()
    val below = block.below()
    val north = block.north()
    val south = block.south()
    val east = block.east()
    val west = block.west()
    
    // Area utilities
    val nearbyBlocks = block.getNearby(3) // All blocks within 3 blocks
    val sameTypeNearby = block.getNearbySameType(5)
    
    // Light utilities
    val lightLevel = block.getLightLevel()
    val isLit = block.isLit() // Light level > 0
    val canSeeSky = block.canSeeSky()
    
    // Physics and updates
    block.updatePhysics()
    block.scheduleUpdate()
    val isSupported = block.isSupported() // Has support below
}
```

### World Extensions

World management utilities:

```kotlin
import cc.modlabs.kpaper.extensions.*

fun handleWorld(world: World) {
    // Player management
    val onlinePlayers = world.getOnlinePlayers()
    val playerCount = world.getPlayerCount()
    world.broadcastMessage("&aServer announcement!")
    world.broadcastToPermission("admin.notify", "&cAdmin alert!")
    
    // Time utilities
    world.setTimeOfDay(TimeOfDay.DAWN)
    world.setTimeOfDay(TimeOfDay.NOON)
    world.setTimeOfDay(TimeOfDay.DUSK)
    world.setTimeOfDay(TimeOfDay.MIDNIGHT)
    val timeOfDay = world.getTimeOfDay()
    
    // Weather utilities
    world.clearWeather()
    world.setStorm(duration = Duration.ofMinutes(5))
    world.setThunder(duration = Duration.ofMinutes(3))
    val isStormy = world.isStormy()
    
    // Location utilities
    val randomLocation = world.getRandomLocation(minY = 60, maxY = 120)
    val safeSpawn = world.findSafeSpawn(around = world.spawnLocation)
    val highestBlock = world.getHighestBlockAt(x, z)
    
    // Chunk utilities
    val loadedChunks = world.getLoadedChunks()
    world.loadChunk(x, z, generate = true)
    world.unloadChunk(x, z, save = true)
    val isChunkLoaded = world.isChunkLoaded(x, z)
    
    // Entity management
    val entities = world.getEntitiesByType<Player>()
    val nearbyEntities = world.getNearbyEntities(location, radius = 10.0)
    world.removeEntitiesByType<Zombie>()
    
    // Game rules
    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
    world.setGameRule(GameRule.KEEP_INVENTORY, true)
    val keepInventory = world.getGameRuleValue(GameRule.KEEP_INVENTORY)
    
    // Border utilities
    val border = world.worldBorder
    border.setSize(1000.0)
    border.setCenter(0.0, 0.0)
    val isInBorder = location.isInWorldBorder()
}
```

## Advanced Extensions

### Collection Extensions

Enhanced collection operations for Minecraft contexts:

```kotlin
import cc.modlabs.kpaper.extensions.*

// Player collections
val players = server.onlinePlayers
val admins = players.withPermission("admin.use")
val survivorPlayers = players.inWorld("world").inGameMode(GameMode.SURVIVAL)
val nearbyPlayers = players.near(location, 10.0)

// Entity filtering
val entities = world.entities
val hostileMobs = entities.ofType<Monster>()
val namedEntities = entities.withCustomName()
val youngAnimals = entities.ofType<Animals>().filter { !it.isAdult }

// Block collections
val blocks = location.getNearbyBlocks(5)
val ores = blocks.ofType(Material.DIAMOND_ORE, Material.GOLD_ORE, Material.IRON_ORE)
val breakable = blocks.filter { it.canBreak(player) }

// Inventory operations
val inventories = listOf(player.inventory, enderChest, someChest)
val totalDiamonds = inventories.countItems(Material.DIAMOND)
val hasSpace = inventories.any { it.hasSpace() }
```

### Event Extensions

Simplified event handling patterns:

```kotlin
import cc.modlabs.kpaper.extensions.*

// Event result shortcuts
listen<PlayerInteractEvent> { event ->
    // Quick cancellation with message
    event.cancelWithMessage("&cYou cannot interact here!")
    
    // Conditional cancellation
    event.cancelIf(!player.hasPermission("interact.use")) {
        "&cNo permission!"
    }
    
    // Cancel and execute
    event.cancelAndExecute {
        player.sendMessage("Interaction blocked!")
        player.playSound(Sound.BLOCK_NOTE_BLOCK_BASS)
    }
}

// Damage event utilities
listen<EntityDamageEvent> { event ->
    val entity = event.entity
    
    // Damage modification
    event.doubleDamage()
    event.halveDamage()
    event.reduceDamage(2.0)
    event.minimumDamage(1.0)
    
    // Quick checks
    if (event.isFallDamage()) {
        event.damage = 0.0
    }
    
    if (event.isPlayerDamage()) {
        val player = entity as Player
        // Handle player damage
    }
}
```

### Component and Text Extensions

Modern text component handling:

```kotlin
import cc.modlabs.kpaper.extensions.*

// Component building
val component = component {
    text("Welcome ") {
        color = NamedTextColor.GREEN
        bold = true
    }
    text(player.name) {
        color = NamedTextColor.YELLOW
        clickEvent = ClickEvent.suggestCommand("/msg ${player.name} ")
        hoverEvent = HoverEvent.showText(Component.text("Click to message"))
    }
    text("!") {
        color = NamedTextColor.GREEN
    }
}

// Legacy color code conversion
val modernComponent = "&aHello &bWorld!".toComponent()
val legacyString = component.toLegacyString()

// Component utilities
val plainText = component.getPlainText()
val isEmpty = component.isEmpty()
val length = component.length()

// Component combining
val combined = component1 + component2
val withNewline = component.appendNewline()
val withPrefix = "[Server] ".toComponent() + component
```

## Best Practices

### 1. Use Extensions Consistently

```kotlin
// Good - using extensions for clarity
fun teleportPlayerSafely(player: Player, location: Location) {
    val safeLocation = location.findSafeSpot()
    if (safeLocation != null) {
        player.teleportSafely(safeLocation)
        player.sendFormattedMessage("&aTeleported safely!")
    } else {
        player.sendFormattedMessage("&cNo safe location found!")
    }
}

// Avoid - verbose vanilla API calls
fun teleportPlayerSafelyOld(player: Player, location: Location) {
    // Lots of manual safety checks and API calls...
}
```

### 2. Leverage Type-Safe Extensions

```kotlin
// Good - type-safe with null handling
val chest = block.getStateSafely<Chest>()
chest?.inventory?.addItem(item)

// Avoid - unsafe casting
val chest = block.state as? Chest
if (chest != null) {
    chest.inventory.addItem(item)
    chest.update()
}
```

### 3. Chain Operations

```kotlin
// Good - method chaining for readability  
server.onlinePlayers
    .withPermission("vip.access")
    .inWorld("lobby")
    .forEach { player ->
        player.sendFormattedMessage("&6VIP message!")
        player.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
    }
```

### 4. Use Utility Functions for Common Tasks

```kotlin
// Good - using utilities
val selectedReward = weightedRewards.randomWeighted()
player.sendCenteredMessage("=== REWARD ===")
logSuccess("Player ${player.name} received reward: $selectedReward")

// Avoid - manual implementation of common patterns
```

## Performance Tips

### 1. Batch Operations

```kotlin
// Good - batch inventory operations
val items = listOf(item1, item2, item3)
inventory.addItemsSafely(items) // Single operation

// Avoid - individual operations in loop
items.forEach { inventory.addItem(it) }
```

### 2. Cache Expensive Operations

```kotlin
class LocationService {
    private val safeLocationCache = mutableMapOf<Location, Location?>()
    
    fun findSafeLocation(location: Location): Location? {
        return safeLocationCache.computeIfAbsent(location) { 
            it.findSafeSpot() 
        }
    }
}
```

### 3. Use Lazy Evaluation

```kotlin
// Good - lazy evaluation for expensive operations
val nearbyPlayers by lazy { location.getNearbyPlayers(50.0) }

// Only computed when first accessed
if (someCondition) {
    nearbyPlayers.forEach { /* process */ }
}
```

The utility functions and extensions in KPaper significantly reduce boilerplate code and make Minecraft plugin development more enjoyable and maintainable. Use them liberally to write cleaner, more expressive code!