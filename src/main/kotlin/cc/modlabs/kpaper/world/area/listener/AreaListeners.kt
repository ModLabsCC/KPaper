package cc.modlabs.kpaper.world.area.listener

import cc.modlabs.kpaper.world.area.AreaCache
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
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent

class AreaListeners : Listener {

    private fun handleMovement (from: Location, to:Location, player: Player){
        val fromArea = from.getArea()
        val toArea = to.getArea()

        if (fromArea != null && toArea != null && fromArea !== toArea) {
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

    /**
     * Custom dimensions (e.g. WorldEngine with a namespace prefix) are often not present in
     * [org.bukkit.Bukkit.getWorlds] when plugins enable. Reload area definitions whenever a world loads
     * so [cc.modlabs.kpaper.world.area.AreaCache] picks up that world's worldConfig.yml.
     */
    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        AreaCache.reloadAreas()
    }

    @EventHandler
    fun onWorldUnload(event: WorldUnloadEvent) {
        AreaCache.clear(event.world.name)
    }
}
