package cc.modlabs.kpaper.world

import org.bukkit.HeightMap
import org.bukkit.World
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import java.util.*

open class EmptyWorldGenerator : ChunkGenerator() {

    override fun shouldGenerateNoise(): Boolean = false

    override fun shouldGenerateCaves(): Boolean = false

    override fun shouldGenerateDecorations(): Boolean = false

    override fun shouldGenerateMobs(): Boolean = false

    override fun shouldGenerateStructures(): Boolean = false

    override fun getDefaultPopulators(world: World): List<BlockPopulator> = Collections.emptyList()

    override fun getBaseHeight(worldInfo: WorldInfo, random: Random, x: Int, z: Int, heightMap: HeightMap): Int = 80
}