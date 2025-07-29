# Migration Guide

This guide helps you migrate existing Bukkit/Paper plugins to KPaper, taking advantage of its modern Kotlin-first APIs while maintaining compatibility with your existing code.

## Migration Overview

KPaper is designed to be **incrementally adoptable** - you don't need to rewrite everything at once. You can gradually migrate parts of your plugin while keeping existing code functional.

### Migration Strategy Options

1. **Gradual Migration** - Migrate feature by feature over time
2. **New Feature Migration** - Use KPaper for new features only  
3. **Full Migration** - Rewrite the entire plugin (recommended for small plugins)

## Basic Plugin Migration

### From JavaPlugin to KPlugin

**Before (Bukkit/Paper):**
```java
public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("Plugin enabled!");
        
        // Register events
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        
        // Register commands
        Objects.requireNonNull(getCommand("test")).setExecutor(new TestCommand());
        
        // Load configuration
        saveDefaultConfig();
        
        getLogger().info("Plugin fully loaded!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled!");
    }
}
```

**After (KPaper):**
```kotlin
class MyPlugin : KPlugin() {
    
    override fun startup() {
        logger.info("Plugin enabled!")
        
        // Register events (much simpler)  
        listen<PlayerJoinEvent> { event ->
            event.player.sendMessage("Welcome ${event.player.name}!")
        }
        
        // Register commands (through CommandBuilder interface)
        // In practice, this would be done in a CommandBootstrapper:
        /*
        class TestCommand : CommandBuilder {
            override val description = "Test command"
            override fun register() = Commands.literal("test")
                .executes { ctx ->
                    ctx.source.sender.sendMessage("Hello from KPaper!")
                    Command.SINGLE_SUCCESS
                }
                .build()
        }
        */
        
        // Configuration is handled automatically
        
        logger.info("Plugin fully loaded!")
    }
    
    override fun shutdown() {
        logger.info("Plugin disabled!")
    }
}
```

### Key Differences

| Aspect | Bukkit/Paper | KPaper |
|--------|-------------|---------|
| Base Class | `JavaPlugin` | `KPlugin` |
| Enable Method | `onEnable()` | `startup()` |
| Disable Method | `onDisable()` | `shutdown()` |
| Event Registration | Manual `registerEvents()` | `listen<Event>()` |
| Command Registration | Manual executor setup | `CommandBuilder` |
| Configuration | Manual `saveDefaultConfig()` | Automatic handling |

## Event System Migration

### Traditional Event Listeners

**Before:**
```java
public class PlayerListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage("Welcome " + player.getName() + "!");
        
        if (!player.hasPlayedBefore()) {
            giveStarterKit(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.CHEST) {
                if (!event.getPlayer().hasPermission("chests.use")) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("No permission!");
                }
            }
        }
    }
}
```

**After:**
```kotlin
class MyPlugin : KPlugin() {
    override fun startup() {
        // Simple event handling
        listen<PlayerJoinEvent> { event ->
            val player = event.player
            player.sendMessage("Welcome ${player.name}!")
            
            if (!player.hasPlayedBefore()) {
                giveStarterKit(player)
            }
        }
        
        // Event with priority
        listen<PlayerInteractEvent>(priority = EventPriority.HIGH) { event ->
            if (event.action == Action.RIGHT_CLICK_BLOCK) {
                val block = event.clickedBlock
                if (block?.type == Material.CHEST) {
                    if (!event.player.hasPermission("chests.use")) {
                        event.isCancelled = true
                        event.player.sendMessage("No permission!")
                    }
                }
            }
        }
    }
}
```

### Event Handler Classes

If you prefer organized event handlers:

**Before:**
```java
public class CombatListener implements Listener {
    private final MyPlugin plugin;
    
    public CombatListener(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Handle damage
    }
    
    @EventHandler  
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Handle death
    }
}

// In main plugin class:
getServer().getPluginManager().registerEvents(new CombatListener(this), this);
```

