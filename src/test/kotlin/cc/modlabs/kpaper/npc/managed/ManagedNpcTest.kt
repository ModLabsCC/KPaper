package cc.modlabs.kpaper.npc.managed

import cc.modlabs.klassicx.tools.minecraft.StringLocation
import cc.modlabs.kpaper.main.featureConfig
import cc.modlabs.kpaper.main.Feature
import cc.modlabs.kpaper.main.PacketEventsSupport
import cc.modlabs.kpaper.skins.SkinEntry
import cc.modlabs.kpaper.skins.SkinLibraryService
import cc.modlabs.kpaper.skins.SkinSource
import com.mojang.brigadier.CommandDispatcher
import io.mockk.*
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlin.random.Random

class ManagedNpcTest {
    @TempDir lateinit var directory: Path
    @AfterEach fun cleanup() = unmockkAll()

    @Test fun `managed NPCs and skin commands are opt in`() {
        val flags = featureConfig {}
        assertFalse(flags.isEnabled(Feature.NPCS))
        assertFalse(flags.isEnabled(Feature.SKINS))
        assertTrue(featureConfig { enableNpcFeatures = true; enableSkinFeatures = true }.isEnabled(Feature.NPCS))
    }

    @Test fun `NPC properties survive restart even with an unloaded world`() {
        val plugin = plugin()
        val storage = NpcStorage(plugin)
        val original = NpcDefinition("guide", point(), name = "<gold>Guide", skin = "named_skin", nameVisible = false,
            lookClose = true, lookCloseRange = 8.0, skinLayers = 64, pose = NpcPose.SITTING,
            animations = setOf(NpcAnimation.TALKING), dialogLines = listOf("<aqua>Hello", "Second line"),
            dialogTrigger = NpcDialogTrigger.BOTH, dialogSelection = NpcDialogSelection.RANDOM, dialogRange = 6.0,
            dialogSound = "project:npc.talk", dialogSoundVolume = 2f, dialogSoundPitch = 0.7f,
            clickSound = "project:npc.click", clickSoundPitch = 1.5f,
            action = "project:shop", actionTrigger = NpcDialogTrigger.PROXIMITY,
            actionData = mapOf("city" to "city-123", "price" to "42"), traits = mapOf("project:guard" to "true"),
            movement = NpcMovementSettings(NpcMovementMode.PATH_PINGPONG, 2.5, 60, 12.0, "market",
                NpcStepMode.STAIRS_SLABS, true, 8.0, listOf(NpcWaypoint("entrance", point(5.0), 3))),
            hologram = "<green>A longer label")
        storage.save(listOf(original))
        assertEquals(original, NpcStorage(plugin).load().single())
        assertTrue(directory.resolve("npcs.yml").toFile().readText().contains("missing_world"))
    }

    @Test fun `malformed NPC config is rejected and retained`() {
        val storage = NpcStorage(plugin())
        storage.save(listOf(NpcDefinition("guide", point())))
        val file = directory.resolve("npcs.yml").toFile()
        val invalid = file.readText().replace("standing", "unknown-pose")
        file.writeText(invalid)
        assertThrows(IllegalArgumentException::class.java) { storage.load() }
        assertEquals(invalid, file.readText())
        file.writeText("npcs: [\n")
        assertThrows(Exception::class.java) { storage.load() }
        assertEquals("npcs: [\n", file.readText())
        file.writeText("npcs: wrong-type\n")
        assertThrows(IllegalArgumentException::class.java) { storage.load() }
    }

