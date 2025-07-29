# Inventory & GUI System

KPaper provides a comprehensive inventory and GUI system that makes creating interactive menus, item builders, and custom inventories simple and intuitive.

## Item Building

### Basic Item Creation

The `ItemBuilder` class provides a fluent API for creating custom items:

```kotlin
import cc.modlabs.kpaper.inventory.ItemBuilder
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

// Basic item
val sword = ItemBuilder(Material.DIAMOND_SWORD)
    .name("&6Legendary Sword")
    .lore(
        "&7A powerful weapon forged",
        "&7by ancient smiths.",
        "",
        "&eDamage: &c+15",
        "&eSharpness: &aV"
    )
    .enchant(Enchantment.SHARPNESS, 5)
    .enchant(Enchantment.UNBREAKING, 3)
    .build()
```

### Advanced Item Features

```kotlin
// Item with custom model data and flags
val customItem = ItemBuilder(Material.STICK)
    .name("&bMagic Wand")
    .lore("&7Channel your inner wizard!")
    .customModelData(12345)
    .hideEnchants()
    .hideAttributes()
    .unbreakable(true)
    .glowing(true) // Adds enchantment glow without enchantment
    .build()

// Player head with custom texture
val playerHead = ItemBuilder(Material.PLAYER_HEAD)
    .name("&a${player.name}'s Head")
    .skullOwner(player)
    .build()

// Item with NBT data
val nbtItem = ItemBuilder(Material.PAPER)
    .name("&eQuest Item")
    .nbt("quest_id", "dragon_slayer")
    .nbt("quest_progress", 0)
    .build()
```

### Item Click Handling

Create items with built-in click handlers:

```kotlin
val interactiveItem = ItemBuilder(Material.COMPASS)
    .name("&eTeleporter")
    .lore("&7Click to teleport home!")
    .onClick { player, event ->
        player.teleport(player.bedSpawnLocation ?: player.world.spawnLocation)
        player.sendMessage("&aTeleported home!")
    }
    .build()
```

## Simple GUIs

### Basic GUI Creation

Create simple inventory GUIs with click handlers:

```kotlin
import cc.modlabs.kpaper.inventory.simple.simpleGUI

fun openShopGUI(player: Player) {
    val gui = simpleGUI("Shop", 27) {
        
        // Weapons section
        item(10, ItemBuilder(Material.DIAMOND_SWORD)
            .name("&cWeapons")
            .lore("&7Click to browse weapons")
            .build()) {
            openWeaponsShop(player)
        }
        
        // Tools section
        item(12, ItemBuilder(Material.DIAMOND_PICKAXE)
            .name("&bTools")
            .lore("&7Click to browse tools")
            .build()) {
            openToolsShop(player)
        }
        
        // Blocks section
        item(14, ItemBuilder(Material.GRASS_BLOCK)
            .name("&aBlocks")
            .lore("&7Click to browse blocks")
            .build()) {
            openBlocksShop(player)
        }
        
        // Close button
        item(22, ItemBuilder(Material.BARRIER)
            .name("&cClose")
            .build()) {
            player.closeInventory()
        }
    }
    
    player.openInventory(gui)
}
```

### Paginated GUIs

Create GUIs with multiple pages:

