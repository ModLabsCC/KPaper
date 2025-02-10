package cc.modlabs.kpaper.game.countdown

import cc.modlabs.kpaper.game.GamePlayers
import org.bukkit.scheduler.BukkitTask

abstract class Countdown(val game: GamePlayers, val defaultDuration: Int) {

    var duration: Int = defaultDuration


    lateinit var countdown: BukkitTask
    abstract fun start()

    abstract fun stop()
}