    @Test fun `NPC registry rejects duplicate IDs and rolls back when disk save fails`() {
        mockkStatic(Bukkit::class)
        mockkObject(PacketEventsSupport)
        mockkConstructor(PacketNpcRenderer::class, NpcMovementController::class)
        every { Bukkit.isPrimaryThread() } returns true
        every { Bukkit.getWorld(any<String>()) } returns null
        every { Bukkit.getPluginManager() } returns mockk(relaxed = true)
        every { PacketEventsSupport.isActive() } returns true
        every { anyConstructed<PacketNpcRenderer>().start() } just Runs
        every { anyConstructed<PacketNpcRenderer>().stop() } just Runs
        every { anyConstructed<PacketNpcRenderer>().remove(any()) } just Runs
        every { anyConstructed<PacketNpcRenderer>().location(any()) } returns null
        every { anyConstructed<NpcMovementController>().start(any()) } just Runs
        every { anyConstructed<NpcMovementController>().remove(any()) } just Runs
        every { anyConstructed<NpcMovementController>().stop() } just Runs
        val service = NpcService(plugin(), mockk(relaxed = true))
        service.start()
        val original = NpcDefinition("guide", point())
        service.put(original)
        val world = mockk<World>()
        every { world.name } returns "missing_world"
        assertThrows(IllegalArgumentException::class.java) { service.create("GUIDE", Location(world, 0.0, 0.0, 0.0)) }
        assertEquals(original, service.get("guide"))
        val path = directory.resolve("npcs.yml").toFile()
        path.writeText("npcs: [\n")
        assertThrows(Exception::class.java) { service.reload() }
        assertEquals(original, service.get("guide"))
        assertTrue(path.delete()); assertTrue(path.mkdir())
        File(path, "keep").writeText("block replacement")
        assertThrows(Exception::class.java) { service.setName("guide", "Changed") }
        assertThrows(Exception::class.java) { service.delete("guide") }
        assertEquals(original, service.get("guide"))
        service.stop()
        assertTrue(service.ids().isEmpty())
    }

    @Test fun `externally persisted NPCs never read or overwrite the project file`() {
        mockkStatic(Bukkit::class)
        mockkObject(PacketEventsSupport)
        mockkConstructor(PacketNpcRenderer::class, NpcMovementController::class)
        every { Bukkit.isPrimaryThread() } returns true
        every { Bukkit.getWorld(any<String>()) } returns null
        every { Bukkit.getPluginManager() } returns mockk(relaxed = true)
        every { PacketEventsSupport.isActive() } returns true
        every { anyConstructed<PacketNpcRenderer>().start() } just Runs
        every { anyConstructed<PacketNpcRenderer>().stop() } just Runs
        every { anyConstructed<PacketNpcRenderer>().remove(any()) } just Runs
        every { anyConstructed<PacketNpcRenderer>().location(any()) } returns null
        every { anyConstructed<NpcMovementController>().start(any()) } just Runs
        every { anyConstructed<NpcMovementController>().stop() } just Runs
        every { anyConstructed<NpcMovementController>().remove(any()) } just Runs
        val file = directory.resolve("npcs.yml").toFile().apply { writeText("project-owned: [\n") }
        val service = NpcService(plugin(), mockk(relaxed = true), persistDefinitions = false)
        service.start()
        val guide = NpcDefinition("guide", point(), action = "project:guide")
        service.registerAction("project:guide") { _, _, _ -> true }
        service.replaceAll(listOf(guide))
        assertThrows(IllegalArgumentException::class.java) { service.replaceAll(listOf(guide, guide)) }
        assertEquals(guide, service.get("guide"))
        assertEquals(listOf("project:guide"), service.actionKeys())
        service.put(guide.copy(name = "Changed"))
        assertTrue(service.delete("guide"))
        assertThrows(IllegalStateException::class.java) { service.reload() }
        assertEquals("project-owned: [\n", file.readText())
        service.stop()
    }

    @Test fun `holograms preserve long names and multiline project labels`() {
        val npc = NpcDefinition("guide", point(), name = "A long VoidRooms guard name",
            hologram = "A long VoidRooms guard name\nM.E.G. Guard")
        assertEquals("A long VoidRooms guard name", npc.name)
        NpcStorage(plugin()).save(listOf(npc))
        assertEquals(npc, NpcStorage(plugin()).load().single())
        assertThrows(IllegalArgumentException::class.java) { npc.copy(hologram = "bad\u0000label") }
        assertThrows(IllegalArgumentException::class.java) { npc.copy(hologram = "x".repeat(4097)) }
        assertThrows(IllegalArgumentException::class.java) { npc.copy(hologram = null) }
    }

    @Test fun `seated body keeps its authored chair rotation independently of head look`() {
        val authored = NpcRotation(90f, 0f)
        val looking = NpcRotation(-45f, 20f)
        assertEquals(authored, npcBodyRotation(NpcPose.SITTING, authored, looking))
        assertEquals(looking, npcBodyRotation(NpcPose.STANDING, authored, looking))
    }

