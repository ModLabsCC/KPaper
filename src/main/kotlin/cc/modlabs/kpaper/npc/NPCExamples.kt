package cc.modlabs.kpaper.npc

import cc.modlabs.kpaper.inventory.ItemBuilder
import cc.modlabs.kpaper.main.KPlugin
import dev.fruxz.stacked.text
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MainHand

/**
 * Example usage of the NPC API.
 * This file demonstrates various ways to create and use NPCs.
 */

// ============================================================================
// BASIC EXAMPLES
// ============================================================================

/**
 * Example 1: Create a simple NPC with just a name
 */
fun createSimpleNPCExample(location: Location): NPC {
    return createSimpleNPC(location, "&aGuard")
}

/**
 * Example 2: Create an NPC using the builder pattern
 */
fun BuilderPatternExample(location: Location): NPC {
    return location.createNPC {
        name("&6Shop Keeper")
        description("&7Click to open shop!")
        mainHand(MainHand.RIGHT)
        immovable(true)
        helmet(ItemStack(Material.DIAMOND_HELMET))
        chestplate(ItemStack(Material.DIAMOND_CHESTPLATE))
    }
}

/**
 * Example 2b: NPC with custom name visibility control
 */
fun customNameVisibilityExample(location: Location): NPC {
    return location.createNPC {
        name("&eHidden Name NPC")
        description("&7My name is hidden!")
        // Hide the custom name
        customNameVisible(false)
    }
}

/**
 * Example 2c: NPC without description (description will be hidden automatically)
 */
fun noDescriptionExample(location: Location): NPC {
    return location.createNPC {
        name("&aSimple NPC")
        // No description provided - will be automatically hidden
        customNameVisible(true)
    }
}

/**
 * Example 2d: NPC with gravity disabled (floating NPC)
 */
fun floatingNPCExample(location: Location): NPC {
    return location.createNPC {
        name("&bFloating NPC")
        description("&7I float in the air!")
        gravity(false) // Disable gravity - NPC will float
        immovable(true)
    }
}

/**
 * Example 16: NPC with a simple conversation
 */
fun conversationNPCExample(location: Location): NPC {
    return location.createNPC {
        name("&eMerchant")
        description("&7Right-click to talk!")
        conversation {
            prompt("greeting", text("&aHello! Welcome to my shop. What would you like to buy?"))
            prompt("response", text("&ePlease type 'yes' or 'no'"), validator = { input ->
                input.equals("yes", ignoreCase = true) || input.equals("no", ignoreCase = true)
            })
            onComplete { npc, player, inputs ->
                val response = inputs["response"]?.lowercase()
                if (response == "yes") {
                    player.sendMessage(text("&aGreat! Here's your item."))
                    // Give item to player, open shop GUI, etc.
                } else {
                    player.sendMessage(text("&cMaybe next time!"))
                }
            }
        }
    }
}

/**
 * Example 17: NPC with a multi-step conversation
 */
fun multiStepConversationExample(location: Location): NPC {
    return location.createNPC {
        name("&bQuest Giver")
        description("&7Right-click to start a quest!")
        conversation {
            prompt("start", text("&aWelcome, adventurer! Are you ready for a quest?"))
            prompt("name", text("&eWhat is your name?"), 
                validator = { input -> input.isNotBlank() && input.length >= 2 },
                onInput = { npc, player, input ->
                    // This is called when player provides input for this prompt
                    if (input.length > 10) {
                        player.sendMessage(text("&eThat's a long name!"))
                    }
                }
            )
            prompt("confirm", text("&aNice to meet you! Ready to begin? (yes/no)"), validator = { input ->
                input.equals("yes", ignoreCase = true) || input.equals("no", ignoreCase = true)
            })
            onComplete { npc, player, inputs ->
                val playerName = inputs["name"] ?: "Unknown"
                val confirmed = inputs["confirm"]?.equals("yes", ignoreCase = true) ?: false
                
                if (confirmed) {
                    player.sendMessage(text("&aQuest started! Good luck, $playerName!"))
                    // Start quest logic here
                } else {
                    player.sendMessage(text("&cQuest cancelled. Come back when you're ready!"))
                }
            }
            escapeSequence("quit") // Custom escape sequence
            timeout(60) // 60 second timeout
        }
    }
}

