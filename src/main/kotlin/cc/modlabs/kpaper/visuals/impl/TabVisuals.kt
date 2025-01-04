package cc.modlabs.kpaper.visuals.impl

import cc.modlabs.kpaper.extensions.timer
import cc.modlabs.kpaper.visuals.VisualElement
import cc.modlabs.kpaper.visuals.VisualManager
import cc.modlabs.kpaper.visuals.VisualsStore
import dev.fruxz.stacked.text
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.time.Duration.Companion.seconds

object TabVisuals : VisualManager {
    private const val maxSlot = 20
    private val store = VisualsStore(maxSlot) // 10 head 10 foot
    private val buff = StringBuilder()

    override fun addVisual(player: Player, visual: VisualElement) =
        store.addVisual(player, visual)

    override fun removeVisual(player: Player, id: String) =
        store.removeVisual(player, id)

    override fun removePlayer(player: Player) =
        store.removePlayer(player)

    fun runTablist() {
        timer(20, "Tab Visuals") {
            store.renderAll(1.seconds).forEach {
                val player = Bukkit.getPlayer(it.first.uniqueId) ?: return@forEach
                buff.setLength(0)
                for (s in it.second.take(10)) {
                    buff
                        .append(s ?: "")
                        .append('\n')
                }
                player.sendPlayerListHeader(text(buff.toString().trim()))
                buff.setLength(0)
                for (s in it.second) {
                    buff
                        .append(s ?: "")
                        .append('\n')
                }
                player.sendPlayerListFooter(text(buff.toString().trim()))
            }
        }
    }
}