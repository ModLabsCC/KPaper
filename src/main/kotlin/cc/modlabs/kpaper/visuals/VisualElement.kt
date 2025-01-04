package cc.modlabs.kpaper.visuals

import kotlin.time.Duration

/**
 * Structural component for [Visualizable]s.
 *
 * [id] uniquely identifies this VisualElement.
 *
 * [visualizable] is rendered.
 *
 * [slot] defines the position in the container.
 * Order and Orientation are defined by the container.
 *
 * [duration] defines for how long the visual is shown.
 * If null it is shown forever.
 *
 * VisualElements are equal when there id matches.
 */
data class VisualElement(
    val id: String,
    val visualizable: Visualizable,
    val slot: Int = 0,
    var duration: Duration? = null,
) {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is VisualElement -> false
            else -> other.id == id
        }
    }

    fun run(delta: Duration) {
        duration = duration?.minus(delta)
    }

    val valid get() = duration?.isPositive() ?: true
    val persistent get() = duration == null
}