    @Test fun `moving an NPC to its unchanged home resets its current walking position`() {
        mockkStatic(Bukkit::class)
        mockkObject(PacketEventsSupport)
        mockkConstructor(PacketNpcRenderer::class, NpcMovementController::class)
        val world = mockk<World>()
        every { world.name } returns "missing_world"
        every { world.uid } returns UUID(0, 1)
        every { Bukkit.getWorld(any<String>()) } returns world
        every { Bukkit.isPrimaryThread() } returns true
        every { Bukkit.getPluginManager() } returns mockk(relaxed = true)
        every { PacketEventsSupport.isActive() } returns true
        val packet = PacketNpc(1, UUID(0, 2), npcProfile("guide", "Guide", null, "test"), world.uid,
            0.0, 64.0, 0.0, 0f, 0f, false, 25.0, emptyMap(), false, 16.0, 127,
            NpcPose.STANDING, true, null, null)
        val prepared = mutableListOf<NpcDefinition>()
        every { anyConstructed<PacketNpcRenderer>().prepare(capture(prepared), any()) } returns packet
        every { anyConstructed<PacketNpcRenderer>().start() } just Runs
        every { anyConstructed<PacketNpcRenderer>().stop() } just Runs
        every { anyConstructed<PacketNpcRenderer>().put(any<String>(), any<PacketNpc>()) } just Runs
        every { anyConstructed<PacketNpcRenderer>().location(any()) } returns Location(world, 99.0, 64.0, 0.0)
        every { anyConstructed<NpcMovementController>().start(any()) } just Runs
        every { anyConstructed<NpcMovementController>().stop() } just Runs
        every { anyConstructed<NpcMovementController>().update(any(), any()) } just Runs
        val service = NpcService(plugin(), mockk(relaxed = true), persistDefinitions = false)
        service.start()
        val original = NpcDefinition("guide", point())
        service.put(original)
        service.put(original.copy(name = "Changed"))
        assertEquals(99.0, prepared.last().position.x)
        service.setHome("guide", Location(world, original.position.x, original.position.y, original.position.z,
            original.position.yaw, original.position.pitch))
        assertEquals(original.position.x, prepared.last().position.x)
        verify(exactly = 2) { anyConstructed<NpcMovementController>().update(any(), true) }
        service.stop()
    }

    @Test fun `late furniture seat position and orientation trigger a single refresh`() {
        val furniture = Location(null, 0.5, 64.5, 0.5, 90f, 0f)
        assertTrue(needsNpcSeatRefresh(furniture, 0.5, 64.05, 0.5, 0f))
        assertFalse(needsNpcSeatRefresh(furniture, 0.5, 64.5, 0.5, 90f))
        assertTrue(needsNpcSeatRefresh(furniture, 0.5, 64.5, 0.5, 0f))
        assertTrue(needsNpcSeatRefresh(furniture, 1.5, 64.5, 0.5, 90f))
    }

    @Test fun `skin aliases imports and failed saves keep existing references intact`() {
        mockkStatic(Bukkit::class)
        every { Bukkit.isPrimaryThread() } returns true
        val plugin = plugin()
        val library = SkinLibraryService(plugin)
        library.start()
        library.put(SkinEntry("guard", "texture-value", "signed", "Guard"))
        library.renameId("guard", "meg_guard")
        library.renameId("meg_guard", "guard_v2")
        assertEquals("guard_v2", library.get("guard")?.id)
        library.rename("guard", "MEG Guard")
        assertEquals("guard_v2", library.resolve("meg guard")?.id)
        val imported = library.importMineSkinData("Merchant", "abc123", "merchant-texture", "signature")
        assertEquals(SkinSource.MINESKIN, imported.source)
        assertEquals(imported, library.getOrImportMineSkin("abc123").join())
        val restarted = SkinLibraryService(plugin).apply { start() }
        assertEquals("signed", restarted.get("guard")?.signature)
        assertEquals(imported, restarted.findMineSkin("abc123"))
        val file = directory.resolve("skins.yml").toFile()
        assertTrue(file.delete())
        assertTrue(file.mkdir())
        File(file, "keep").writeText("block replacement")
        assertThrows(Exception::class.java) { library.rename("guard", "Lost rename") }
        assertEquals("MEG Guard", library.get("guard")?.displayName)
        assertTrue(File(file, "keep").exists())
        library.close()
        assertThrows(IllegalStateException::class.java) { library.put(SkinEntry("late", "texture")) }
    }

