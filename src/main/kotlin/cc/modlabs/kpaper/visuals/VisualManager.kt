package cc.modlabs.kpaper.visuals

import org.bukkit.entity.Player

/**
 * Generic manager to render [Visualizable]s to an unkown representation
 */
interface VisualManager {
    fun addVisual(player: Player, visual: VisualElement)
    fun removeVisual(player: Player, id: String)
    fun removePlayer(player: Player)
}