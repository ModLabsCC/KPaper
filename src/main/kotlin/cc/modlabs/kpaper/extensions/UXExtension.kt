package cc.modlabs.kpaper.extensions

import dev.fruxz.ascend.extension.time.inWholeMinecraftTicks
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


/**
 * Spawns particles at the specified location.
 *
 * @param particle the type of particle to spawn
 * @param count the number of particles to spawn
 * @param offsetX the x-axis offset of the particle
 * @param offsetY the y-axis offset of the particle
 * @param offsetZ the z-axis offset of the particle
 * @param speed the speed of the particles
 * @param dustOptions the dust options for the particles (if applicable)
 */
fun Location.spawnParticle(
    particle: Particle,
    count: Int,
    offsetX: Double,
    offsetY: Double,
    offsetZ: Double,
    speed: Double,
    dustOptions: Particle.DustOptions
) {
    this.world.spawnParticle(particle, this, count, offsetX, offsetY, offsetZ, speed, dustOptions)
}

/**
 * Spawns a colored particle at the given location.
 *
 * @param color The color of the particle. Defaults to white.
 */
fun Location.spawnColoredParticle(color: Color = Color.WHITE) {
    spawnParticle(Particle.DUST, 1, 0.0, 0.0, 0.0, 0.0, Particle.DustOptions(color, 0.4f))
}

/**
 * Spawns a line of particles from the start location to the end location, with the given color.
 * The line is created by gradually moving the start location towards the end location.
 *
 * @param start The starting location of the particle line.
 * @param end The ending location of the particle line.
 * @param color The color of the particles to be spawned.
 */
fun spawnParticleLine(start: Location, end: Location, clickedFace: BlockFace, color: Color) {
    val blockLoc = end.getBoundingBoxBlockFaceMiddleLocation(clickedFace)
    val playerEyeLocation = start.clone().subtract(0.0, 0.45, 0.0)
    val alpha = getAlphaInRadians(start)

    val offset = getLocationWithWorld(playerEyeLocation, alpha)
    val newPlayerEyeLocation = playerEyeLocation.add(offset.clone().multiply(0.4))

    for (c in 10 downTo 1) {
        createParticle(start, blockLoc, color, newPlayerEyeLocation, c)
    }
}

/**
 * Calculates the value of alpha in radians based on the start location.
 *
 * @param start the starting location
 * @return the value of alpha in radians
 */
fun getAlphaInRadians(start: Location): Double {
    return (start.clone().yaw + 180) / 180 * Math.PI + 0 / 180 * Math.PI
}

/**
 * Calculates a new location with the specified world based on the player's eye location and the given alpha angle.
 *
 * @param playerEyeLocation the player's eye location
 * @param alpha the angle in radians
 * @return a new location with the specified world
 */
fun getLocationWithWorld(playerEyeLocation: Location, alpha: Double): Location {
    return Location(playerEyeLocation.clone().world, cos(alpha), 0.0, sin(alpha))
}

/**
 * Creates a particle at the specified location with the given properties.
 *
 * @param start The starting location for the particle.
 * @param blockLoc The location of the block.
 * @param color The color of the particle.
 * @param playerEyeLocation The location of the player's eyes.
 * @param c The constant value used to calculate the particle's location.
 */
fun createParticle(start: Location, blockLoc: Location, color: Color, playerEyeLocation: Location, c: Int) {
    val t = (c / 10.0)
    val particleLocation = playerEyeLocation.clone().add(getDifferenceLocation(blockLoc, playerEyeLocation).multiply(t))
    start.world.spawnParticle(Particle.DUST, particleLocation, 1, 0.0, 0.0, 0.0, Particle.DustOptions(color, 1f))
}

/**
 * Calculates the difference between two locations.
 *
 * @param location1 The first location.
 * @param location2 The second location.
 * @return The difference between the two locations.
 */
fun getDifferenceLocation(location1: Location, location2: Location): Location {
    return location1.clone().subtract(location2)
}


/**
 * Generates a list of edge points on the block's bounding box.
 *
 * @param location The location of the block.
 * @param stepSize The size of each step for generating points. Defaults to 0.1.
 * @return A list of edge points on the block's bounding box.
 */
fun generateBlockEdgePoints(location: Location, stepSize: Double = 0.1): List<Location> {
    val boundingBox = location.block.boundingBox

    val xCount = ((boundingBox.maxX - boundingBox.minX) / stepSize).toInt()
    val yCount = ((boundingBox.maxY - boundingBox.minY) / stepSize).toInt()
    val zCount = ((boundingBox.maxZ - boundingBox.minZ) / stepSize).toInt()

    val threshold = 0.15

    return mutableListOf<Location>().apply {
        for (xIndex in 0..xCount) {
            val x = boundingBox.minX + (xIndex * stepSize)
            for (yIndex in 0..yCount) {
                val y = boundingBox.minY + (yIndex * stepSize)
                for (zIndex in 0..zCount) {
                    val z = boundingBox.minZ + (zIndex * stepSize)

                    val isEdgeX = x <= boundingBox.minX + threshold || x >= boundingBox.maxX - threshold
                    val isEdgeY = y <= boundingBox.minY + threshold || y >= boundingBox.maxY - threshold
                    val isEdgeZ = z <= boundingBox.minZ + threshold || z >= boundingBox.maxZ - threshold

                    // Checking if the point is on the edge in exactly two dimensions.
                    val isEdge = listOf(isEdgeX, isEdgeY, isEdgeZ).count { it } == 2

                    if (isEdge) {
                        add(Location(location.world, x, y, z))
                    }
                }
            }
        }
    }
}

