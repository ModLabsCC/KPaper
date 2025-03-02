package cc.modlabs.kpaper.packets

import cc.modlabs.kpaper.extensions.connection
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus

object PacketInterceptor {

    private val packetCallbacks = mutableMapOf<Int, Pair<Class<out Packet<*>>, (Packet<*>) -> Unit>>()
    private var counter = 0

    /**
     * Registers a packet callback.
     * The callback will be called when the given packet is received.
     * Don't forget to inject the packet interceptor into the player before you can receive packets.
     *
     * @param packet the packet to register the callback for
     * @param callback the callback to be called when the packet is received
     * @return the ID of the callback
     */
    fun <T : Packet<*>> registerPacketCallback(packet: Class<T>, callback: (T) -> Unit): Int {
        val id = counter++
        packetCallbacks[id] = Pair(packet, callback as (Packet<*>) -> Unit)
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
    internal fun handlePacket(packet: Packet<*>) {
        packetCallbacks.filter { it.value.first.javaClass == packet.javaClass }.forEach { (id, pair) ->
            pair.second(packet)
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
            if (packet !is Packet<*>) return
            PacketInterceptor.handlePacket(packet)
            super.channelRead(channelHandlerContext, packet)
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
}