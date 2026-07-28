package cc.modlabs.kpaper.world.area

import cc.modlabs.klassicx.tools.minecraft.StringLocation
import cc.modlabs.kpaper.world.area.model.Area
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AreaCacheTest {
    @AfterEach
    fun clear() = AreaCache.clear()

    @Test
    fun `chunk lookup only returns nearby and broad areas in load order`() {
        val broad = area("broad", 0.0, 100_000.0)
        AreaCache.addArea(broad)
        repeat(5_000) { index ->
            AreaCache.addArea(area("area_$index", index * 32.0, index * 32.0 + 1))
        }

        val nearby = AreaCache.candidates("world", 3_200.5, 0.5)

        assertEquals(2, nearby.size)
        assertSame(broad, nearby[0])
        assertEquals("area_100", nearby[1].name)
    }

    private fun area(name: String, minX: Double, maxX: Double) = Area(
        name,
        point(minX, 0.0, 0.0),
        point(maxX, 10.0, 1.0),
    )

    private fun point(x: Double, y: Double, z: Double) =
        StringLocation(x, y, z, 0f, 0f, "world")
}