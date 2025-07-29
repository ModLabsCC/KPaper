package cc.modlabs.kpaper.extensions

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

operator fun Location.component1() = x
operator fun Location.component2() = y
operator fun Location.component3() = z
operator fun Location.component4() = yaw
operator fun Location.component5() = pitch

operator fun Block.component1() = x
operator fun Block.component2() = y
operator fun Block.component3() = z

infix fun Block.eqType(block: Block) = type == block.type
infix fun Block.eqType(material: Material) = type == material

fun Location.dropItem(item: ItemStack) = world.dropItem(this, item)
fun Location.dropItemNaturally(item: ItemStack) = world.dropItemNaturally(this, item)

fun Location.spawnArrow(direction: Vector, speed: Float, spread: Float) =
    world.spawnArrow(this, direction, speed, spread)

fun Location.generateTree(type: TreeType) = world.generateTree(this, type)

@Suppress("DEPRECATION")
fun Location.generateTree(type: TreeType, delegate: BlockChangeDelegate) = world.generateTree(this, type, delegate)

fun Location.strikeLightning() = world.strikeLightning(this)
fun Location.strikeLightningEffect() = world.strikeLightningEffect(this)

fun Location.playEffect(effect: Effect, data: Int) = world.playEffect(this, effect, data)
fun Location.playEffect(effect: Effect, data: Int, radius: Int) = world.playEffect(this, effect, data, radius)
fun <T> Location.playEffect(effect: Effect, data: T) = world.playEffect(this, effect, data)
fun <T> Location.playEffect(effect: Effect, data: T, radius: Int) = world.playEffect(this, effect, data, radius)

fun Location.playSound(sound: Sound, volume: Float, pitch: Float) = world.playSound(this, sound, volume, pitch)

fun Location.toCoords() = "$blockX;$blockY;$blockZ"

fun Location.surroundings(): List<Location> {
    val list = mutableListOf<Location>()
    for (x in -1..1) {
        for (z in -1..1) {
            if (x == 0 && z == 0) continue
            list.add(this.clone().add(x.toDouble(), 0.0, z.toDouble()))
        }
    }
    return list
}


fun str2Loc(str: String): Location {
    val args = str.split(",").toTypedArray()
    val worldName = args[0]
    val world = Bukkit.getWorld(worldName) ?: throw IllegalArgumentException("World $worldName not found")

    if (args.size == 4) {
        return Location(world, args[1].toDouble(), args[2].toDouble(), args[3].toDouble())
    } else if (args.size == 6) {
        return Location(
            world,
            args[1].toDouble(),
            args[2].toDouble(),
            args[3].toDouble(),
            args[4].toFloat(),
            args[5].toFloat()
        )
    }
    return Location(Bukkit.getWorlds()[0], 0.5, 10.0, 0.5)
}

fun loc2BlockStr(loc: Location): String {
    var location = ""
    location += loc.world.name + ","
    location += loc.blockX.toString() + ","
    location += loc.blockY.toString() + ","
    location += loc.blockZ
    return location
}

fun loc2Str(location: Location): String {
    var loc = ""
    loc += location.world.name + ","
    loc += location.x.toString() + ","
    loc += location.y.toString() + ","
    loc += location.z.toString() + ","
    loc += location.yaw.toString() + ","
    loc += location.pitch
    return loc
}


fun Location.asSafeLocation(): Location {
    // Now we test if the location (2 blocks high) is occupied and if so, we move up until we find a free spot
    var location = this.clone()
    while ((location.block.type.isSolid || location.clone().add(0.0, 1.0, 0.0).block.type.isSolid)) {
        location = location.add(0.0, 1.0, 0.0)
    }
    return location
}

fun Location.asSafeLocationOrNull(): Location? {
    // Now we test if the location (2 blocks high) is occupied and if so, we move up until we find a free spot
    var location = this.clone()
    while ((location.block.type.isSolid || location.clone().add(0.0, 1.0, 0.0).block.type.isSolid)) {
        location = location.add(0.0, 1.0, 0.0)
    }
    if(location.y > 319) return null
    return location
}

fun Location.isInRadius(middle: Location, radius: Int): Boolean {
    return this.x in middle.x - radius..middle.x + radius && this.z in middle.z - radius..middle.z + radius
}

fun Location.inWorld(world: World) = this.clone().apply { this.world = world }

fun Location.inWorld(worlds: String) = this.clone().apply { this.world = Bukkit.getWorld(worlds.lowercase()) }

fun Location.toNorth(): Location {
    return this.apply { this.yaw = 180.0f; this.pitch = 0f }
}

fun Location.toSaveAbleString(): String {
    return "${world.name};$x;$y;$z"
}

fun Location.toSaveAbleBlockString(): String {
    return "${world.name};${blockX};${blockY};${blockZ}"
}

fun Location.toSaveAbleDirectionalString(): String {
    return "${world.name};$x;$y;$z;$yaw;$pitch"
}