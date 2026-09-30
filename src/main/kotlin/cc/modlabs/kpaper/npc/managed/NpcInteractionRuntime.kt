package cc.modlabs.kpaper.npc.managed

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.logging.Level

internal class NpcInteractionRuntime(
    private val plugin: JavaPlugin,
    private val renderer: PacketNpcRenderer,
    private val get: (String) -> NpcDefinition?,
    private val action: (String) -> NpcAction?,
) {
    private data class Key(val player: UUID, val npc: String)
    private val clicks = mutableMapOf<Key, Long>()
    private val cooldowns = mutableMapOf<Key, Long>()
    private val bags = mutableMapOf<Key, NpcDialogShuffleBag>()
    private val tasks = mutableMapOf<Key, MutableList<BukkitTask>>()

    fun clear() {
        tasks.values.flatten().forEach(BukkitTask::cancel)
        clicks.clear(); cooldowns.clear(); bags.clear(); tasks.clear()
    }

    fun remove(id: String) {
        tasks.filterKeys { it.npc == id }.values.flatten().forEach(BukkitTask::cancel)
        tasks.keys.removeIf { it.npc == id }
        clicks.keys.removeIf { it.npc == id }
        cooldowns.keys.removeIf { it.npc == id }
        bags.keys.removeIf { it.npc == id }
    }

    fun quit(player: UUID) {
        tasks.filterKeys { it.player == player }.values.flatten().forEach(BukkitTask::cancel)
        tasks.keys.removeIf { it.player == player }
        clicks.keys.removeIf { it.player == player }
        cooldowns.keys.removeIf { it.player == player }
        bags.keys.removeIf { it.player == player }
    }

    fun interact(player: Player, id: String, trigger: NpcDialogTrigger) {
        val npc = get(id) ?: return
        val location = renderer.location(id) ?: return
        if (!player.isOnline || player.isDead || player.gameMode == GameMode.SPECTATOR || player.world != location.world) return
        val range = if (trigger == NpcDialogTrigger.CLICK) 6.0 else npc.dialogRange
        if (player.location.distanceSquared(location) > range * range) return
        val key = Key(player.uniqueId, id)
        val now = System.currentTimeMillis()
        clicks.entries.removeIf { now - it.value > 1000L }
        val previous = clicks[key]
        if (previous != null && now - previous < 350L) return
        clicks[key] = now
        if (trigger == NpcDialogTrigger.CLICK) {
            npc.clickSound?.let { player.playSound(location, it, SoundCategory.VOICE, 1f, npc.clickSoundPitch) }
        }
        if (npc.action != null && matches(npc.actionTrigger, trigger)) {
            val callback = action(npc.action) ?: return // Unknown action fails closed.
            val continueDialog = try {
                callback(player, npc.snapshot(), trigger)
            } catch (failure: Exception) {
                plugin.logger.log(Level.WARNING, "NPC action '${npc.action}' failed for '$id'", failure)
                false
            }
            if (!continueDialog || get(id) !== npc) return
        }
        if (!matches(npc.dialogTrigger, trigger) || npc.dialogLines.isEmpty() || (cooldowns[key] ?: 0L) > now) return
        val lines = selectedNpcDialogLines(npc.dialogLines, npc.dialogSelection, bags.getOrPut(key, ::NpcDialogShuffleBag))
        cooldowns[key] = now + lines.size * 1500L + 2000L
        lines.forEachIndexed { index, line ->
            later(key, index * 30L) {
                val current = renderer.location(id) ?: return@later
                if (!player.isOnline || player.world != current.world || player.location.distanceSquared(current) > 96.0 * 96.0) return@later
                if (index == 0) npc.dialogSound?.let {
                    player.playSound(current, it, SoundCategory.VOICE, npc.dialogSoundVolume, npc.dialogSoundPitch)
                }
                player.sendMessage(parsedNpcName(npc.name).color(NamedTextColor.GOLD)
                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                    .append(npcDialogComponent(line).color(NamedTextColor.WHITE)))
                if (NpcAnimation.TALKING in npc.animations) {
                    renderer.swing(id, index % 2 != 0)
                    renderer.headPitch(id, -20f)
                    later(key, 8L) { renderer.headPitch(id, 20f) }
                    later(key, 16L) { renderer.headPitch(id, -20f) }
                    later(key, 24L) { renderer.headPitch(id, 0f) }
                }
            }
        }
        later(key, lines.size * 30L + 40L) { cooldowns.remove(key) }
    }

    private fun later(key: Key, delay: Long, block: () -> Unit) {
        lateinit var task: BukkitTask
        task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            try { block() } finally {
                tasks[key]?.remove(task)
                if (tasks[key]?.isEmpty() == true) tasks.remove(key)
            }
        }, delay)
        tasks.getOrPut(key, ::mutableListOf).add(task)
    }

    private fun matches(configured: NpcDialogTrigger, actual: NpcDialogTrigger) = configured == actual || configured == NpcDialogTrigger.BOTH
}