**After:**
```kotlin
class CombatEventHandler(private val plugin: MyPlugin) : EventHandler() {
    
    override fun load() {
        listen<EntityDamageEvent> { handleDamage(it) }
        listen<PlayerDeathEvent> { handleDeath(it) }
    }
    
    override fun unload() {
        // Cleanup if needed
    }
    
    private fun handleDamage(event: EntityDamageEvent) {
        // Handle damage
    }
    
    private fun handleDeath(event: PlayerDeathEvent) {
        // Handle death  
    }
}

// In main plugin class:
class MyPlugin : KPlugin() {
    private lateinit var combatHandler: CombatEventHandler
    
    override fun startup() {
        combatHandler = CombatEventHandler(this)
        combatHandler.load()
    }
    
    override fun shutdown() {
        combatHandler.unload()
    }
}
```

## Command System Migration

### Basic Commands

**Before:**
```java
public class TestCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage("Usage: /test <message>");
            return true;
        }
        
        String message = String.join(" ", args);
        player.sendMessage("You said: " + message);
        return true;
    }
}

// In plugin.yml:
commands:
  test:
    description: Test command
    usage: /test <message>

// In main class:
Objects.requireNonNull(getCommand("test")).setExecutor(new TestCommand());
```

**After:**
```kotlin
class MyPlugin : KPlugin() {
    override fun startup() {
        // Command would be registered through CommandBootstrapper:
        /*
        class TestCommand : CommandBuilder {
            override val description = "Test command"
            
            override fun register() = Commands.literal("test")
                .requires { it.sender is Player }
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val message = StringArgumentType.getString(ctx, "message")
                        player.sendMessage("You said: $message")
                        Command.SINGLE_SUCCESS
                    }
                )
                .build()
        }
        */
    }
}
```

### Complex Commands with Sub-commands

**Before:**
```java
public class AdminCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /admin <subcommand>");
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "ban":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /admin ban <player> [reason]");
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found!");
                    return true;
                }
                
                String reason = args.length > 2 ? 
                    String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : 
                    "Banned by admin";
                    
                target.ban(reason, null, sender.getName());
                sender.sendMessage("Banned " + target.getName());
                break;
                
            case "heal":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Players only!");
                    return true;
                }
                
                Player player = (Player) sender;
                player.setHealth(player.getMaxHealth());
                player.sendMessage("Healed!");
                break;
                
            default:
                sender.sendMessage("Unknown subcommand: " + subcommand);
                break;
        }
        
        return true;
    }
}
```

**After:**
```kotlin
class AdminCommand : CommandBuilder {
    override val description = "Admin commands"
    
    override fun register() = Commands.literal("admin")
        .requires { it.sender.hasPermission("admin.use") }
        
        // /admin ban <player> [reason]
        .then(Commands.literal("ban")
            .requires { it.sender.hasPermission("admin.ban") }
            .then(Commands.argument("target", ArgumentTypes.player())
                .executes { ctx ->
                    val sender = ctx.source.sender
                    val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                    val reason = "Banned by admin"
                    
                    target.ban(reason, sender.name)
                    sender.sendMessage("Banned ${target.name}: $reason")
                    Command.SINGLE_SUCCESS
                }
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes { ctx ->
                        val sender = ctx.source.sender
                        val target = ArgumentTypes.player().parse(ctx.input).resolve(ctx.source).singlePlayer
                        val reason = StringArgumentType.getString(ctx, "reason")
                        
                        target.ban(reason, sender.name)
                        sender.sendMessage("Banned ${target.name}: $reason")
                        Command.SINGLE_SUCCESS
                    }
                )
            )
        )
        
        // /admin heal
        .then(Commands.literal("heal")
            .requires { 
                it.sender.hasPermission("admin.heal") && 
                it.sender is Player 
            }
            .executes { ctx ->
                val player = ctx.source.sender as Player
                player.health = player.maxHealth
                player.sendMessage("Healed!")
                Command.SINGLE_SUCCESS
            }
        )
        .build()
}
    }
}
```

## GUI System Migration

### Traditional Inventory GUIs

