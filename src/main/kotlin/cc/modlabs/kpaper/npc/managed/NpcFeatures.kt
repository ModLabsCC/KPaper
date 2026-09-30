package cc.modlabs.kpaper.npc.managed

import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose



enum class NpcPose(val id: String, val entityPose: EntityPose, vararg val aliases: String) {
    STANDING("standing", EntityPose.STANDING, "stehen"),
    CROUCHING("crouching", EntityPose.CROUCHING, "hocken", "sneaking"),
    SITTING("sitting", EntityPose.SITTING, "sitzen"),
    LYING("lying", EntityPose.SLEEPING, "liegen", "sleeping"),
    CRAWLING("crawling", EntityPose.SWIMMING, "kriechen", "swimming"),
    FALL_FLYING("fall_flying", EntityPose.FALL_FLYING, "fliegen"),
    DYING("dying", EntityPose.DYING, "sterben");

    companion object {
        fun from(value: String) = entries.firstOrNull {
            it.id.equals(value, ignoreCase = true) || it.aliases.any { alias -> alias.equals(value, ignoreCase = true) }
        }
    }
}


enum class NpcAnimation(val id: String) {
    TALKING("talking"),
    ARM_SWING("arm-swing"),
    IDLE_LOOK("idle-look");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.id.equals(value, ignoreCase = true) }
    }
}


enum class NpcDialogSelection {
    ALL,
    RANDOM;

    companion object {
        fun from(value: String) = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}


enum class NpcDialogTrigger {
    CLICK,
    PROXIMITY,
    BOTH;

    val onClick get() = this == CLICK || this == BOTH
    val onProximity get() = this == PROXIMITY || this == BOTH

    companion object {
        fun from(value: String) = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}


enum class NpcMovementMode(val id: String) {
    OFF("off"),
    ROAMING("roaming"),
    RANDOM_RETURN("random-return"),
    RANDOM("random"),
    PATH_CIRCLE("path-circle"),
    PATH_PINGPONG("path-pingpong"),
    DANCING("dancing");

    val usesWaypoints: Boolean get() = this in setOf(RANDOM_RETURN, RANDOM, PATH_CIRCLE, PATH_PINGPONG)

    companion object {
        fun from(value: String) = entries.firstOrNull { it.id.equals(value, ignoreCase = true) }
    }
}


enum class NpcStepMode(val id: String) {
    NONE("none"),
    STAIRS_SLABS("stairs-slabs"),
    FULL_BLOCKS("full-blocks");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.id.equals(value, ignoreCase = true) }
    }
}


data class NpcMovementSettings(
    val mode: NpcMovementMode = NpcMovementMode.OFF,
    val speed: Double = 1.6,
    val waitTicks: Int = 40,
    val roamingRadius: Double = 6.0,
    val roamingRegionId: String? = null,
    val stepMode: NpcStepMode = NpcStepMode.FULL_BLOCKS,
    val onlyWhenUnwatched: Boolean = false,
    val danceToMusicRadius: Double? = null,
    val waypoints: List<NpcWaypoint> = emptyList(),
)