```kotlin
class PaginatedShopGUI(private val items: List<ShopItem>) {
    private var currentPage = 0
    private val itemsPerPage = 21
    
    fun open(player: Player) {
        val totalPages = (items.size + itemsPerPage - 1) / itemsPerPage
        
        val gui = simpleGUI("Shop - Page ${currentPage + 1}/$totalPages", 54) {
            
            // Display items for current page
            val startIndex = currentPage * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, items.size)
            
            for (i in startIndex until endIndex) {
                val slot = i - startIndex
                val item = items[i]
                
                item(slot, ItemBuilder(item.material)
                    .name("&e${item.name}")
                    .lore(
                        "&7Price: &a$${item.price}",
                        "&7Stock: &b${item.stock}",
                        "",
                        "&eClick to purchase!"
                    )
                    .build()) {
                    purchaseItem(player, item)
                }
            }
            
            // Navigation buttons
            if (currentPage > 0) {
                item(45, ItemBuilder(Material.ARROW)
                    .name("&7← Previous Page")
                    .build()) {
                    currentPage--
                    open(player) // Reopen with new page
                }
            }
            
            if (currentPage < totalPages - 1) {
                item(53, ItemBuilder(Material.ARROW)
                    .name("&7Next Page →")
                    .build()) {
                    currentPage++
                    open(player)
                }
            }
            
            // Close button
            item(49, ItemBuilder(Material.BARRIER)
                .name("&cClose")
                .build()) {
                player.closeInventory()
            }
        }
        
        player.openInventory(gui)
    }
}
```

## Advanced GUI System (KGUI)

### Creating Complex GUIs

For more advanced GUIs, use the KGUI system:

```kotlin
import cc.modlabs.kpaper.inventory.KGUI
import cc.modlabs.kpaper.inventory.GUI

class AdvancedShopGUI : KGUI() {
    
    override val title = "Advanced Shop"
    override val size = 54
    
    private var selectedCategory: ShopCategory = ShopCategory.WEAPONS
    private val playerMoney = mutableMapOf<UUID, Int>()
    
    override fun build(player: Player): GUI {
        return GUI(title, size) {
            
            // Category selection
            drawCategorySelector(player, this)
            
            // Item display based on selected category
            drawItems(player, this)
            
            // Player info section
            drawPlayerInfo(player, this)
            
            // Action buttons
            drawActionButtons(player, this)
        }
    }
    
    private fun drawCategorySelector(player: Player, gui: GUI) {
        ShopCategory.values().forEachIndexed { index, category ->
            val slot = index + 9
            
            gui.item(slot, ItemBuilder(category.icon)
                .name(if (category == selectedCategory) "&a${category.displayName}" else "&7${category.displayName}")
                .lore(
                    "&7Items: &b${category.items.size}",
                    if (category == selectedCategory) "&a► Selected" else "&eClick to select"
                )
                .glowing(category == selectedCategory)
                .build()) {
                selectedCategory = category
                refresh(player)
            }
        }
    }
    
    private fun drawItems(player: Player, gui: GUI) {
        val items = selectedCategory.items
        val startSlot = 18
        
        items.take(21).forEachIndexed { index, item ->
            val slot = startSlot + index
            
            gui.item(slot, ItemBuilder(item.material)
                .name("&e${item.name}")
                .lore(
                    "&7Price: &a$${item.price}",
                    "&7Description:",
                    *item.description.map { "&8$it" }.toTypedArray(),
                    "",
                    if (canAfford(player, item)) "&aClick to purchase!" else "&cNot enough money!"
                )
                .build()) {
                
                if (canAfford(player, item)) {
                    purchaseItem(player, item)
                    refresh(player)
                } else {
                    player.sendMessage("&cYou don't have enough money!")
                }
            }
        }
    }
    
    private fun drawPlayerInfo(player: Player, gui: GUI) {
        val money = playerMoney[player.uniqueId] ?: 0
        
        gui.item(4, ItemBuilder(Material.PLAYER_HEAD)
            .name("&a${player.name}")
            .lore(
                "&7Money: &a$$money",
                "&7Purchases: &b${getPurchaseCount(player)}",
                "&7Member since: &e${getJoinDate(player)}"
            )
            .skullOwner(player)
            .build())
    }
    
    private fun drawActionButtons(player: Player, gui: GUI) {
        // Sell items button
        gui.item(48, ItemBuilder(Material.EMERALD)
            .name("&aSell Items")
            .lore("&7Click to sell items from your inventory")
            .build()) {
            openSellGUI(player)
        }
        
        // Transaction history
        gui.item(50, ItemBuilder(Material.BOOK)
            .name("&eTransaction History")
            .lore("&7View your recent purchases")
            .build()) {
            openTransactionHistory(player)
        }
    }
}
```

