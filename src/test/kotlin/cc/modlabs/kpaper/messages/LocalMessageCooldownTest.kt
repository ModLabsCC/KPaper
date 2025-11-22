package cc.modlabs.kpaper.messages

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class LocalMessageCooldownTest {

    @Test
    fun `cooldown is active immediately after adding and expires after duration`() {
        val player = UUID.randomUUID()
        val msg = "hello.world"

        LocalMessageCooldown.addCooldown(player, msg, 50.milliseconds)
        assertTrue(LocalMessageCooldown.hasCooldown(player, msg))

        Thread.sleep(60)
        assertFalse(LocalMessageCooldown.hasCooldown(player, msg))
    }

    @Test
    fun `different message is not blocked`() {
        val player = UUID.randomUUID()
        LocalMessageCooldown.addCooldown(player, "a", Duration.parse("50ms"))
        assertTrue(LocalMessageCooldown.hasCooldown(player, "a"))
        assertFalse(LocalMessageCooldown.hasCooldown(player, "b"))
    }
}
