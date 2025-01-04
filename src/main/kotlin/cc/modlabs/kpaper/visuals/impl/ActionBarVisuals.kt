package cc.modlabs.kpaper.visuals.impl

import cc.modlabs.kpaper.extensions.timer
import cc.modlabs.kpaper.visuals.VisualElement
import cc.modlabs.kpaper.visuals.VisualManager
import cc.modlabs.kpaper.visuals.VisualsStore
import dev.fruxz.stacked.text
import org.bukkit.entity.Player
import kotlin.time.Duration.Companion.seconds

object ActionBarVisuals : VisualManager {
    private val store = VisualsStore(5)
    private val buff = StringBuilder()

    override fun addVisual(player: Player, visual: VisualElement) =
        store.addVisual(player, visual)

    override fun removeVisual(player: Player, id: String) =
        store.removeVisual(player, id)

    override fun removePlayer(player: Player) =
        store.removePlayer(player)

    fun runActionBars() {
        timer(20, "Render ActionBar") {
            store.renderAll(1.seconds).forEach {
                buff.setLength(0)
                it.second.forEach { s -> buff.append(s ?: "").append(' ') }
                it.first.sendActionBar(text(buff.toString()))
            }
        }
    }
}