/**
 * Example 18: NPC with conversation set programmatically
 */
fun programmaticConversationExample(location: Location): NPC {
    val npc = location.createNPC {
        name("&dProgrammatic NPC")
        description("&7Right-click me!")
    }
    
    // Set conversation after creation
    npc.setConversation {
        prompt("question1", text("&aWhat's your favorite color?"))
        prompt("question2", text("&eDo you like Minecraft? (yes/no)"), validator = { input ->
            input.equals("yes", ignoreCase = true) || input.equals("no", ignoreCase = true)
        })
        onComplete { npc, player, inputs ->
            val color = inputs["question1"] ?: "unknown"
            val likesMinecraft = inputs["question2"]?.equals("yes", ignoreCase = true) ?: false
            
            player.sendMessage(text("&aYour favorite color is $color!"))
            if (likesMinecraft) {
                player.sendMessage(text("&aGreat! I like Minecraft too!"))
            } else {
                player.sendMessage(text("&cThat's okay, everyone has different tastes!"))
            }
        }
    }
    
    return npc
}

/**
 * Example 3: Create an NPC with custom equipment using ItemBuilder
 */
fun customEquipmentExample(location: Location): NPC {
    val customSword = ItemBuilder(Material.DIAMOND_SWORD) {
        name("&6Legendary Blade")
        lore("&7A powerful weapon")
    }.build()
    
    return location.createNPC {
        name("&c&lLegendary Warrior")
        description("&7A powerful guardian")
        itemInMainHand(customSword)
        helmet(ItemStack(Material.DIAMOND_HELMET))
        chestplate(ItemStack(Material.DIAMOND_CHESTPLATE))
        leggings(ItemStack(Material.DIAMOND_LEGGINGS))
        boots(ItemStack(Material.DIAMOND_BOOTS))
    }
}

/**
 * Example 4: Create an NPC with custom skin parts
 * Note: Skin parts configuration depends on the Paper API version.
 * The DSL block receives a SkinParts.Mutable object that you can configure.
 */
fun SkinPartsExample(location: Location): NPC {
    return location.createNPC {
        name("Mysterious Figure")
        skinParts {  }
        // Configure skin parts - the exact properties depend on your Paper version
        // skinParts {
        //     // Configure visible parts here
        // }
    }
}

// ============================================================================
// WALKING EXAMPLES
// ============================================================================

/**
 * Example 5: Create a walking NPC that moves to a location
 */
fun WalkingNPCExample(location: Location, targetLocation: Location): NPC {
    val npc = location.createNPC {
        name("&aWandering NPC")
        description("&7I'm walking around!")
        immovable(false) // Must be false for walking
    }
    
    // Make the NPC walk to a target location
    npc.walkTo(targetLocation)
    
    return npc
}

/**
 * Example 6: Create a patrolling guard that follows a looping path
 */
fun PatrollingGuardExample(spawnLocation: Location): NPC {
    // Define patrol points in a square pattern
    val patrolPoints = listOf(
        spawnLocation.clone().add(10.0, 0.0, 0.0),
        spawnLocation.clone().add(10.0, 0.0, 10.0),
        spawnLocation.clone().add(0.0, 0.0, 10.0),
        spawnLocation.clone() // Will loop back to first point
    )
    
    val npc = spawnLocation.createNPC {
        name("&cGuard")
        description("&7On patrol")
        helmet(ItemStack(Material.IRON_HELMET))
        chestplate(ItemStack(Material.IRON_CHESTPLATE))
        leggings(ItemStack(Material.IRON_LEGGINGS))
        boots(ItemStack(Material.IRON_BOOTS))
        itemInMainHand(ItemStack(Material.IRON_SWORD))
        immovable(false)
    }
    
    // Start patrolling - will loop continuously
    npc.startPatrolling(patrolPoints)
    
    return npc
}

/**
 * Example 7: Control NPC walking (pause, resume, teleport)
 */
fun WalkingControlExample(npc: NPC, newLocation: Location) {
    // Pause walking
    npc.pauseWalking()
    
    // Resume walking
    npc.resumeWalking()
    
    // Teleport instantly (without walking)
    npc.teleport(newLocation)
    
    // Start walking to a new location
    npc.walkTo(newLocation)
}

