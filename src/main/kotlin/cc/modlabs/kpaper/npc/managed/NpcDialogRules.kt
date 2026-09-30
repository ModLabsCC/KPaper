package cc.modlabs.kpaper.npc.managed

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.NamespacedKey
import java.util.ArrayDeque
import kotlin.random.Random

internal fun selectedNpcDialogLines(
    lines: List<String>,
    selection: NpcDialogSelection,
    shuffleBag: NpcDialogShuffleBag = NpcDialogShuffleBag(),
) = if (selection == NpcDialogSelection.RANDOM) listOfNotNull(shuffleBag.next(lines)) else lines

internal class NpcDialogShuffleBag(private val random: Random = Random.Default) {
    private var source = emptyList<String>()
    private val remaining = ArrayDeque<Int>()
    private var lastIndex: Int? = null

    fun next(lines: List<String>): String? {
        if (lines.isEmpty()) {
            source = emptyList()
            remaining.clear()
            lastIndex = null
            return null
        }
        if (source != lines) {
            source = lines.toList()
            remaining.clear()
            lastIndex = null
        }
        if (remaining.isEmpty()) refill()
        return remaining.removeFirst().also { lastIndex = it }.let(source::get)
    }

    private fun refill() {
        val order = source.indices.shuffled(random).toMutableList()
        val previous = lastIndex
        if (previous != null && order.size > 1 && order.first() == previous) {
            val replacement = order.indexOfFirst { it != previous }
            val first = order[0]
            order[0] = order[replacement]
            order[replacement] = first
        }
        remaining.addAll(order)
    }
}

internal fun validatedNpcDialogLine(value: String): String {
    val line = value.trim()
    require(line.length <= 512 && line.none(Char::isISOControl)) { "Die Dialogzeile enthält ungültige Zeichen." }
    val plain = PlainTextComponentSerializer.plainText().serialize(npcDialogComponent(line))
    require(plain.length in 1..160 && plain.none(Char::isISOControl)) {
        "Eine Dialogzeile muss 1 bis 160 sichtbare Zeichen lang sein."
    }
    return line
}

internal fun appendedNpcDialogLines(lines: List<String>, value: String) = lines + validatedNpcDialogLine(value)

internal fun validatedNpcDialogSound(value: String): String =
    NamespacedKey.fromString(value.trim())?.asString() ?: error("Ungültiger Sound-Key '$value'.")

internal fun npcDialogComponent(value: String) = NPC_DIALOG_MINI_MESSAGE.deserialize(value)

private val NPC_DIALOG_MINI_MESSAGE = MiniMessage.builder()
    .tags(TagResolver.resolver(
        StandardTags.color(),
        StandardTags.decorations(),
        StandardTags.gradient(),
        StandardTags.rainbow(),
        StandardTags.reset(),
    ))
    .build()