**Before:**
```java
public class ShopGUI {
    
    public static void openShop(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, "Shop");
        
        // Create items
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName(ChatColor.RED + "Weapons");
        swordMeta.setLore(Arrays.asList(ChatColor.GRAY + "Click to browse weapons"));
        sword.setItemMeta(swordMeta);
        
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(closeMeta);
        
        // Set items
        inventory.setItem(13, sword);
        inventory.setItem(22, close);
        
        player.openInventory(inventory);
    }
}

// Event handler for clicks
@EventHandler
public void onInventoryClick(InventoryClickEvent event) {
    if (!event.getView().getTitle().equals("Shop")) return;
    
    event.setCancelled(true);
    
    if (event.getCurrentItem() == null) return;
    
    Player player = (Player) event.getWhoClicked();
    int slot = event.getSlot();
    
    if (slot == 13) {
        // Open weapons shop
        openWeaponsShop(player);
    } else if (slot == 22) {
        player.closeInventory();
    }
}
```

**After:**
```kotlin
class MyPlugin : KPlugin() {
    
    fun openShop(player: Player) {
        val gui = simpleGUI("Shop", 27) {
            
            item(13, ItemBuilder(Material.DIAMOND_SWORD)
                .name("&cWeapons")
                .lore("&7Click to browse weapons")
                .build()) {
                openWeaponsShop(player)
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

## Configuration Migration

### File-based Configuration

**Before:**
```java
public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        String welcomeMessage = getConfig().getString("messages.welcome", "Welcome!");
        int maxPlayers = getConfig().getInt("limits.max-players", 100);
        boolean enableFeature = getConfig().getBoolean("features.special-feature", false);
        
        // Use configuration values...
    }
}
```

**After:**
```kotlin
class MyPlugin : KPlugin() {
    
    private lateinit var config: MyConfig
    
    override fun startup() {
        config = loadConfiguration() // Automatic config loading
        
        val welcomeMessage = config.messages.welcome
        val maxPlayers = config.limits.maxPlayers  
        val enableFeature = config.features.specialFeature
        
        // Use configuration values...
    }
}

// Type-safe configuration classes
data class MyConfig(
    val messages: Messages = Messages(),
    val limits: Limits = Limits(),
    val features: Features = Features()
) {
    data class Messages(
        val welcome: String = "Welcome!"
    )
    
    data class Limits(
        val maxPlayers: Int = 100
    )
    
    data class Features(
        val specialFeature: Boolean = false
    )
}
```

## Data Storage Migration

### Player Data Management

**Before:**
```java
public class PlayerDataManager {
    private final Map<UUID, PlayerData> playerData = new HashMap<>();
    private final File dataFolder;
    
