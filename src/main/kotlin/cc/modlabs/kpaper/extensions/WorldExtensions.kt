package cc.modlabs.kpaper.extensions

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Entity

fun mainWorld() = Bukkit.getWorlds()[0]
fun chunk(world: World, x: Int, y: Int) = world.getChunkAt(x, y)
fun chunk(block: Block) = chunk(block.world, block.x shr 4, block.z shr 4)

inline fun <reified T : Entity> World.spawn(location: Location): T {
    return spawn(location, T::class.java)
}

inline fun <reified T : Entity> World.getEntitiesByClass(): Collection<T> {
    return getEntitiesByClass(T::class.java)
}

/**
 * Broadcast the given sound to all online players.
 *
 * @param sound The sound to be broadcasted.
 * @param volume The volume of the sound (default value is 1.0f).
 * @param pitch The pitch of the sound (default value is 1.0f).
 */
fun broadcastSound(sound: Sound, volume: Float = 1.0f, pitch: Float = 1.0f) {
    Bukkit.getOnlinePlayers().forEach { it.playSound(it, sound, volume, pitch) }
}


fun World.setSpawnLocation(block: Block): Boolean {
    return setSpawnLocation(block.x, block.y, block.z)
}


fun Location.matches(x: Int, y: Int, z: Int) = blockX == x && blockY == y && blockZ == z