/**
 * Generates particles in a rectangular space defined by two corner points.
 *
 * @param point1 the first corner point of the space
 * @param point2 the second corner point of the space
 * @param color the color of the particles
 */
fun generateParticlesAroundSpace(point1: Location, point2: Location, color: Color) {
    val points = mutableListOf<Location>()
    val y = point1.y

    val x1 = point1.blockX
    val x2 = point2.blockX

    val z1 = point1.blockZ
    val z2 = point2.blockZ

    val xMin = if (x1 < x2) x1 else x2
    val xMax = if (x1 > x2) x1 else x2

    val zMin = if (z1 < z2) z1 else z2
    val zMax = if (z1 > z2) z1 else z2

    for (x in xMin..xMax) {
        for (z in zMin..zMax) {
            points.add(Location(point1.world, x.toDouble(), y, z.toDouble()))
        }
    }

    points.forEach { it.spawnColoredParticle(color) }
}

/**
 * Spawns a block outline with the specified color at the given location.
 *
 * @param location The location where the block outline will be spawned.
 * @param color The color of the block outline.
 */
fun spawnBlockOutline(location: Location, color: Color) {
    val points = generateBlockEdgePoints(location)
    points.forEach { it.spawnColoredParticle(color) }
}

fun Location.getBoundingBoxBlockFaceMiddleLocation(blockFace: BlockFace): Location {
    val boundingBox = block.boundingBox
    val x =
        boundingBox.minX + ((boundingBox.maxX - boundingBox.minX) / 2) + (blockFace.modX * ((boundingBox.maxX - boundingBox.minX) / 2))
    val y =
        boundingBox.minY + ((boundingBox.maxY - boundingBox.minY) / 2) + (blockFace.modY * ((boundingBox.maxY - boundingBox.minY) / 2))
    val z =
        boundingBox.minZ + ((boundingBox.maxZ - boundingBox.minZ) / 2) + (blockFace.modZ * ((boundingBox.maxZ - boundingBox.minZ) / 2))

    return Location(world, x, y, z)
}



// Melody builder
fun buildMelody(builder: MelodyBuilder.() -> Unit): Melody {
    val melodyBuilder = MelodyBuilder()
    builder(melodyBuilder)
    return melodyBuilder.build()
}

// Melody and Beat classes
class Melody(private val beats: List<Beat>) {
    fun play(player: Player) {
        beats.forEach { it.play(player) }
    }
}

class Beat(private val sounds: List<SoundEffect>) {
    fun play(player: Player) {
        sounds.forEach { it.play(player) }
    }
}

data class SoundEffect(
    val sound: String,
    val volume: Float = 0.4f,
    val pitch: Float = 1f
) {

    constructor(sound: Sound, volume: Float = 0.4f, pitch: Float = 1f) : this(Registry.SOUNDS.getKey(sound).toString(), volume, pitch)
    fun play(player: Player) {
        player.playSound(player.location, sound, volume, pitch)
    }
}

class MelodyBuilder {
    private val beats = mutableListOf<Beat>()

    fun beat(vararg sounds: SoundEffect) {
        beats.add(Beat(sounds.toList()))
    }

    fun build(): Melody = Melody(beats)
}

// Utility function to create SoundEffect
fun soundOf(sound: Sound, pitch: Float = 1.0f, volume: Float = 1.0f): SoundEffect {
    return SoundEffect(sound, pitch, volume)
}

fun PotionEffect(type: PotionEffectType, durationTicks: Int, amplifier: Int = 0, ambient: Boolean = true, particles: Boolean = true, icon: Boolean = true) =
    PotionEffect(
        type,
        durationTicks,
        amplifier,
        ambient,
        particles,
        icon
    )

fun PotionEffect(type: PotionEffectType, duration: Duration = 10.seconds, amplifier: Int = 0, ambient: Boolean = true, particles: Boolean = true, icon: Boolean = true) =
    PotionEffect(type, duration.inWholeMinecraftTicks.toInt(), amplifier, ambient, particles, icon)

fun buildPotionEffect(type: PotionEffectType, duration: Duration = 10.seconds, amplifier: Int = 0, ambient: Boolean = true, particles: Boolean = true, icon: Boolean = true, builder: PotionEffect.() -> Unit) =
    PotionEffect(type, duration, amplifier, ambient, particles, icon).apply(builder)