package cc.modlabs.kpaper.world.area

import cc.modlabs.klassicx.tools.minecraft.StringLocation
import cc.modlabs.kpaper.main.KPlugin
import cc.modlabs.kpaper.main.PluginInstance
import cc.modlabs.kpaper.world.area.model.Area
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.papermc.paper.threadedregions.scheduler.EntityScheduler
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.function.Consumer

class AreaVisualizerTest {
    @AfterEach
    fun cleanUp() = AreaVisualizer.clear()

    @Test
    fun `toggle starts then cancels the same outline`() {
        PluginInstance = mockk<KPlugin>(relaxed = true)
        val task = mockk<ScheduledTask>(relaxed = true)
        val scheduler = mockk<EntityScheduler>()
        val player = mockk<Player>()
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.scheduler } returns scheduler
        every { scheduler.runAtFixedRate(any(), any(), any(), 1, 20) } returns task
        val area = Area("test", point(0.0, 0.0, 0.0), point(5.0, 5.0, 5.0))

        assertTrue(AreaVisualizer.toggle(area, player))
        assertFalse(AreaVisualizer.toggle(area, player))
        verify(exactly = 1) { task.cancel() }
    }

    @Test
    fun `showFor stops after five seconds`() {
        PluginInstance = mockk<KPlugin>(relaxed = true)
        val task = mockk<ScheduledTask>(relaxed = true)
        val action = slot<Consumer<ScheduledTask>>()
        val scheduler = mockk<EntityScheduler>()
        val player = mockk<Player>(relaxed = true)
        every { player.world.name } returns "world"
        every { player.scheduler } returns scheduler
        every { scheduler.runAtFixedRate(any(), capture(action), any(), 1, 20) } returns task
        val area = Area("test", point(0.0, 0.0, 0.0), point(5.0, 5.0, 5.0))

        assertTrue(AreaVisualizer.showFor(area, player))
        repeat(4) { action.captured.accept(task) }
        verify(exactly = 0) { task.cancel() }
        action.captured.accept(task)
        verify(exactly = 1) { task.cancel() }
    }

    private fun point(x: Double, y: Double, z: Double) =
        StringLocation(x, y, z, 0f, 0f, "world")
}