package cc.modlabs.kpaper.packets

import cc.modlabs.kpaper.extensions.connection
import cc.modlabs.kpaper.extensions.getLogger
import dev.fruxz.ascend.extension.forceCast
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus

object PacketInterceptor {

    private val packetCallbacks =
        mutableMapOf<Int, Pair<Class<out Packet<*>>, (Player, Packet<*>) -> Unit>>()
    private var counter = 0

    /**
     * Registers a packet callback.
     * The callback will be called when the given packet is received.
     * Don't forget to inject the packet interceptor into the player before you can receive packets.
     *
     * @param packet the packet class to register the callback for
     * @param callback the callback to be called when the packet is received
     * @return the ID of the callback
     */
    fun <T : Packet<*>> registerPacketCallback(
        packet: Class<T>,
        callback: (Player, T) -> Unit
    ): Int {
        val id = counter++
        packetCallbacks[id] = Pair(packet) { player, pkt -> callback(player, pkt.forceCast()) }
        return id
    }

    /**
     * Unregisters a packet callback.
     *
     * @param id the ID of the callback to unregister
     */
    fun unregisterPacketCallback(id: Int) {
        packetCallbacks.remove(id)
    }

    /**
     * Handles a received packet. (This method is called by the packet interceptor)
     * Don't call this method directly.
     *
     * @param packet the packet to handle
     */
    @ApiStatus.Internal
    internal fun handlePacket(player: Player, packet: Packet<*>) {
        packetCallbacks.forEach { (_, pair) ->
            val (packetClass, callback) = pair
            if (packetClass.isInstance(packet)) {
                callback(player, packet)
            }
        }
    }
}

/**
 * Injects a packet interceptor into the player.
 * You need to call this method on the player before you can receive packets.
 * This should be called in the PlayerJoinEvent.
 */
fun Player.injectPacketInterceptor() {
    val channelDuplexHandler = object : ChannelDuplexHandler() {
        override fun channelRead(channelHandlerContext: ChannelHandlerContext, packet: Any) {
            super.channelRead(channelHandlerContext, packet)
            try {
                if (packet is Packet<*>) {
                    PacketInterceptor.handlePacket(this@injectPacketInterceptor, packet)
                } else {
                    getLogger().info("Received a packet of type ${packet.javaClass.simpleName} but it is not a packet.")
                }
            } catch (e: Exception) {
                getLogger().warn("An error occurred while handling a packet.")
                e.printStackTrace()
            }
        }
    }
    var channel: Channel? = null
    try {
        channel = connection.connection.channel
        channel.pipeline().addBefore("packet_handler", name, channelDuplexHandler)
    } catch (e: IllegalArgumentException) {
        // Bei Plugin-Neuladen, um doppelte Handler-Namen-Ausnahme zu verhindern
        if (channel == null) {
            return
        }
        if (!channel.pipeline().names().contains(name)) return
        channel.pipeline().remove(name)
        injectPacketInterceptor()
    } catch (_: IllegalAccessException) {
    } catch (_: NoSuchFieldException) {
    }

    getLogger().info(channel.toString())
}