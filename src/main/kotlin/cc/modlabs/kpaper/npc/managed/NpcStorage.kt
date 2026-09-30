package cc.modlabs.kpaper.npc.managed

import org.bukkit.plugin.java.JavaPlugin
import cc.modlabs.klassicx.tools.minecraft.StringLocation
import cc.modlabs.kpaper.file.readYaml
import cc.modlabs.kpaper.file.saveAtomically
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.Locale

internal class NpcStorage(private val plugin: JavaPlugin) {
    private val file = File(plugin.dataFolder, "npcs.yml")

    fun load(): List<NpcDefinition> {
        val config = readYaml(file)
        require(!config.contains("npcs") || config.isConfigurationSection("npcs")) { "Invalid NPC section" }
        return config.getConfigurationSection("npcs")?.getKeys(false).orEmpty().map { id -> loadNpc(config, id) }
    }

    fun save(definitions: Collection<NpcDefinition>) {
        val config = YamlConfiguration()
        definitions.forEach { npc ->
            val path = "npcs.${npc.id}"
            config.set("$path.action", npc.action)
            config.set("$path.action-trigger", npc.actionTrigger.name.lowercase())
            config.set("$path.action-data", npc.actionData)
            config.set("$path.traits", npc.traits)
            config.set("$path.skin", npc.skin)
            config.set("$path.name", npc.name)
            config.set("$path.name-visible", npc.nameVisible)
            config.set("$path.hologram", npc.hologram)
            config.set("$path.animations", npc.animations.map(NpcAnimation::id).sorted())
            config.set("$path.click-sound", npc.clickSound)
            config.set("$path.click-sound-pitch", npc.clickSoundPitch)
            config.set("$path.look-close", npc.lookClose)
            config.set("$path.look-close-range", npc.lookCloseRange)
            npc.equipment.forEach { (slot, item) -> config.set("$path.equipment.${slot.name.lowercase()}", item) }
            config.set("$path.dialog.lines", npc.dialogLines)
            config.set("$path.dialog.trigger", npc.dialogTrigger.name.lowercase())
            config.set("$path.dialog.selection", npc.dialogSelection.name.lowercase())
            config.set("$path.dialog.range", npc.dialogRange)
            config.set("$path.dialog.sound", npc.dialogSound)
            config.set("$path.dialog.sound-volume", npc.dialogSoundVolume)
            config.set("$path.dialog.sound-pitch", npc.dialogSoundPitch)
            config.set("$path.pose", npc.pose.id)
            config.set("$path.skin-layers", npc.skinLayers.toInt() and 0x7f)
            config.set("$path.world", npc.position.world)
            config.set("$path.x", npc.position.x)
            config.set("$path.y", npc.position.y)
            config.set("$path.z", npc.position.z)
            config.set("$path.yaw", npc.position.yaw)
            config.set("$path.pitch", npc.position.pitch)
            saveMovement(config, path, npc.movement)
        }
        config.saveAtomically(file)
    }

    private fun loadNpc(config: YamlConfiguration, id: String): NpcDefinition {
        val path = "npcs.$id"
        val world = requireNotNull(config.getString("$path.world")) { "NPC '$id' has no world" }
        return NpcDefinition(
            id = normalizedNpcId(id),
            position = StringLocation(
                config.getDouble("$path.x"),
                config.getDouble("$path.y"),
                config.getDouble("$path.z"),
                config.getDouble("$path.yaw").toFloat(),
                config.getDouble("$path.pitch").toFloat(),
                world,
            ),
            action = config.getString("$path.action"),
            actionTrigger = config.getString("$path.action-trigger")?.let { requireNotNull(NpcDialogTrigger.from(it)) } ?: NpcDialogTrigger.CLICK,
            actionData = config.getConfigurationSection("$path.action-data")?.getValues(false)?.mapValues { it.value.toString() }.orEmpty(),
            traits = config.getConfigurationSection("$path.traits")?.getValues(false)?.mapValues { it.value.toString() }.orEmpty(),
            skin = config.getString("$path.skin"),
            name = config.getString("$path.name") ?: id.take(16),
            lookClose = config.getBoolean("$path.look-close"),
            lookCloseRange = config.getDouble("$path.look-close-range", 5.0),
            equipment = NPC_EQUIPMENT_SLOTS.mapNotNull { slot ->
                config.getItemStack("$path.equipment.${slot.name.lowercase()}")
                    ?.takeUnless { it.type.isAir }
                    ?.let { slot to it.clone() }
            }.toMap(),
            dialogLines = config.getStringList("$path.dialog.lines"),
            dialogTrigger = config.getString("$path.dialog.trigger")?.let { requireNotNull(NpcDialogTrigger.from(it)) } ?: NpcDialogTrigger.CLICK,
            dialogRange = config.getDouble("$path.dialog.range", 4.0),
            skinLayers = config.getInt("$path.skin-layers", ALL_NPC_SKIN_LAYERS.toInt()).also { require(it in 0..127) }.toByte(),
            dialogSound = config.getString("$path.dialog.sound"),
            dialogSoundVolume = config.getDouble("$path.dialog.sound-volume", 1.0).toFloat(),
            dialogSoundPitch = config.getDouble("$path.dialog.sound-pitch", 1.0).toFloat(),
            pose = config.getString("$path.pose")?.let { requireNotNull(NpcPose.from(it)) } ?: NpcPose.STANDING,
            dialogSelection = config.getString("$path.dialog.selection")?.let { requireNotNull(NpcDialogSelection.from(it)) } ?: NpcDialogSelection.ALL,
            movement = loadMovement(config, path, world),
            nameVisible = config.getBoolean("$path.name-visible", true),
            hologram = config.getString("$path.hologram"),
            animations = config.getStringList("$path.animations").map { requireNotNull(NpcAnimation.from(it)) }.toSet(),
            clickSound = config.getString("$path.click-sound"),
            clickSoundPitch = config.getDouble("$path.click-sound-pitch", 1.0).toFloat(),
        )
    }

