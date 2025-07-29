package cc.modlabs.kpaper.world

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DirectionTest : FunSpec({

    test("Direction enum should have correct values") {
        Direction.MIDDLE.toString() shouldBe "MIDDLE"
        Direction.NORTH.toString() shouldBe "NORTH"
        Direction.NORTH_EAST.toString() shouldBe "NORTH_EAST"
        Direction.EAST.toString() shouldBe "EAST"
        Direction.SOUTH_EAST.toString() shouldBe "SOUTH_EAST"
        Direction.SOUTH.toString() shouldBe "SOUTH"
        Direction.SOUTH_WEST.toString() shouldBe "SOUTH_WEST"
        Direction.WEST.toString() shouldBe "WEST"
        Direction.NORTH_WEST.toString() shouldBe "NORTH_WEST"
    }
    
    test("Direction enum should have all expected compass directions") {
        val directions = Direction.values()
        directions.size shouldBe 9
    }
    
    test("Direction values should be accessible by name") {
        Direction.valueOf("MIDDLE") shouldBe Direction.MIDDLE
        Direction.valueOf("NORTH") shouldBe Direction.NORTH
        Direction.valueOf("NORTH_EAST") shouldBe Direction.NORTH_EAST
        Direction.valueOf("EAST") shouldBe Direction.EAST
        Direction.valueOf("SOUTH_EAST") shouldBe Direction.SOUTH_EAST
        Direction.valueOf("SOUTH") shouldBe Direction.SOUTH
        Direction.valueOf("SOUTH_WEST") shouldBe Direction.SOUTH_WEST
        Direction.valueOf("WEST") shouldBe Direction.WEST
        Direction.valueOf("NORTH_WEST") shouldBe Direction.NORTH_WEST
    }
    
    test("Direction should include 8-directional compass with center") {
        // Verify this is a 9-direction system (8 compass + center)
        val cardinalDirections = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
        val diagonalDirections = listOf(Direction.NORTH_EAST, Direction.SOUTH_EAST, Direction.SOUTH_WEST, Direction.NORTH_WEST)
        val centerDirection = Direction.MIDDLE
        
        cardinalDirections.size shouldBe 4
        diagonalDirections.size shouldBe 4
        
        // All should be distinct
        val allDirections = cardinalDirections + diagonalDirections + centerDirection
        allDirections.size shouldBe 9
        allDirections.distinct().size shouldBe 9
    }
})