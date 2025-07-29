package cc.modlabs.kpaper

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A basic test to verify the test infrastructure is working properly
 */
class TestInfrastructureTest : FunSpec({

    test("Kotest framework should be working") {
        val result = 2 + 2
        result shouldBe 4
    }
    
    test("Kotlin basics should work in test environment") {
        val list = listOf(1, 2, 3, 4, 5)
        val doubled = list.map { it * 2 }
        
        doubled shouldBe listOf(2, 4, 6, 8, 10)
    }
    
    test("String operations should work") {
        val text = "Hello, KPaper!"
        text.length shouldBe 14
        text.lowercase() shouldBe "hello, kpaper!"
    }
})