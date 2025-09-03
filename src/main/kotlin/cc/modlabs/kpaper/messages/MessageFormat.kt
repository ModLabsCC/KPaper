package cc.modlabs.kpaper.messages

import cc.modlabs.kpaper.extensions.SoundEffect
import cc.modlabs.kpaper.extensions.toTranslated
import dev.fruxz.stacked.text
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class MessageFormat(val format: String) {
    INFO("<color:#CAD3C8>ℹ %s"),
    WARNING("<color:#F8EFBA>⚠ %s"),
    ERROR("<color:#FD7272>🪲 %s"),
    SUCCESS("<color:#a3e877>✔ %s"),
    LOCKED("<gradient:#FC427B:#FD7272>🔒 %s"),
    PLAIN("<color:#CAD3C8>%s");
}

fun MessageFormat.sendPlayer(player: Player, message: String, args: String? = null) {
    if (args == null) player.sendMessage(text(format.format(message)))
    else player.sendMessage(text(format.format(args, message)))
    this.getSound()?.let { player.playSound(player.location, it.sound, it.volume, it.pitch) }
}

fun MessageFormat.sendCommandSender(sender: CommandSender, message: String, args: String? = null) {
    if (args == null) sender.sendMessage(text(format.format(message)))
    else sender.sendMessage(text(format.format(args, message)))
    if (sender is Player) this.getSound()?.let { sender.playSound(sender.location, it.sound, it.volume, it.pitch) }
}

fun MessageFormat.sendCommandSenderWithCooldown(
    sender: CommandSender,
    message: String,
    args: String? = null,
    cooldown: Duration = 3.seconds
) {
    if (sender is Player && LocalMessageCooldown.hasCooldown(sender.uniqueId, message)) return
    if (args == null) sender.sendMessage(text(format.format(message)))
    else sender.sendMessage(text(format.format(args, message)))
    if (sender is Player) this.getSound()?.let { sender.playSound(sender.location, it.sound, it.volume, it.pitch) }

    if (sender is Player) LocalMessageCooldown.addCooldown(sender.uniqueId, message, cooldown)
}

fun MessageFormat.getSound(): SoundEffect? {
    return when (this) {
        MessageFormat.ERROR -> SoundEffect(Sound.BLOCK_PISTON_EXTEND, 0.3f, 1f)
        MessageFormat.SUCCESS -> SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1f)
        MessageFormat.LOCKED -> SoundEffect(Sound.BLOCK_CHEST_LOCKED, 0.3f, 1f)
        else -> null
    }
}

fun CommandSender.sendMessageFormated(message: String, format: MessageFormat, args: String? = null) =
    format.sendCommandSender(this, message, args)

fun CommandSender.sendMessageFormatedWithCooldown(
    message: String,
    format: MessageFormat,
    args: String? = null,
    cooldown: Duration = 3.seconds
) =
    format.sendCommandSenderWithCooldown(this, message, args, cooldown)

fun Player.sendMessageFormated(message: String, format: MessageFormat, args: String? = null) =
    format.sendPlayer(this, message, args)


fun CommandSender.sendInfo(messageKey: String, placeholders: Map<String, Any?> = mapOf(), translated: Boolean = true) =
    sendMessageFormated(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.INFO
    )

fun CommandSender.sendInfoWithCooldown(
    messageKey: String,
    placeholders: Map<String, Any?> = mapOf(),
    translated: Boolean = true,
    cooldown: Duration = 3.seconds
) =
    sendMessageFormatedWithCooldown(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.INFO, cooldown = cooldown
    )

fun CommandSender.sendWarning(
    messageKey: String,
    placeholders: Map<String, Any?> = mapOf(),
    translated: Boolean = true
) =
    sendMessageFormated(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.WARNING
    )

fun CommandSender.sendWarningWithCooldown(
    messageKey: String,
    placeholders: Map<String, Any?> = mapOf(),
    translated: Boolean = true,
    cooldown: Duration = 3.seconds
) =
    sendMessageFormatedWithCooldown(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.WARNING, cooldown = cooldown
    )

fun CommandSender.sendError(messageKey: String, placeholders: Map<String, Any?> = mapOf(), translated: Boolean = true) =
    sendMessageFormated(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.ERROR
    )

fun CommandSender.sendErrorWithCooldown(
    messageKey: String,
    placeholders: Map<String, Any?> = mapOf(),
    translated: Boolean = true,
    cooldown: Duration = 3.seconds
) =
    sendMessageFormatedWithCooldown(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.ERROR, cooldown = cooldown
    )

fun CommandSender.sendSuccess(
    messageKey: String,
    placeholders: Map<String, Any?> = mapOf(),
    translated: Boolean = true
) =
    sendMessageFormated(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.SUCCESS
    )

fun CommandSender.sendSuccessWithCooldown(
    messageKey: String,
    placeholders: Map<String, Any?> = mapOf(),
    translated: Boolean = true,
    cooldown: Duration = 3.seconds
) =
    sendMessageFormatedWithCooldown(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.SUCCESS, cooldown = cooldown
    )

fun CommandSender.sendLocked(
    messageKey: String,
    placeholders: Map<String, Any?> = mapOf(),
    translated: Boolean = true
) =
    sendMessageFormated(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.LOCKED
    )

fun CommandSender.sendLockedWithCooldown(
    messageKey: String,
    placeholders: Map<String, Any?> = mapOf(),
    translated: Boolean = true,
    cooldown: Duration = 3.seconds
) =
    sendMessageFormatedWithCooldown(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey, MessageFormat.LOCKED, cooldown = cooldown
    )

fun CommandSender.sendPlain(messageKey: String, placeholders: Map<String, Any?> = mapOf(), translated: Boolean = true) =
    sendMessageFormated(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey,
        MessageFormat.PLAIN
    )

fun CommandSender.sendPlainWithCooldown(
    messageKey: String,
    placeholders: Map<String, Any?> = mapOf(),
    translated: Boolean = true,
    cooldown: Duration = 3.seconds
) =
    sendMessageFormatedWithCooldown(
        if (translated) messageKey.toTranslated(this, placeholders)
        else messageKey,
        MessageFormat.PLAIN, cooldown = cooldown
    )

fun CommandSender.sendMessageBlock(vararg messages: String, translated: Boolean = true) {
    messages.forEach { sendPlain(it, translated = translated) }
}

internal val String.minecraftTranslated
    get() = "<lang:$this>"
