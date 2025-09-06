package cc.modlabs.kpaper.extensions

import cc.modlabs.kpaper.main.PluginInstance
import dev.fruxz.stacked.text
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shortcut to get all online players.
 * @see Bukkit.getOnlinePlayers
 */
val onlinePlayers: Collection<Player> get() = Bukkit.getOnlinePlayers()

/**
 * Shortcut to get a collection of all
 * online players plus the console command sender.
 */
val onlineSenders: Collection<CommandSender> get() = Bukkit.getOnlinePlayers().plus(Bukkit.getConsoleSender())

/**
 * Shortcut to get the Server.
 * @see Bukkit.getServer
 */
val server get() = Bukkit.getServer()

/**
 * Shortcut to get the PluginManager.
 * @see Bukkit.getPluginManager
 */
val pluginManager get() = Bukkit.getPluginManager()

/**
 * Broadcasts a message ([msg]) on the server.
 * @return the number of recipients
 * @see Bukkit.broadcastMessage
 */
fun broadcast(msg: String) = Bukkit.getServer().broadcast(text(msg))

/**
 * Broadcasts a message ([msg]) on the server.
 * @return the number of recipients
 * @see Bukkit.broadcastMessage
 */
fun broadcast(msg: Component) = Bukkit.getServer().broadcast(msg)

/**
 * Shortcut to get the ConsoleSender.
 * @see Bukkit.getConsoleSender
 */
val console get() = Bukkit.getConsoleSender()

/**
 * Shortcut for creating a new [NamespacedKey]
 */
fun pluginKey(key: String) = NamespacedKey(PluginInstance, key)

/**
 * Shortcut to get a collection of all worlds
 */
val worlds: List<World> get() = Bukkit.getWorlds()

val Int.minecraftTicks: Duration
    get() = (this.toLong() * 50).milliseconds

val Long.minecraftTicks: Duration
    get() = (this * 50).milliseconds

val Double.minecraftTicks: Duration
    get() = (this * 50).milliseconds

val Float.minecraftTicks: Duration
    get() = (this * 50F).toDouble().milliseconds