    @Test fun `corrupt skin reload preserves loaded library`() {
        mockkStatic(Bukkit::class)
        every { Bukkit.isPrimaryThread() } returns true
        val library = SkinLibraryService(plugin()).apply { start(); put(SkinEntry("guard", "texture", "signature")) }
        directory.resolve("skins.yml").toFile().writeText("skins: [\n")
        assertThrows(Exception::class.java) { library.reload() }
        assertEquals("signature", library.get("guard")?.signature)
    }

    @Test fun `multiple imported NPC textures retain distinct names and signatures`() {
        mockkStatic(Bukkit::class)
        every { Bukkit.isPrimaryThread() } returns true
        val library = SkinLibraryService(plugin()).apply { start() }
        val first = library.rememberTexture("texture-a", signature = "signed-a")
        val second = library.rememberTexture("texture-b", signature = "signed-b")
        val signedAgain = library.rememberTexture("texture-a", signature = "signed-new")
        assertEquals(3, library.all().size)
        assertEquals(3, library.all().map { it.displayName }.distinct().size)
        assertEquals(first, library.rememberTexture("texture-a", signature = "signed-a"))
        assertEquals("signed-b", second.signature)
        assertEquals("signed-new", signedAgain.signature)
    }

    @Test fun `packet profiles are stable and isolated across plugins and skins`() {
        val skin = SkinEntry("guard", "texture", "signature")
        val profile = npcProfile("guide", "Guide", skin, "first")
        assertEquals(profile.id, npcProfile("guide", "Guide", skin, "first").id)
        assertNotEquals(profile.id, npcProfile("guide", "Guide", skin, "second").id)
        assertNotEquals(profile.id, npcProfile("guide", "Guide", skin.copy(texture = "changed"), "first").id)
        assertEquals("signature", profile.properties["textures"].single().signature)
        assertTrue(npcProfile("guide", "Guide", null, "first").properties.isEmpty)
        assertNull(npcProfileRemovalDelay(true))
        assertEquals(40L, npcProfileRemovalDelay(false))
    }

    @Test fun `proximity fires once per entry and respects worlds and visibility`() {
        mockkStatic(Bukkit::class)
        val world = mockk<World>()
        val player = mockk<Player>()
        val uuid = UUID.randomUUID()
        val worldId = UUID.randomUUID()
        every { world.uid } returns worldId
        every { player.uniqueId } returns uuid
        every { player.world } returns world
        every { Bukkit.getOnlinePlayers() } returns listOf(player)
        every { Bukkit.getPlayer(uuid) } returns player
        val npc = PacketNpc(1, UUID.randomUUID(), npcProfile("guide", "Guide", null, "test"), worldId,
            0.0, 0.0, 0.0, 0f, 0f, false, 25.0, emptyMap(), true, 16.0, 127,
            NpcPose.STANDING, true, null, null)
        val packets = mockk<NpcPacketSender>(relaxed = true)
        val hooks = NpcHooks()
        var entries = 0
        val runtime = NpcVisibilityRuntime(hooks, mapOf("guide" to npc), { _, _ -> entries++ }, packets)
        every { player.location } returns Location(world, 3.0, 0.0, 0.0)
        repeat(5) { runtime.sync() }
        assertEquals(1, entries)
        verify(exactly = 1) { packets.spawn(player, npc) }
        every { player.location } returns Location(world, 20.0, 0.0, 0.0)
        runtime.sync()
        every { player.location } returns Location(world, 3.0, 0.0, 0.0)
        runtime.sync()
        assertEquals(2, entries)
        hooks.canSee = { _, _ -> false }
        runtime.sync()
        verify { packets.remove(player, npc) }
        hooks.canSee = { _, _ -> true }
        runtime.sync()
        assertEquals(3, entries)
        runtime.reset(player) // respawn or world switch clears client entities
        runtime.sync()
        assertEquals(4, entries)
        every { world.uid } returns UUID.randomUUID()
        runtime.sync()
        assertEquals(4, entries)
    }