### GUI Animation

Add animations to your GUIs:

```kotlin
class AnimatedLoadingGUI : KGUI() {
    override val title = "Loading..."
    override val size = 27
    
    private var animationFrame = 0
    private val loadingSlots = listOf(10, 11, 12, 14, 15, 16)
    
    override fun build(player: Player): GUI {
        return GUI(title, size) {
            
            // Static background
            fillBorder(ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build())
            
            // Animated loading indicator
            loadingSlots.forEachIndexed { index, slot ->
                val isActive = index == (animationFrame % loadingSlots.size)
                
                item(slot, ItemBuilder(if (isActive) Material.LIME_STAINED_GLASS_PANE else Material.GRAY_STAINED_GLASS_PANE)
                    .name(if (isActive) "&aLoading..." else " ")
                    .build())
            }
            
            // Start animation
            animateLoading(player)
        }
    }
    
    private fun animateLoading(player: Player) {
        taskRunLater(10) {
            if (player.openInventory.topInventory.holder == this) {
                animationFrame++
                refresh(player)
                
                // Continue animation or complete loading
                if (animationFrame < 20) {
                    animateLoading(player)
                } else {
                    // Loading complete, open actual GUI
                    openMainGUI(player)
                }
            }
        }
    }
}
```

## Anvil GUIs

Create custom anvil interfaces for text input:

```kotlin
import cc.modlabs.kpaper.inventory.AnvilGUI

fun openNameInputGUI(player: Player) {
    AnvilGUI.builder()
        .title("Enter a name:")
        .itemLeft(ItemBuilder(Material.PAPER)
            .name("Enter name here...")
            .build())
        .onClick { slot, snapshot ->
            if (slot != AnvilGUI.Slot.OUTPUT) {
                return@onClick Collections.emptyList()
            }
            
            val inputText = snapshot.text
            
            if (inputText.isBlank()) {
                return@onClick listOf(AnvilGUI.ResponseAction.replaceInputText("Name cannot be empty!"))
            }
            
            if (inputText.length > 16) {
                return@onClick listOf(AnvilGUI.ResponseAction.replaceInputText("Name too long!"))
            }
            
            // Process the input
            processNameInput(player, inputText)
            
            return@onClick listOf(AnvilGUI.ResponseAction.close())
        }
        .plugin(this)
        .open(player)
}
```

## Inventory Extensions

### Inventory Utilities

```kotlin
import cc.modlabs.kpaper.extensions.*

// Check if inventory has space
if (player.inventory.hasSpace()) {
    player.inventory.addItem(newItem)
} else {
    player.sendMessage("Your inventory is full!")
}

// Remove specific amount of items
player.inventory.removeItem(Material.DIAMOND, 5)

// Get available space
val spaces = player.inventory.availableSpace()
player.sendMessage("You have $spaces free inventory slots")

// Clear specific item types
player.inventory.clearItems(Material.DIRT, Material.COBBLESTONE)
```

### Custom Inventory Serialization

Save and load inventory contents:

```kotlin
import cc.modlabs.kpaper.inventory.InventorySerializer

class PlayerDataManager {
    
    fun savePlayerInventory(player: Player) {
        val serialized = InventorySerializer.serialize(player.inventory)
        // Save to database or file
        saveToDatabase(player.uniqueId, "inventory", serialized)
    }
    
    fun loadPlayerInventory(player: Player) {
        val serialized = loadFromDatabase(player.uniqueId, "inventory")
        if (serialized != null) {
            val inventory = InventorySerializer.deserialize(serialized)
            player.inventory.contents = inventory.contents
        }
    }
    
    fun createBackup(player: Player): String {
        return InventorySerializer.serialize(player.inventory)
    }
    
    fun restoreFromBackup(player: Player, backup: String) {
        val inventory = InventorySerializer.deserialize(backup)
        player.inventory.contents = inventory.contents
        player.updateInventory()
    }
}
```

