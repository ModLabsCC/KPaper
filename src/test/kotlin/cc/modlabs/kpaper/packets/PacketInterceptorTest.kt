package cc.modlabs.kpaper.packets

import io.mockk.mockk
import io.mockk.verify
import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PacketInterceptorTest {

    @Test
    fun `register and handle invokes callback for matching type`() {
        val player: Player = mockk(relaxed = true)
        val packet: Packet<*> = mockk(relaxed = true)

        var called = 0
        val id = PacketInterceptor.registerPacketCallback(Packet::class.java) { p, pkt ->
            called++
        }

        PacketInterceptor.handlePacket(player, packet)
        assertEquals(1, called)

        // Cleanup
        PacketInterceptor.unregisterPacketCallback(id)
    }

    @Test
    fun `unregister prevents further invocations`() {
        val player: Player = mockk(relaxed = true)
        val packet: Packet<*> = mockk(relaxed = true)

        var called = 0
        val id = PacketInterceptor.registerPacketCallback(Packet::class.java) { _, _ -> called++ }

        PacketInterceptor.handlePacket(player, packet)
        PacketInterceptor.unregisterPacketCallback(id)
        PacketInterceptor.handlePacket(player, packet)

        assertEquals(1, called)
    }
}
