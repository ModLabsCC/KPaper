package cc.modlabs.kpaper.extensions

import com.mojang.brigadier.context.CommandContext
import dev.fruxz.stacked.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

val CommandContext<CommandSourceStack>.sender: CommandSender
    get() = source.sender

fun Player.soundExecution() {
    playSound(location, Sound.ENTITY_ITEM_PICKUP, .75F, 2F)
    playSound(location, Sound.ITEM_ARMOR_EQUIP_LEATHER, .25F, 2F)
    playSound(location, Sound.ITEM_ARMOR_EQUIP_CHAIN, .1F, 2F)
}

fun UUID.toPlayer(): Player? = Bukkit.getPlayer(this)

fun String.toUUID(): UUID = UUID.fromString(this)

fun CommandSender.sendEmtpyLine() = sendMessage(text(" "))

fun Player.toOfflinePlayer(): OfflinePlayer = Bukkit.getOfflinePlayer(uniqueId)

fun Player.sendDeniedSound() = playSound(location, "minecraft:block.note_block.bass", 1f, 1f)

fun Player.sendSuccessSound() = playSound(location, "minecraft:block.note_block.pling", 1f, 1f)

fun Player.sendTeleportSound() = playSound(location, "minecraft:block.note_block.harp", 1f, 1f)

fun Player.sendOpenSound() = playSound(location, "minecraft:block.note_block.chime", 1f, 1f)

fun Player.maxOutHealth() {
    health = getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
}


fun UUID.toOfflinePlayer(): OfflinePlayer {
    return Bukkit.getOfflinePlayer(this)
}

fun String.toOfflinePlayer(): OfflinePlayer {
    return try {
        UUID.fromString(this).toOfflinePlayer()
    } catch (e: IllegalArgumentException) {
        Bukkit.getOfflinePlayer(this)
    }
}

fun String.toOfflinePlayerIfCached(): OfflinePlayer? {
    return Bukkit.getOfflinePlayerIfCached(this)
}

fun CommandSender.sendMessagePlain(message: String) {
    sendMessage(text(message))
}

fun CommandSender.sendMessagePlain(message: Component) {
    sendMessage(message)
}

fun Collection<Player>.sendMessagePlain(message: String) {
    forEach { it.sendMessagePlain(message) }
}

fun Collection<Player>.sendMessagePlain(message: Component) {
    forEach { it.sendMessagePlain(message) }
}