## MineSkin Integration

Use custom player head textures:

```kotlin
import cc.modlabs.kpaper.inventory.mineskin.MineSkinFetcher

fun createCustomHead(textureUrl: String): ItemStack {
    val texture = MineSkinFetcher.fetchTexture(textureUrl)
    
    return ItemBuilder(Material.PLAYER_HEAD)
        .name("&6Custom Head")
        .texture(texture)
        .build()
}

// Pre-defined texture heads
fun createZombieHead(): ItemStack {
    return ItemBuilder(Material.PLAYER_HEAD)
        .name("&2Zombie Head")
        .texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTZmYzg1NGJiODRjZjRiNzY5NzI5Nzk3M2UwMmI3OWJjNzBmYzNjODM4MjNmODA4ZjY4ZmZmYjQ2ZjkwNSJ9fX0=")
        .build()
}
```

## GUI Best Practices

### 1. Consistent Layout

```kotlin
// Create reusable layout functions
fun GUI.drawBorder(item: ItemStack) {
    val borderSlots = listOf(0, 1, 2, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 51, 52, 53)
    borderSlots.forEach { slot ->
        item(slot, item)
    }
}

fun GUI.drawNavigationButtons() {
    // Back button
    item(48, ItemBuilder(Material.ARROW)
        .name("&7← Back")
        .build()) {
        openPreviousGUI(player)
    }
    
    // Close button
    item(49, ItemBuilder(Material.BARRIER)
        .name("&cClose")
        .build()) {
        player.closeInventory()
    }
    
    // Help button
    item(50, ItemBuilder(Material.BOOK)
        .name("&eHelp")
        .lore("&7Click for help with this menu")
        .build()) {
        showHelp(player)
    }
}
```

### 2. Error Handling

```kotlin
fun safeGUIOpen(player: Player, guiFactory: () -> Inventory) {
    try {
        val gui = guiFactory()
        player.openInventory(gui)
    } catch (exception: Exception) {
        logger.error("Failed to open GUI for ${player.name}", exception)
        player.sendMessage("&cFailed to open menu. Please try again.")
        
        // Fallback to a simple error GUI
        val errorGUI = simpleGUI("Error", 9) {
            item(4, ItemBuilder(Material.BARRIER)
                .name("&cAn error occurred")
                .lore("&7Please contact an administrator")
                .build())
        }
        player.openInventory(errorGUI)
    }
}
```

### 3. Performance Optimization

```kotlin
// Cache frequently used items
object GUIItems {
    val BORDER_ITEM = ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
        .name(" ")
        .build()
    
    val CLOSE_BUTTON = ItemBuilder(Material.BARRIER)
        .name("&cClose")
        .build()
    
    val BACK_BUTTON = ItemBuilder(Material.ARROW)
        .name("&7← Back")
        .build()
}

// Reuse GUI instances when possible
class GUIManager {
    private val guiCache = mutableMapOf<String, KGUI>()
    
    fun getGUI(type: String): KGUI {
        return guiCache.computeIfAbsent(type) { createGUI(type) }
    }
}
```

## Troubleshooting

### Common Issues

**GUI not opening:**
- Check if player is online
- Verify inventory isn't already open
- Ensure GUI size is valid (multiple of 9, max 54)

**Click events not working:**
- Confirm event listeners are registered
- Check for event cancellation in other plugins
- Verify correct slot indices

**Items not displaying:**
- Check for null items
- Verify slot numbers are within bounds
- Ensure ItemBuilder creates valid items

**Memory leaks:**
- Clean up GUI references when players disconnect
- Avoid storing player references in static collections
- Properly close inventories on plugin disable

## Related Topics

- [Events](events.md) - Handling inventory events
- [Extensions](extensions.md) - Inventory-related extensions  
- [Utilities](utilities.md) - Helper functions for items
- [Examples](../examples/common-patterns.md) - GUI examples and patterns