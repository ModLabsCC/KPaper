package cc.modlabs.kpaper.world.area.model

import cc.modlabs.klassicx.tools.minecraft.StringLocation
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AreaTest {
    @Test
    fun `two points remain a cuboid`() {
        val area = Area("legacy", point(0.0, 0.0, 0.0), point(4.0, 10.0, 4.0))

        assertTrue(area.contains("world", 2.0, 5.0, 2.0))
        assertFalse(area.contains("world", 5.0, 5.0, 2.0))
        assertFalse(area.contains("other", 2.0, 5.0, 2.0))
    }

    @Test
    fun `concave polygon includes its edges and excludes its notch`() {
        val area = Area(
            "concave",
            listOf(
                point(0.0, 0.0, 0.0),
                point(4.0, 10.0, 0.0),
                point(4.0, 0.0, 4.0),
                point(2.0, 10.0, 2.0),
                point(0.0, 0.0, 4.0),
            ),
        )

        assertTrue(area.contains("world", 3.0, 5.0, 1.0))
        assertTrue(area.contains("world", 3.0, 5.0, 3.0))
        assertFalse(area.contains("world", 2.0, 5.0, 3.0))
        assertFalse(area.contains("world", 3.0, 11.0, 1.0))
    }

    private fun point(x: Double, y: Double, z: Double) =
        StringLocation(x, y, z, 0f, 0f, "world")
}