    public PlayerDataManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }
    
    public void loadPlayerData(Player player) {
        File file = new File(dataFolder, "players/" + player.getUniqueId() + ".yml");
        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            PlayerData data = new PlayerData();
            data.setLevel(config.getInt("level", 1));
            data.setExperience(config.getInt("experience", 0));
            data.setMoney(config.getDouble("money", 100.0));
            playerData.put(player.getUniqueId(), data);
        }
    }
    
    public void savePlayerData(Player player) {
        PlayerData data = playerData.get(player.getUniqueId());
        if (data != null) {
            File file = new File(dataFolder, "players/" + player.getUniqueId() + ".yml");
            file.getParentFile().mkdirs();
            
            FileConfiguration config = new YamlConfiguration();
            config.set("level", data.getLevel());
            config.set("experience", data.getExperience());  
            config.set("money", data.getMoney());
            
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

**After:**
```kotlin
class PlayerDataManager(private val plugin: MyPlugin) {
    private val playerData = mutableMapOf<UUID, PlayerData>()
    
    suspend fun loadPlayerData(player: Player): PlayerData {
        return playerData.computeIfAbsent(player.uniqueId) {
            loadFromFile(it) ?: PlayerData.createDefault(it)
        }
    }
    
    suspend fun savePlayerData(uuid: UUID, data: PlayerData) {
        playerData[uuid] = data
        withContext(Dispatchers.IO) {
            saveToFile(uuid, data)
        }
    }
    
    private suspend fun loadFromFile(uuid: UUID): PlayerData? = withContext(Dispatchers.IO) {
        val file = File(plugin.dataFolder, "players/$uuid.json")
        if (file.exists()) {
            gson.fromJson(file.readText(), PlayerData::class.java)
        } else null
    }
    
    private suspend fun saveToFile(uuid: UUID, data: PlayerData) = withContext(Dispatchers.IO) {
        val file = File(plugin.dataFolder, "players/$uuid.json")
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(data))
    }
}

data class PlayerData(
    val uuid: UUID,
    var level: Int = 1,
    var experience: Int = 0,
    var money: Double = 100.0
) {
    companion object {
        fun createDefault(uuid: UUID) = PlayerData(uuid)
    }
}
```

## Gradual Migration Strategy

### Phase 1: Setup KPaper

1. **Add KPaper Dependency** to your build file
2. **Change Base Class** from JavaPlugin to KPlugin
3. **Update Lifecycle Methods** (onEnable → startup, onDisable → shutdown)
4. **Test Basic Functionality** to ensure everything still works

### Phase 2: Migrate Events

1. **Start with Simple Events** (like PlayerJoinEvent)
2. **Gradually Replace** @EventHandler methods with listen<Event>()
3. **Keep Old Listeners** alongside new ones during transition
4. **Remove Old Listeners** once new ones are tested

### Phase 3: Migrate Commands

1. **Replace Simple Commands** first using CommandBuilder
2. **Update Complex Commands** with sub-command support
3. **Add Argument Validation** and type-safe parsing
4. **Remove Old CommandExecutor** classes

### Phase 4: Modernize GUIs

1. **Replace Manual Inventory Creation** with simpleGUI()
2. **Simplify Click Handling** with inline callbacks
3. **Add Advanced Features** like pagination and animations
4. **Remove Old InventoryClickEvent** handlers

### Phase 5: Enhance with KPaper Features

1. **Add Coroutines** for async operations
2. **Use Extensions** for cleaner code
3. **Implement Advanced Features** like custom events
4. **Optimize Performance** with KPaper utilities

## Common Migration Issues

### Issue 1: Plugin Not Loading

**Problem:** Plugin fails to load after migration to KPlugin

**Solution:**
```kotlin
// Ensure you have the correct plugin.yml main class
main: com.yourpackage.YourPlugin

// And your class extends KPlugin properly
class YourPlugin : KPlugin() {
    // Implementation
}
```

### Issue 2: Events Not Working

**Problem:** Event listeners from old system still firing alongside new ones

**Solution:**
```kotlin
// Remove old listener registration
// getServer().getPluginManager().registerEvents(new OldListener(), this);

// Keep only KPaper event registration
listen<PlayerJoinEvent> { /* new handler */ }
```

### Issue 3: Commands Not Registering

**Problem:** CommandBuilder commands not appearing

**Solution:**
```kotlin
// Commands should be registered through Paper's lifecycle system
// in a CommandBootstrapper, not directly in plugin startup

class CommandBootstrapper : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        val manager = context.lifecycleManager
        
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val myCommand = MyCommand()
            event.registrar().register(myCommand.register(), myCommand.description)
        }
    }
}

class MyCommand : CommandBuilder {
    override val description = "My command"
    override fun register() = Commands.literal("mycommand")
        .executes { ctx ->
            // Command logic
            Command.SINGLE_SUCCESS
        }
        .build()
}
```

### Issue 4: Configuration Not Loading

**Problem:** Configuration values returning defaults

**Solution:**
```kotlin
// Ensure configuration is loaded in startup()
override fun startup() {
    val config = loadConfiguration<MyConfig>() // Load config first
    // Then use config values
}
```

## Best Practices for Migration

### 1. Test Thoroughly

- Test each migrated component individually
- Keep backup of working version
- Use staging server for testing
- Validate all functionality before deployment

### 2. Maintain Compatibility

- Keep old systems running during migration
- Provide fallbacks for critical functionality
- Document breaking changes for users
- Version your plugin appropriately

### 3. Leverage KPaper Features

- Use type-safe configurations
- Implement proper error handling
- Take advantage of async capabilities
- Use extensions for cleaner code

### 4. Plan the Migration

- Start with non-critical features
- Migrate related functionality together
- Update documentation as you go
- Train team members on new patterns

The migration to KPaper is incremental and can be done at your own pace. Start small, test thoroughly, and gradually adopt more KPaper features as you become comfortable with the API.