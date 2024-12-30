package de.joker.kpaper.world

import com.destroystokyo.paper.MaterialSetTag
import org.bukkit.HeightMap
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import java.util.*
import kotlin.random.asKotlinRandom

open class FlatWorldGenerator : ChunkGenerator() {

    override fun shouldGenerateNoise(): Boolean = false

    override fun shouldGenerateCaves(): Boolean = false

    override fun shouldGenerateDecorations(): Boolean = false

    override fun shouldGenerateMobs(): Boolean = false

    override fun shouldGenerateStructures(): Boolean = false

    override fun getDefaultPopulators(world: World): List<BlockPopulator> = Collections.emptyList()

    override fun getBaseHeight(worldInfo: WorldInfo, random: Random, x: Int, z: Int, heightMap: HeightMap): Int = 192

    override fun generateBedrock(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        for (x in 0..15) {
            for (z in 0..15) {
                chunkData.setBlock(x, -63, z, Material.BEDROCK)
            }
        }
    }

    override fun generateSurface(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int, chunkData: ChunkData) {
        val ktRandom = random.asKotlinRandom()
        for (x in 0..15) {
            for (z in 0..15) {
                for (y in -62..192) {
                    when {
                        y < 190 -> chunkData.setBlock(x, y, z, MaterialSetTag.BASE_STONE_OVERWORLD.values.random(ktRandom))
                        y == 190 -> chunkData.setBlock(x, y, z, Material.MOSS_BLOCK)
                        y == 191 -> chunkData.setBlock(x, y, z, Material.GRASS_BLOCK)
                        else -> random.nextDouble().let {
                            when {
                                it < 0.1 -> chunkData.setBlock(x, y, z, Material.SHORT_GRASS)
                                it < 0.2 -> run {
                                    chunkData.setBlock(x, y, z, Material.TALL_GRASS)
                                }
                                it < 0.25 -> chunkData.setBlock(x, y, z, Material.FERN)
                            }
                        }
                    }
                }
            }
        }
    }
}