/**
 * Example 7b: Control NPC patrolling (start, pause, resume, stop)
 */
fun PatrollingControlExample(npc: NPC, patrolPoints: List<Location>) {
    // Start patrolling a path (will loop continuously)
    npc.startPatrolling(patrolPoints)
    
    // Pause patrolling (NPC stops but remains in patrol mode)
    npc.pausePatrolling()
    
    // Resume patrolling (continues from where it paused)
    npc.resumePatrolling()
    
    // Stop patrolling completely (exits patrol mode)
    npc.stopPatrolling()
}

/**
 * Example 7c: NPC with pathfinding enabled (navigates around obstacles and jumps)
 * Pathfinding is enabled by default, but you can control it explicitly.
 */
fun PathfindingNPCExample(location: Location, targetLocation: Location): NPC {
    val npc = location.createNPC {
        name("&bSmart Navigator")
        description("&7I can navigate around obstacles!")
        immovable(false) // Must be false for walking
    }
    
    // Pathfinding is enabled by default, but you can explicitly enable it
    npc.setPathfindingEnabled(true)
    
    // The NPC will automatically:
    // - Navigate around obstacles using A* pathfinding
    // - Jump over 1-block high obstacles
    // - Find the best path to the target
    npc.walkTo(targetLocation)
    
    return npc
}

/**
 * Example 7d: NPC with pathfinding disabled (moves directly, ignoring obstacles)
 */
fun DirectPathNPCExample(location: Location, targetLocation: Location): NPC {
    val npc = location.createNPC {
        name("&cDirect Walker")
        description("&7I walk straight to my target!")
        immovable(false)
    }
    
    // Disable pathfinding - NPC will move directly towards target
    // This is useful for simple scenarios or when you want direct movement
    npc.setPathfindingEnabled(false)
    
    npc.walkTo(targetLocation)
    
    return npc
}

/**
 * Example 7e: NPC that navigates complex terrain with obstacles
 */
fun ComplexTerrainNPCExample(spawnLocation: Location): NPC {
    val npc = spawnLocation.createNPC {
        name("&eMountain Climber")
        description("&7I can climb and jump!")
        immovable(false)
    }
    
    // Pathfinding is enabled by default
    // The NPC can:
    // - Jump over 1-block high obstacles
    // - Navigate around walls and barriers
    // - Find paths through complex terrain
    // - Drop down up to 3 blocks safely
    
    // Example: Walk to a location that requires navigating around obstacles
    val targetLocation = spawnLocation.clone().add(20.0, 5.0, 20.0)
    npc.walkTo(targetLocation)
    
    return npc
}

// ============================================================================
// ADVANCED EXAMPLES
// ============================================================================

/**
 * Example 8: NPC that approaches a player
 */
class FriendlyNPC(private val spawnLocation: Location) {
    private val npc: NPC
    
    init {
        npc = spawnLocation.createNPC {
            name("&aFriendly Guide")
            description("&7I'll help you explore!")
            immovable(false)
        }
    }
    
    fun approachPlayer(player: Player) {
        val playerLocation = player.location
        npc.walkTo(playerLocation)
    }
    
    fun followPlayer(player: Player, distance: Double = 3.0) {
        // Calculate a location near the player
        val direction = player.location.direction
        val followLocation = player.location.clone()
            .subtract(direction.multiply(distance))
        
        npc.walkTo(followLocation)
    }
    
    fun getNPC(): NPC = npc
}

/**
 * Example 9: NPC Manager for handling multiple NPCs
 */
class NPCManager {
    private val npcs = mutableMapOf<String, NPC>()
    
    fun createNPC(id: String, location: Location, config: NPCBuilder.() -> Unit): NPC {
        val npc = location.createNPC(config)
        npcs[id] = npc
        return npc
    }
    
    fun getNPC(id: String): NPC? = npcs[id]
    
    fun removeNPC(id: String) {
        npcs[id]?.getEntity()?.remove()
        npcs.remove(id)
    }
    
    fun getAllNPCs(): Collection<NPC> = npcs.values
    
    fun makeAllWalkTo(location: Location) {
        npcs.values.forEach { it.walkTo(location) }
    }
    
