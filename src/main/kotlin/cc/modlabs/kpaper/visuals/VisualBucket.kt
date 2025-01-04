package cc.modlabs.kpaper.visuals

import org.bukkit.entity.Player
import java.util.*
import kotlin.time.Duration

private class VisualBucket {
    var items = LinkedList<VisualElement>()

    fun render(delta: Duration, delete: (String) -> Unit): String? {
        // update all elements of this bucket and remove expired ones
        items.removeAll {
            it.run(delta)
            if (!it.valid) delete(it.id)
            !it.valid
        }
        if (items.isEmpty()) {
            return null
        }
        // render top ones
        return items.first().visualizable(delta)
    }

    fun remove(id: String) =
        items.removeAll { it.id == id }

    fun push(element: VisualElement, delete: (String) -> Unit) {
        // temporary item
        if (!element.persistent) {
            items.addFirst(element)
            return
        }
        // replace persistent item in slot
        if (items.isNotEmpty() && items.last().persistent) {
            delete(items.removeLast().id)
        }
        items.addLast(element)

    }
}

/**
 * VisualStore is a helper to create fixed-slot renderers for the visual API
 */
class VisualsStore(var maxSize: Int) {
    private data class Container(
        // Buckets forward mapping for render
        val buckets: Array<VisualBucket>,
        // Hints backwards mapping id->slot for deletion
        val hints: MutableMap<String, Int> = HashMap()
    )

    private fun container() = Container(Array(maxSize) { VisualBucket() })
    private val playerStorage = HashMap<Player, Container>()

    /**
     * Adds a new [visual] for the [player]
     *
     * @throws IndexOutOfBoundsException when the [visual]s slot is too big
     */
    fun addVisual(player: Player, visual: VisualElement) {
        if (visual.slot >= maxSize || visual.slot < 0) {
            throw IndexOutOfBoundsException("Slot ${visual.slot} is out of bound [0; $maxSize]")
        }
        val storage = playerStorage.computeIfAbsent(player) { container() }
        storage.buckets[visual.slot].push(visual) { storage.hints.remove(it) }
        storage.hints[visual.id] = visual.slot
    }

    /**
     * Removes every [Visualizable] with the given [id] from [player]
     */
    fun removeVisual(player: Player, id: String) {
        val storage = playerStorage[player] ?: return
        val slot = storage.hints.remove(id) ?: return
        storage.buckets[slot].remove(id)
    }

    fun hasVisual(player: Player): Boolean {
        return playerStorage.containsKey(player)
    }

    /**
     * Removes the [player] from this store and every visual attached to him
     */
    fun removePlayer(player: Player) {
        playerStorage.remove(player)
    }

    /**
     * Renders the visual for every player and [delta] as passed time.
     * Returns the list of all players with their corresponding visuals
     * attached to it rendered as list of strings.
     * A rendered value of null indicates that no visual was present for the slot.
     */
    fun renderAll(delta: Duration): Sequence<Pair<Player, Sequence<String?>>> {
        return playerStorage.asSequence().map {
            val iterator = it.value.buckets.iterator()
            it.key to Sequence { iterator }.map { v ->
                v.render(delta) { s ->
                    it.value.hints.remove(s)
                }
            }
        }
    }
}