    private fun loadMovement(config: YamlConfiguration, npcPath: String, world: String): NpcMovementSettings {
        val path = "$npcPath.movement"
        val pointSection = config.getConfigurationSection("$path.points")
        val order = config.getStringList("$path.point-order").ifEmpty { pointSection?.getKeys(false).orEmpty().toList() }
        require(order.distinct().size == order.size) { "Duplicate waypoint names" }
        val waypoints = order.map { rawName ->
                val name = validatedNpcWaypointName(rawName)
                val pointPath = "$path.points.$name"
                NpcWaypoint(
                    name,
                    StringLocation(
                        config.getDouble("$pointPath.x"),
                        config.getDouble("$pointPath.y"),
                        config.getDouble("$pointPath.z"),
                        config.getDouble("$pointPath.yaw").toFloat(),
                        config.getDouble("$pointPath.pitch").toFloat(),
                        world,
                    ),
                    config.getInt("$pointPath.weight", 1),
                )
        }
        return NpcMovementSettings(
            mode = config.getString("$path.mode")?.let { requireNotNull(NpcMovementMode.from(it)) } ?: NpcMovementMode.OFF,
            speed = config.getDouble("$path.speed", 1.6),
            waitTicks = config.getInt("$path.wait-ticks", 40),
            roamingRadius = config.getDouble("$path.roaming-radius", 6.0),
            roamingRegionId = config.getString("$path.roaming-region")?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty),
            stepMode = config.getString("$path.step-mode")?.let { requireNotNull(NpcStepMode.from(it)) } ?: NpcStepMode.FULL_BLOCKS,
            onlyWhenUnwatched = config.getBoolean("$path.only-when-unwatched", false),
            danceToMusicRadius = if (config.contains("$path.dance-to-music-radius")) config.getDouble("$path.dance-to-music-radius") else null,
            waypoints = waypoints,
        )
    }

    private fun saveMovement(config: YamlConfiguration, npcPath: String, movement: NpcMovementSettings) {
        val path = "$npcPath.movement"
        config.set("$path.mode", movement.mode.id)
        config.set("$path.speed", movement.speed)
        config.set("$path.wait-ticks", movement.waitTicks)
        config.set("$path.roaming-radius", movement.roamingRadius)
        config.set("$path.roaming-region", movement.roamingRegionId)
        config.set("$path.step-mode", movement.stepMode.id)
        config.set("$path.only-when-unwatched", movement.onlyWhenUnwatched)
        config.set("$path.dance-to-music-radius", movement.danceToMusicRadius)
        config.set("$path.point-order", movement.waypoints.map(NpcWaypoint::name))
        movement.waypoints.forEach { waypoint ->
            val pointPath = "$path.points.${waypoint.name}"
            config.set("$pointPath.x", waypoint.position.x)
            config.set("$pointPath.y", waypoint.position.y)
            config.set("$pointPath.z", waypoint.position.z)
            config.set("$pointPath.yaw", waypoint.position.yaw)
            config.set("$pointPath.pitch", waypoint.position.pitch)
            config.set("$pointPath.weight", waypoint.weight)
        }
    }
}
