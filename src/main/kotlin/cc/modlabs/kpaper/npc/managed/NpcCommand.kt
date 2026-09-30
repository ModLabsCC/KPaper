package cc.modlabs.kpaper.npc.managed

import cc.modlabs.kpaper.command.CommandBuilder
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.Locale

class NpcCommand(private val service: () -> NpcService) : CommandBuilder {
    override val description = "Manage persistent packet NPCs"

    override fun register(): LiteralCommandNode<CommandSourceStack> {
        val root = Commands.literal("npc").requires { it.sender.hasPermission(PERMISSION) }
        root.then(Commands.literal("list").executes { ctx -> list(ctx, 1) }
            .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes { ctx -> list(ctx, IntegerArgumentType.getInteger(ctx, "page")) }))
        root.then(Commands.literal("reload").executes { ctx -> run(ctx) { it.reload() } })
        root.then(Commands.literal("create").then(word("id").executes { ctx -> run(ctx) {
            it.create(string(ctx, "id"), player(ctx).location)
        } }))
        root.then(Commands.literal("delete").then(npcId().executes { ctx -> run(ctx) {
            require(it.delete(string(ctx, "id"))) { "NPC not found" }
        } }))
        root.then(Commands.literal("here").then(npcId().executes { ctx -> run(ctx) {
            it.setHome(string(ctx, "id"), player(ctx).location)
        } }))
        root.then(Commands.literal("info").then(npcId().executes { ctx -> run(ctx) {
            val npc = requireNotNull(it.get(string(ctx, "id"))) { "NPC not found" }
            ctx.source.sender.sendMessage(Component.text("${npc.id}: ${npc.name}, skin=${npc.skin ?: "default"}, " +
                "world=${npc.position.world}, pose=${npc.pose.id}, movement=${npc.movement.mode.id}, " +
                "dialog=${npc.dialogLines.size}, action=${npc.action ?: "none"}"))
        } }))
        root.then(Commands.literal("name").then(npcId().then(greedy("name").executes { ctx -> run(ctx) {
            it.setName(string(ctx, "id"), string(ctx, "name"))
        } })))
        root.then(Commands.literal("hologram").then(npcId()
            .then(Commands.literal("clear").executes { ctx -> edit(ctx) { it.copy(hologram = null) } })
            .then(greedy("label").executes { ctx -> edit(ctx) { it.copy(hologram = string(ctx, "label")) } })))
        root.then(Commands.literal("skin").then(npcId()
            .then(Commands.literal("clear").executes { ctx -> run(ctx) { it.setSkin(string(ctx, "id"), null) } })
            .then(word("skin") { service().skins.keys() }.executes { ctx -> run(ctx) {
                it.setSkin(string(ctx, "id"), string(ctx, "skin"))
            } })))
        root.then(Commands.literal("skin-layer").then(npcId().then(word("layer") { NPC_SKIN_LAYERS.keys + "all" }
            .then(Commands.argument("enabled", BoolArgumentType.bool()).executes { ctx -> run(ctx) {
                it.setSkinLayer(string(ctx, "id"), string(ctx, "layer"), BoolArgumentType.getBool(ctx, "enabled"))
            } }))))
        root.then(Commands.literal("equipment").then(npcId().then(word("slot") { NPC_EQUIPMENT_SLOTS.map { it.name.lowercase() } }
            .then(Commands.literal("held").executes { ctx -> run(ctx) {
                it.setEquipment(string(ctx, "id"), requireNotNull(npcEquipmentSlot(string(ctx, "slot"))), player(ctx).inventory.itemInMainHand)
            } })
            .then(Commands.literal("clear").executes { ctx -> run(ctx) {
                it.setEquipment(string(ctx, "id"), requireNotNull(npcEquipmentSlot(string(ctx, "slot"))), null)
            } }))))
        root.then(Commands.literal("pose").then(npcId().then(word("pose") { NpcPose.entries.map { it.id } }
            .executes { ctx -> edit(ctx) { it.copy(pose = requireNotNull(NpcPose.from(string(ctx, "pose")))) } })))
        root.then(Commands.literal("name-visible").then(npcId().then(Commands.argument("visible", BoolArgumentType.bool())
            .executes { ctx -> edit(ctx) { it.copy(nameVisible = BoolArgumentType.getBool(ctx, "visible")) } })))
        root.then(Commands.literal("lookclose").then(npcId().then(Commands.argument("enabled", BoolArgumentType.bool())
            .then(Commands.argument("range", DoubleArgumentType.doubleArg(1.0, 32.0)).executes { ctx -> edit(ctx) {
                it.copy(lookClose = BoolArgumentType.getBool(ctx, "enabled"), lookCloseRange = DoubleArgumentType.getDouble(ctx, "range"))
            } }))))
        root.then(Commands.literal("rotation").then(npcId()
            .then(Commands.argument("yaw", DoubleArgumentType.doubleArg(-180.0, 180.0))
                .then(Commands.argument("pitch", DoubleArgumentType.doubleArg(-90.0, 90.0)).executes { ctx -> edit(ctx) {
                    it.copy(position = cc.modlabs.klassicx.tools.minecraft.StringLocation(
                        it.position.x, it.position.y, it.position.z, DoubleArgumentType.getDouble(ctx, "yaw").toFloat(),
                        DoubleArgumentType.getDouble(ctx, "pitch").toFloat(), it.position.world))
                } }))))
        root.then(Commands.literal("animation").then(npcId().then(word("animation") { NpcAnimation.entries.map { it.id } }
            .then(Commands.argument("enabled", BoolArgumentType.bool()).executes { ctx -> edit(ctx) {
                it.copy(animations = updatedNpcAnimations(it.animations, requireNotNull(NpcAnimation.from(string(ctx, "animation"))),
                    BoolArgumentType.getBool(ctx, "enabled")))
            } }))))
        root.then(Commands.literal("action").then(npcId()
            .then(Commands.literal("clear").executes { ctx -> edit(ctx) { it.copy(action = null, actionData = emptyMap()) } })
            .then(word("action") { service().actionKeys() }
                .then(word("trigger") { triggerIds() }.executes { ctx -> edit(ctx) {
                    val key = string(ctx, "action")
                    require(key in service().actionKeys()) { "Action is not registered" }
                    it.copy(action = key, actionTrigger = trigger(ctx))
                } }))))
        root.then(Commands.literal("action-data").then(npcId().then(word("key") { ctx -> service().get(string(ctx, "id"))?.actionData?.keys.orEmpty() }
            .then(greedy("value").executes { ctx -> edit(ctx) {
                it.copy(actionData = it.actionData + (string(ctx, "key") to string(ctx, "value")))
            } }))))
        root.then(Commands.literal("click-sound").then(npcId()
            .then(Commands.literal("clear").executes { ctx -> edit(ctx) { it.copy(clickSound = null) } })
            .then(word("sound").then(Commands.argument("pitch", DoubleArgumentType.doubleArg(0.5, 2.0)).executes { ctx -> edit(ctx) {
                it.copy(clickSound = string(ctx, "sound"), clickSoundPitch = DoubleArgumentType.getDouble(ctx, "pitch").toFloat())
            } }))))
        root.then(Commands.literal("trait").then(npcId().then(word("trait") { service().registeredTraits() }
            .then(Commands.literal("clear").executes { ctx -> edit(ctx) { it.copy(traits = it.traits - string(ctx, "trait")) } })
            .then(greedy("value").executes { ctx -> edit(ctx) {
                it.copy(traits = it.traits + (validatedNpcKey(string(ctx, "trait")) to string(ctx, "value")))
            } }))))
        val dialog = Commands.literal("dialog").then(npcId()
            .then(Commands.literal("add").then(greedy("line").executes { ctx -> run(ctx) { it.addDialogLine(string(ctx, "id"), string(ctx, "line")) } }))
            .then(Commands.literal("clear").executes { ctx -> edit(ctx) { it.copy(dialogLines = emptyList()) } })
            .then(Commands.literal("remove").then(Commands.argument("line", IntegerArgumentType.integer(1)).executes { ctx -> edit(ctx) {
                val index = IntegerArgumentType.getInteger(ctx, "line") - 1
                require(index in it.dialogLines.indices) { "Dialog line not found" }
                it.copy(dialogLines = it.dialogLines.filterIndexed { i, _ -> i != index })
            } }))
            .then(Commands.literal("trigger").then(word("trigger") { triggerIds() }.executes { ctx -> edit(ctx) { it.copy(dialogTrigger = trigger(ctx)) } }))
            .then(Commands.literal("selection").then(word("selection") { NpcDialogSelection.entries.map { it.name.lowercase() } }
                .executes { ctx -> edit(ctx) { it.copy(dialogSelection = requireNotNull(NpcDialogSelection.from(string(ctx, "selection")))) } }))
            .then(Commands.literal("range").then(Commands.argument("range", DoubleArgumentType.doubleArg(1.0, 16.0))
                .executes { ctx -> edit(ctx) { it.copy(dialogRange = DoubleArgumentType.getDouble(ctx, "range")) } }))
            .then(Commands.literal("sound").then(word("sound")
                .executes { ctx -> edit(ctx) { it.copy(dialogSound = string(ctx, "sound")) } }
                .then(Commands.argument("volume", DoubleArgumentType.doubleArg(0.0, 16.0))
                    .then(Commands.argument("pitch", DoubleArgumentType.doubleArg(0.5, 2.0)).executes { ctx -> edit(ctx) {
                        it.copy(dialogSound = string(ctx, "sound"), dialogSoundVolume = DoubleArgumentType.getDouble(ctx, "volume").toFloat(),
                            dialogSoundPitch = DoubleArgumentType.getDouble(ctx, "pitch").toFloat())
                    } }))))
            .then(Commands.literal("sound-clear").executes { ctx -> edit(ctx) { it.copy(dialogSound = null) } }))
        root.then(dialog)
        val movement = npcId()
            .then(Commands.literal("mode").then(word("mode") { NpcMovementMode.entries.map { it.id } }.executes { ctx -> edit(ctx) {
                val mode = requireNotNull(NpcMovementMode.from(string(ctx, "mode")))
                require(!mode.usesWaypoints || it.movement.waypoints.isNotEmpty()) { "Add a waypoint first" }
                it.copy(movement = it.movement.copy(mode = mode))
            } }))
            .then(Commands.literal("speed").then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.2, 8.0))
                .executes { ctx -> edit(ctx) { it.copy(movement = it.movement.copy(speed = DoubleArgumentType.getDouble(ctx, "speed"))) } }))
            .then(Commands.literal("radius").then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0, 64.0))
                .executes { ctx -> edit(ctx) { it.copy(movement = it.movement.copy(roamingRadius = DoubleArgumentType.getDouble(ctx, "radius"))) } }))
            .then(Commands.literal("wait").then(Commands.argument("ticks", IntegerArgumentType.integer(0, 6000))
                .executes { ctx -> edit(ctx) { it.copy(movement = it.movement.copy(waitTicks = IntegerArgumentType.getInteger(ctx, "ticks"))) } }))
            .then(Commands.literal("step").then(word("step") { NpcStepMode.entries.map { it.id } }
                .executes { ctx -> edit(ctx) { it.copy(movement = it.movement.copy(stepMode = requireNotNull(NpcStepMode.from(string(ctx, "step"))))) } }))
            .then(Commands.literal("unwatched").then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes { ctx -> edit(ctx) { it.copy(movement = it.movement.copy(onlyWhenUnwatched = BoolArgumentType.getBool(ctx, "enabled"))) } }))
            .then(Commands.literal("music").then(Commands.literal("off").executes { ctx -> edit(ctx) {
                it.copy(movement = it.movement.copy(danceToMusicRadius = null))
            } }).then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0, 64.0)).executes { ctx -> edit(ctx) {
                it.copy(movement = it.movement.copy(danceToMusicRadius = DoubleArgumentType.getDouble(ctx, "radius")))
            } }))
            .then(Commands.literal("region").then(Commands.literal("clear").executes { ctx -> edit(ctx) {
                it.copy(movement = it.movement.copy(roamingRegionId = null))
            } }).then(word("region") { service().hooks.regionKeys() }.executes { ctx -> edit(ctx) {
                it.copy(movement = it.movement.copy(roamingRegionId = string(ctx, "region")))
            } }))
            .then(Commands.literal("point-add").then(word("point")
                .then(Commands.argument("weight", IntegerArgumentType.integer(1, 1000)).executes { ctx -> run(ctx) {
                    it.addWaypoint(string(ctx, "id"), string(ctx, "point"), player(ctx).location, IntegerArgumentType.getInteger(ctx, "weight"))
                } })))
            .then(Commands.literal("point-remove").then(word("point", { ctx ->
                service().get(string(ctx, "id"))?.movement?.waypoints?.map { it.name }.orEmpty()
            }).executes { ctx -> edit(ctx) { it.copy(movement = it.movement.copy(waypoints = it.movement.waypoints.filterNot {
                point -> point.name == string(ctx, "point")
            })) } }))
            .then(Commands.literal("point-weight").then(word("point") { ctx ->
                service().get(string(ctx, "id"))?.movement?.waypoints?.map { it.name }.orEmpty()
            }.then(Commands.argument("weight", IntegerArgumentType.integer(1, 1000)).executes { ctx -> edit(ctx) {
                require(it.movement.waypoints.any { point -> point.name == string(ctx, "point") }) { "Waypoint not found" }
                it.copy(movement = it.movement.copy(waypoints = it.movement.waypoints.map { point ->
                    if (point.name == string(ctx, "point")) point.copy(weight = IntegerArgumentType.getInteger(ctx, "weight")) else point
                }))
            } })))
        root.then(Commands.literal("movement").then(movement))
        return root.build()
    }

    private fun npcId() = word("id") { service().ids() }
    private fun greedy(name: String) = Commands.argument(name, StringArgumentType.greedyString())
    private fun word(name: String, options: (CommandContext<CommandSourceStack>) -> Collection<String> = { emptyList() }): RequiredArgumentBuilder<CommandSourceStack, String> =
        Commands.argument(name, StringArgumentType.word()).suggests { ctx, builder ->
            val input = builder.remaining.lowercase(Locale.ROOT)
            options(ctx).filter { it.lowercase(Locale.ROOT).startsWith(input) }.forEach(builder::suggest)
            builder.buildFuture()
        }
    private fun string(ctx: CommandContext<CommandSourceStack>, name: String) = StringArgumentType.getString(ctx, name)
    private fun player(ctx: CommandContext<CommandSourceStack>) = requireNotNull(ctx.source.sender as? Player) { "This command requires a player" }
    private fun triggerIds() = NpcDialogTrigger.entries.map { it.name.lowercase() }
    private fun trigger(ctx: CommandContext<CommandSourceStack>) = requireNotNull(NpcDialogTrigger.from(string(ctx, "trigger")))
    private fun edit(ctx: CommandContext<CommandSourceStack>, transform: (NpcDefinition) -> NpcDefinition) =
        run(ctx) { it.update(string(ctx, "id"), transform) }
    private fun list(ctx: CommandContext<CommandSourceStack>, requested: Int) = run(ctx) {
        val ids = it.ids()
        val pages = maxOf(1, (ids.size + 6) / 7)
        val page = requested.coerceAtMost(pages)
        ctx.source.sender.sendMessage(Component.text("NPCs ${ids.size}, page $page/$pages: " +
            ids.drop((page - 1) * 7).take(7).joinToString().ifEmpty { "none" }))
    }
    private fun run(ctx: CommandContext<CommandSourceStack>, action: (NpcService) -> Unit): Int = try {
        action(service())
        ctx.source.sender.sendMessage(Component.text("NPC command completed."))
        1
    } catch (failure: Exception) {
        ctx.source.sender.sendMessage(Component.text(failure.message ?: "NPC command failed"))
        0
    }

    companion object { const val PERMISSION = "kpaper.npc.admin" }
}
