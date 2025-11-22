package cc.modlabs.kpaper.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RandomFunctionsTest {

    @Test
    fun `multi seeded random is deterministic`() {
        val r1 = getMultiSeededRandom(1234L, 1, 2, 3)
        val r2 = getMultiSeededRandom(1234L, 1, 2, 3)
        // Same sequence should be produced
        repeat(10) {
            assertEquals(r1.nextInt(), r2.nextInt())
        }
    }

    @Test
    fun `getRandomIntAt returns value within range`() {
        val v = getRandomIntAt(10, -5, 9999L, 7)
        assertTrue(v in 0 until 7)
    }

    @Test
    fun `getRandomFloatAt returns deterministic result for coords and seed`() {
        val a = getRandomFloatAt(42, 24, 2024L)
        val b = getRandomFloatAt(42, 24, 2024L)
        assertEquals(a, b)
        assertTrue(a in 0.0f..1.0f)
    }
}