    @Test fun `dialog input is bounded and random selections do not repeat`() {
        assertEquals(0x7e.toByte(), updatedNpcSkinLayers(127, "cape", false))
        assertEquals(1.4, npcLabelHeight(NpcPose.SITTING))
        assertEquals(0.6, npcLabelHeight(NpcPose.LYING))
        assertThrows(IllegalArgumentException::class.java) { NpcDefinition("guide", point(), dialogRange = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { normalizedNpcId("../invalid") }
        assertThrows(IllegalArgumentException::class.java) { validatedNpcDialogLine("x".repeat(161)) }
        assertNull(npcDialogComponent("<click:run_command:'/op'>Hello</click>").clickEvent())
        val lines = listOf("A", "B", "C")
        val bag = NpcDialogShuffleBag(Random(42))
        val first = List(3) { bag.next(lines) }
        assertEquals(lines.toSet(), first.toSet())
        assertNotEquals(first.last(), bag.next(lines))
        assertEquals(NpcPathStep(1, -1), nextNpcPathStep(2, 1, 3, false))
        assertEquals("a", selectWeightedWaypoint(listOf(NpcWaypoint("a", point()), NpcWaypoint("b", point(), 9)), "b")?.name)
    }

    @Test fun `actions run once per player and reject spoofed far away clicks`() {
        val world = mockk<World>()
        every { world.name } returns "missing_world"
        val definition = NpcDefinition("guide", point(), action = "project:shop", traits = mapOf("project:merchant" to "true"))
        val renderer = mockk<PacketNpcRenderer>()
        every { renderer.location("guide") } returns Location(world, 0.0, 0.0, 0.0)
        var calls = 0
        val callback: NpcAction = { _, npc, trigger ->
            assertEquals("true", npc.traits["project:merchant"])
            assertEquals(NpcDialogTrigger.CLICK, trigger)
            calls++
            false
        }
        val runtime = NpcInteractionRuntime(plugin(), renderer, { definition }, { key ->
            assertEquals("project:shop", key)
            callback
        })
        val first = player(world)
        val second = player(world)
        runtime.interact(first, "guide", NpcDialogTrigger.CLICK)
        runtime.interact(first, "guide", NpcDialogTrigger.CLICK)
        runtime.interact(second, "guide", NpcDialogTrigger.CLICK)
        assertEquals(2, calls)
        every { first.location } returns Location(world, 100.0, 0.0, 0.0)
        runtime.quit(first.uniqueId)
        runtime.interact(first, "guide", NpcDialogTrigger.CLICK)
        assertEquals(2, calls)
        runtime.clear()
    }

    @Test fun `NPC commands filter suggestions and handle console without player casts`() {
        val service = mockk<NpcService>(relaxed = true)
        every { service.ids() } returns listOf("guide", "merchant")
        val sender = mockk<org.bukkit.command.CommandSender>(relaxed = true)
        every { sender.hasPermission(NpcCommand.PERMISSION) } returns true
        val source = mockk<CommandSourceStack>()
        every { source.sender } returns sender
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        dispatcher.root.addChild(NpcCommand { service }.register())
        assertEquals(listOf("guide"), dispatcher.getCompletionSuggestions(dispatcher.parse("npc here g", source)).get().list.map { it.text })
        assertEquals(0, dispatcher.execute("npc create guide", source))
        verify(exactly = 0) { service.create(any(), any()) }
        assertEquals(1, dispatcher.execute("npc list", source))
        every { sender.hasPermission(NpcCommand.PERMISSION) } returns false
        assertThrows(Exception::class.java) { dispatcher.execute("npc list", source) }
    }

    private fun point(x: Double = 1.0) = StringLocation(x, 65.0, 2.0, 90f, 10f, "missing_world")
    private fun plugin(): JavaPlugin = mockk<JavaPlugin>(relaxed = true).also {
        every { it.dataFolder } returns directory.toFile()
        every { it.getResource(any()) } returns null
    }

    private fun player(world: World): Player = mockk<Player>(relaxed = true).also {
        every { it.uniqueId } returns UUID.randomUUID()
        every { it.world } returns world
        every { it.location } returns Location(world, 1.0, 0.0, 0.0)
        every { it.isOnline } returns true
        every { it.isDead } returns false
        every { it.gameMode } returns org.bukkit.GameMode.SURVIVAL
    }
}
