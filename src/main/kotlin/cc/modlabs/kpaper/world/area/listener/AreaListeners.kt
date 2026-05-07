package cc.modlabs.kpaper.world.area.listener

import cc.modlabs.kpaper.world.area.model.getArea
import cc.modlabs.kpaper.world.area.model.onEnter
import cc.modlabs.kpaper.world.area.model.onLeave
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent

class AreaListeners : Listener {

    private fun handleMovement (from: Location, to:Location, player: Player){
        val fromArea = from.getArea()
        val toArea = to.getArea()

        if (fromArea != null && toArea != null && fromArea.name != toArea.name) {
            fromArea.onLeave(player)
            toArea.onEnter(player)
        } else if (fromArea != null && toArea == null) {
            fromArea.onLeave(player)
        } else if (fromArea == null && toArea != null) {
            toArea.onEnter(player)
        }
    }

    @EventHandler
    fun onMove (event: PlayerMoveEvent){
        val player = event.player
        val from = event.from
        val to = event.to
        handleMovement(from, to, player)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val area =event.player.location.getArea() ?: return
        area.onEnter(event.player)
    }

    @EventHandler
    fun onTeleportIntoArea (event: PlayerTeleportEvent){
        val player = event.player
        val from = event.from
        val to = event.to
        handleMovement(from, to, player)
    }
}