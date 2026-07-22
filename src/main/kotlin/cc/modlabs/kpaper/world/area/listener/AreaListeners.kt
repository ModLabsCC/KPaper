package cc.modlabs.kpaper.world.area.listener

import cc.modlabs.kpaper.world.area.AreaCache
import cc.modlabs.kpaper.world.area.model.Area
import cc.modlabs.kpaper.world.area.model.areas
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

    private fun handleMovement(from: Location, to: Location, player: Player) {
        val fromAreas = from.areas().toIdentitySet()
        val toAreas = to.areas().toIdentitySet()

        for (area in fromAreas) {
            if (area !in toAreas) {
                area.onLeave(player)
            }
        }
        for (area in toAreas) {
            if (area !in fromAreas) {
                area.onEnter(player)
            }
        }
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        // Area membership only changes when the player crosses a block boundary.
        if (
            from.world === to.world &&
            from.blockX == to.blockX &&
            from.blockY == to.blockY &&
            from.blockZ == to.blockZ
        ) {
            return
        }
        handleMovement(from, to, event.player)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        for (area in event.player.location.areas()) {
            area.onEnter(event.player)
        }
    }

    @EventHandler
    fun onTeleportIntoArea(event: PlayerTeleportEvent) {
        handleMovement(event.from, event.to, event.player)
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

    private fun Collection<Area>.toIdentitySet(): MutableSet<Area> {
        val set = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Area, Boolean>())
        set.addAll(this)
        return set
    }
}