    fun clearAll() {
        npcs.values.forEach { it.getEntity()?.remove() }
        npcs.clear()
    }
}

/**
 * Example 10: Complete shop NPC setup with event handling
 */
class ShopNPC(private val location: Location) {
    private val npc: NPC
    
    init {
        npc = location.createNPC {
            name("&6&lShop Keeper")
            description("&7Right-click to browse items!")
            helmet(ItemStack(Material.LEATHER_HELMET))
            chestplate(ItemStack(Material.LEATHER_CHESTPLATE))
            immovable(true)
            mainHand(MainHand.RIGHT)
        }
        
        // Make NPC look at nearby players
        npc.setLookAtPlayers(true)
        npc.setProximityRange(8.0) // Look at players within 8 blocks
        
        // Register event handler for right-click
        npc.onEvent(NPCEventType.RIGHT_CLICKED) { event ->
            val player = event.player ?: return@onEvent
            player.sendMessage("&aOpening shop...")
            // Open shop GUI here
        }
    }
    
    fun getNPC(): NPC = npc
    
    fun getLocation(): Location = location
}

/**
 * Example 11: Warrior NPC factory function
 */
fun createWarriorNPC(location: Location, name: String): NPC {
    val sword = ItemBuilder(Material.DIAMOND_SWORD) {
        name("&6Warrior's Blade")
        lore("&7A weapon of legend")
    }.build()
    
    val helmet = ItemBuilder(Material.DIAMOND_HELMET) {
        name("&bHero's Helmet")
    }.build()
    
    return location.createNPC {
        this.name(name)
        description("&7A brave warrior")
        itemInMainHand(sword)
        helmet(helmet)
        chestplate(ItemStack(Material.DIAMOND_CHESTPLATE))
        leggings(ItemStack(Material.DIAMOND_LEGGINGS))
        boots(ItemStack(Material.DIAMOND_BOOTS))
        mainHand(MainHand.RIGHT)
        immovable(true)
    }
}

/**
 * Example 12: Convert existing Mannequin to NPC
 */
fun convertMannequinExample(mannequin: org.bukkit.entity.Mannequin): NPC {
    // If you already have a Mannequin entity, convert it to an NPC
    return mannequin.toNPC()
}

/**
 * Example 13: NPC with profile (player skin)
 */
fun NPCWithProfileExample(
    location: Location,
    profile: MannequinProfile,
    name: String
): NPC {
    return createNPCWithProfile(location, profile, name)
}

/**
 * Example 14: Dynamic NPC that responds to events
 */
class DynamicNPC(private val spawnLocation: Location) {
    private val npc: NPC
    private var currentTarget: Location? = null
    
    init {
        npc = spawnLocation.createNPC {
            name("&eDynamic NPC")
            description("&7I move based on events!")
            immovable(false)
        }
    }
    
    fun onPlayerNearby(player: Player, radius: Double) {
        val distance = npc.getEntity()?.location?.distance(player.location) ?: return
        if (distance <= radius && currentTarget == null) {
            currentTarget = player.location.clone()
            npc.walkTo(currentTarget!!)
        }
    }
    
    fun onPlayerLeft(player: Player) {
        // Return to spawn if player left
        if (currentTarget?.distance(player.location) ?: 0.0 > 10.0) {
            currentTarget = null
            npc.walkTo(spawnLocation)
        }
    }
    
    fun getNPC(): NPC = npc
}

/**
 * Example 15: NPC with multiple event handlers including shift-clicks and proximity
 */
