package cc.modlabs.kpaper.game

import org.bukkit.entity.Player

interface GamePlayers {

    var livingPlayers: MutableList<Player>

    var spectators: MutableList<Player>
}