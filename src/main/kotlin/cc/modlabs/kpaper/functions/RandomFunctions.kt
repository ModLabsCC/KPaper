package cc.modlabs.kpaper.functions

import kotlin.math.abs
import kotlin.random.Random

/**
 * Returns a Random object with a seed derived from the given seed and a series of integers.
 *
 * @param seed the base seed used to generate the initial Random object
 * @param ints the additional integers used to further seed the Random object
 * @return a Random object with the seeded value
 */
fun getMultiSeededRandom(seed: Long, vararg ints: Int): Random {
    var seeder = seed
    for (num in ints) {
        seeder = Random(seeder + num).nextLong()
    }
    return Random(seeder)
}

fun getRandomIntAt(x: Int, y: Int, seed: Long, max: Int): Int {
    return abs(getMultiSeededRandom(seed, x, y).nextInt(max))
}

fun getRandomFloatAt(x: Int, y: Int, seed: Long): Float {
    return getMultiSeededRandom(seed, x, y).nextFloat()
}