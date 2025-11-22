package cc.modlabs.kpaper.extensions

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.junit.jupiter.api.Test

class UXExtensionTest {

    private fun locWith(world: World = mockk(relaxed = true)): Location = Location(world, 10.0, 64.0, -5.0)

    @Test
    fun `spawnParticle delegates to world with same args`() {
        val world: World = mockk(relaxed = true)
        val loc = locWith(world)

        loc.spawnParticle(
            particle = Particle.CRIT,
            count = 3,
            offsetX = 0.1,
            offsetY = 0.2,
            offsetZ = 0.3,
            speed = 0.0,
            dustOptions = Particle.DustOptions(Color.WHITE, 1.0f)
        )

        verify(exactly = 1) {
            world.spawnParticle(
                Particle.CRIT,
                loc,
                3,
                0.1,
                0.2,
                0.3,
                0.0,
                any<Particle.DustOptions>()
            )
        }
    }

    @Test
    fun `spawnColoredParticle uses DUST and DustOptions`() {
        val world: World = mockk(relaxed = true)
        val loc = locWith(world)

        loc.spawnColoredParticle(Color.RED)

        verify(exactly = 1) {
            world.spawnParticle(
                Particle.DUST,
                loc,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                any<Particle.DustOptions>()
            )
        }
    }
}