fun NPCWithEventsExample(location: Location): NPC {
    val npc = location.createNPC {
        name("&eInteractive NPC")
        description("&7Try interacting with me!")
        immovable(true)
    }
    
    // Handle right-click
    npc.onEvent(NPCEventType.RIGHT_CLICKED) { event ->
        val player = event.player ?: return@onEvent
        player.sendMessage("&aYou right-clicked me!")
    }
    
    // Handle shift-right-click
    npc.onEvent(NPCEventType.SHIFT_RIGHT_CLICKED) { event ->
        val player = event.player ?: return@onEvent
        player.sendMessage("&eYou shift-right-clicked me!")
    }
    
    // Handle left-click
    npc.onEvent(NPCEventType.LEFT_CLICKED) { event ->
        val player = event.player ?: return@onEvent
        player.sendMessage("&cYou left-clicked me!")
    }
    
    // Handle shift-left-click
    npc.onEvent(NPCEventType.SHIFT_LEFT_CLICKED) { event ->
        val player = event.player ?: return@onEvent
        player.sendMessage("&6You shift-left-clicked me!")
    }
    
    // Handle damage
    npc.onEvent(NPCEventType.DAMAGED) { event ->
        val player = event.player ?: return@onEvent
        player.sendMessage("&cOuch! That hurts!")
        // Prevent damage if needed
    }
    
    // Set proximity range (default is 5.0 blocks)
    npc.setProximityRange(10.0)
    
    // Enable NPC to look at nearby players
    npc.setLookAtPlayers(true)
    
    // Handle player sneaking nearby
    npc.onEvent(NPCEventType.PLAYER_SNEAKING_NEARBY) { event ->
        val player = event.player ?: return@onEvent
        val distance = event.data["distance"] as? Double ?: 0.0
        player.sendMessage("&7NPC: I see you sneaking! (${String.format("%.1f", distance)} blocks away)")
    }
    
    // Handle player punching nearby
    npc.onEvent(NPCEventType.PLAYER_PUNCHING_NEARBY) { event ->
        val player = event.player ?: return@onEvent
        val distance = event.data["distance"] as? Double ?: 0.0
        player.sendMessage("&cNPC: Stop punching! (${String.format("%.1f", distance)} blocks away)")
    }
    
    // Handle patrol events
    npc.onEvent(NPCEventType.PATROL_POINT_REACHED) { event ->
        val location = event.data["location"] as? Location
        // Do something when patrol point is reached
    }
    
    npc.onEvent(NPCEventType.PATROL_CYCLE_COMPLETE) { event ->
        // Do something when patrol cycle completes
    }
    
    return npc
}

/**
 * Example 16: NPC that looks at players (watchful guard)
 */
fun WatchfulGuardExample(location: Location): NPC {
    val npc = location.createNPC {
        name("&cWatchful Guard")
        description("&7I'm watching you...")
        helmet(ItemStack(Material.IRON_HELMET))
        chestplate(ItemStack(Material.IRON_CHESTPLATE))
        immovable(true)
    }
    
    // Enable looking at players within 15 blocks
    npc.setLookAtPlayers(true)
    npc.setProximityRange(15.0)
    
    // Optional: Handle when player enters range
    npc.onEvent(NPCEventType.PLAYER_SNEAKING_NEARBY) { event ->
        val player = event.player ?: return@onEvent
        player.sendMessage("&cGuard: I'm watching you!")
    }
    
    return npc
}

/**
 * Example 17: Complete usage in a plugin class
 */
/*
class MyPlugin : KPlugin() {
    private lateinit var npcManager: NPCManager
    private val shopNPCs = mutableListOf<ShopNPC>()
    private val guards = mutableListOf<NPC>()
    
    override fun startup() {
        npcManager = NPCManager()
        
        // Create shop NPCs
        val shopLocation1 = world.getLocation(100.0, 64.0, 200.0)
        val shop1 = ShopNPC(shopLocation1)
        shopNPCs.add(shop1)
        
        // Create patrolling guards
        val guardSpawn = world.getLocation(50.0, 64.0, 50.0)
        val guard = example6_PatrollingGuard(guardSpawn)
        guards.add(guard)
        
        // Add event handler to guard
        guard.onEvent(NPCEventType.RIGHT_CLICKED) { event ->
            val player = event.player ?: return@onEvent
            player.sendMessage("&cGuard: &7Stay back!")
        }
        
        // Make guard look at nearby players
        guard.setLookAtPlayers(true)
        guard.setProximityRange(10.0)
        
        // Create a friendly NPC
        val friendlyLocation = world.getLocation(0.0, 64.0, 0.0)
        val friendly = FriendlyNPC(friendlyLocation)
    }
    
    override fun shutdown() {
        // Clean up NPCs
        npcManager.clearAll()
        guards.forEach { 
            it.removeAllEventHandlers()
            it.getEntity()?.remove() 
        }
        shopNPCs.forEach { 
            it.getNPC().removeAllEventHandlers()
            it.getNPC().getEntity()?.remove() 
        }
    }
}
*/
