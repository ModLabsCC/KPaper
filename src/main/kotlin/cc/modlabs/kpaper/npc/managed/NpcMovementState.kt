package cc.modlabs.kpaper.npc.managed

import org.bukkit.Location
import java.util.ArrayDeque

internal data class NpcMovementState(
    var position: Location,
    val path: ArrayDeque<Location> = ArrayDeque(),
    var waitUntil: Long = 0,
    var pathIndex: Int = 0,
    var pathDirection: Int = 1,
    var returningHome: Boolean = false,
    var lastWaypoint: String? = null,
    var danceMove: NpcDanceMove? = null,
    var danceMoveTick: Int = 0,
    var danceMoveDuration: Int = 0,
    var danceSwingInterval: Int = 8,
    var dancing: Boolean = false,
    var danceOrigin: Location? = null,
    var dancePose: NpcPose? = null,
    var musicPlaying: Boolean = false,
    var nextMusicCheck: Long = 0,
    var nextIdleSwing: Long = 0,
    var nextIdleLook: Long = 0,
)
