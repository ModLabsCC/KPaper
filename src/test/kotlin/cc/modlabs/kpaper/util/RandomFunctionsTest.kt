package cc.modlabs.kpaper.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random

class RandomFunctionsTest : FunSpec({

    test("getMultiSeededRandom should produce consistent results with same seed and ints") {
        val seed = 12345L
        
        val random1 = getMultiSeededRandom(seed, 1, 2, 3)
        val random2 = getMultiSeededRandom(seed, 1, 2, 3)
        
        // Same seed and ints should produce the same first random value
        random1.nextInt() shouldBe random2.nextInt()
    }
    
    test("getMultiSeededRandom should produce different results with different ints") {
        val seed = 12345L
        
        val random1 = getMultiSeededRandom(seed, 1, 2, 3)
        val random2 = getMultiSeededRandom(seed, 4, 5, 6)
        
        // Different ints should produce different sequences
        random1.nextInt() shouldNotBe random2.nextInt()
    }
    
    test("getMultiSeededRandom should produce different results with different seeds") {
        val random1 = getMultiSeededRandom(12345L, 1, 2, 3)
        val random2 = getMultiSeededRandom(54321L, 1, 2, 3)
        
        // Different seeds should produce different sequences
        random1.nextInt() shouldNotBe random2.nextInt()
    }
    
    test("getRandomIntAt should be deterministic for same coordinates and seed") {
        val seed = 12345L
        val x = 10
        val y = 20
        val max = 100
        
        val result1 = getRandomIntAt(x, y, seed, max)
        val result2 = getRandomIntAt(x, y, seed, max)
        
        result1 shouldBe result2
    }
    
    test("getRandomIntAt should return values within bounds") {
        val seed = 12345L
        val max = 50
        
        repeat(100) { iteration ->
            val result = getRandomIntAt(iteration, iteration * 2, seed, max)
            result shouldBeInRange 0 until max
        }
    }
    
    test("getRandomIntAt should produce different values for different coordinates") {
        val seed = 12345L
        val max = 100
        
        val result1 = getRandomIntAt(10, 20, seed, max)
        val result2 = getRandomIntAt(30, 40, seed, max)
        
        // High probability that different coordinates produce different values
        result1 shouldNotBe result2
    }
    
    test("getRandomFloatAt should be deterministic for same coordinates and seed") {
        val seed = 12345L
        val x = 10
        val y = 20
        
        val result1 = getRandomFloatAt(x, y, seed)
        val result2 = getRandomFloatAt(x, y, seed)
        
        result1 shouldBe result2
    }
    
    test("getRandomFloatAt should return values between 0.0 and 1.0") {
        val seed = 12345L
        
        repeat(100) { iteration ->
            val result = getRandomFloatAt(iteration, iteration * 2, seed)
            result shouldBeInRange 0.0f..1.0f
        }
    }
    
    test("getRandomFloatAt should produce different values for different coordinates") {
        val seed = 12345L
        
        val result1 = getRandomFloatAt(10, 20, seed)
        val result2 = getRandomFloatAt(30, 40, seed)
        
        // High probability that different coordinates produce different values
        result1 shouldNotBe result2
    }
})