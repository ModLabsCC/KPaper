package cc.modlabs.kpaper.game.countdown

import cc.modlabs.kpaper.game.GamePlayers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.bukkit.scheduler.BukkitTask

class CountdownTest : FunSpec({

    class TestCountdown(game: GamePlayers, defaultDuration: Int) : Countdown(game, defaultDuration) {
        var startCalled = false
        var stopCalled = false
        
        override fun start() {
            startCalled = true
        }
        
        override fun stop() {
            stopCalled = true
            if (::countdown.isInitialized) {
                countdown.cancel()
            }
        }
    }

    test("Countdown should initialize with correct default duration") {
        val mockGame = mockk<GamePlayers>()
        val defaultDuration = 60
        
        val countdown = TestCountdown(mockGame, defaultDuration)
        
        countdown.defaultDuration shouldBe defaultDuration
        countdown.duration shouldBe defaultDuration
        countdown.game shouldBe mockGame
    }
    
    test("Countdown duration can be modified") {
        val mockGame = mockk<GamePlayers>()
        val countdown = TestCountdown(mockGame, 60)
        
        countdown.duration = 30
        countdown.duration shouldBe 30
        countdown.defaultDuration shouldBe 60 // Should remain unchanged
    }
    
    test("Countdown start method should be called") {
        val mockGame = mockk<GamePlayers>()
        val countdown = TestCountdown(mockGame, 60)
        
        countdown.start()
        countdown.startCalled shouldBe true
    }
    
    test("Countdown stop method should be called") {
        val mockGame = mockk<GamePlayers>()
        val countdown = TestCountdown(mockGame, 60)
        
        countdown.stop()
        countdown.stopCalled shouldBe true
    }
    
    test("Countdown should handle BukkitTask assignment") {
        val mockGame = mockk<GamePlayers>()
        val mockTask = mockk<BukkitTask>()
        val countdown = TestCountdown(mockGame, 60)
        
        countdown.countdown = mockTask
        countdown.countdown shouldBe mockTask
    }
    
    test("Multiple countdown instances should be independent") {
        val mockGame1 = mockk<GamePlayers>()
        val mockGame2 = mockk<GamePlayers>()
        
        val countdown1 = TestCountdown(mockGame1, 30)
        val countdown2 = TestCountdown(mockGame2, 60)
        
        countdown1.duration = 15
        countdown2.duration = 90
        
        countdown1.duration shouldBe 15
        countdown2.duration shouldBe 90
        countdown1.defaultDuration shouldBe 30
        countdown2.defaultDuration shouldBe 60
    }
})