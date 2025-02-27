package cc.modlabs.kpaper.extensions

import cc.modlabs.klassicx.extensions.code
import cc.modlabs.klassicx.translation.Translations
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

fun getUnknownTranslation(key: String): String {
    return "<hover:show_text:'Click to copy'><click:suggest_command:'$key'>$key</click></hover>"
}

val String.minecraftTranslated
    get() = "<lang:$this>"


fun String.toTranslated(player: Player, placeholders: Map<String, Any?> = mapOf(), overwriteLang: String? = null): String {
    return Translations.getTranslation(
        overwriteLang ?: player.locale().code, this, placeholders) ?: getUnknownTranslation(this)
}

fun String.toTranslated(commandSender: CommandSender, placeholders: Map<String, Any?> = mapOf()): String {
    return if (commandSender is Player) {
        toTranslated(commandSender, placeholders)
    } else {
        Translations.getTranslation("en_US", this, placeholders) ?: getUnknownTranslation(this)
    }
}

fun Player.translate(messageKey: String, placeholders: Map<String, Any?> = mapOf()): String {
    return messageKey.toTranslated(this, placeholders)
}

fun CommandSender.translate(messageKey: String, placeholders: Map<String, Any?> = mapOf()): String {
    return messageKey.toTranslated(this, placeholders)
}


fun String.toMinecraftTranslated(vararg args: Any): String {
    return "<lang:$this${
        if (args.isNotEmpty()) {
            args.joinToString(separator = ":", prefix = ":")
        } else {
            ""
        }
    }>"
}