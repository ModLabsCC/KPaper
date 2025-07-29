package cc.modlabs.kpaper.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.slf4j.Logger

class LoggerFunctionsTest : FunSpec({

    test("getLogger should return a valid Logger instance") {
        val logger = getLogger()
        
        logger shouldNotBe null
        logger.shouldBeInstanceOf<Logger>()
    }
    
    test("getInternalKPaperLogger should return a valid Logger instance") {
        val logger = getInternalKPaperLogger()
        
        logger shouldNotBe null
        logger.shouldBeInstanceOf<Logger>()
    }
    
    test("getInternalKPaperLogger should have correct logger name") {
        val logger = getInternalKPaperLogger()
        
        logger.name shouldBe "cc.modlabs.kpaper"
    }
    
    test("getLogger and getInternalKPaperLogger should return different loggers") {
        val pluginLogger = getLogger()
        val internalLogger = getInternalKPaperLogger()
        
        pluginLogger shouldNotBe internalLogger
        pluginLogger.name shouldNotBe internalLogger.name
    }
    
    test("multiple calls to getLogger should return same logger instance") {
        val logger1 = getLogger()
        val logger2 = getLogger()
        
        logger1 shouldBe logger2
    }
    
    test("multiple calls to getInternalKPaperLogger should return same logger instance") {
        val logger1 = getInternalKPaperLogger()
        val logger2 = getInternalKPaperLogger()
        
        logger1 shouldBe logger2
    }
})