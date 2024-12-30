package de.joker.kpaper.main.extensions

import com.mojang.brigadier.context.CommandContext
import dev.fruxz.stacked.text
import io.papermc.paper.command.brigadier.CommandSourceStack
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