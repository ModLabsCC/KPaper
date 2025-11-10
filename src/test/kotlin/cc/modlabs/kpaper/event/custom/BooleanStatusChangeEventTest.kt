package cc.modlabs.kpaper.event.custom

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BooleanStatusChangeEventTest : FunSpec({

    // Concrete implementation for testing the abstract class
    class TestBooleanStatusChangeEvent(newValue: Boolean, isAsync: Boolean = false) : BooleanStatusChangeEvent(newValue, isAsync)

    test("BooleanStatusChangeEvent should initialize with correct newValue") {
        val newValue = true
        val event = TestBooleanStatusChangeEvent(newValue)
        
        event.newValue shouldBe newValue
    }
    
    test("BooleanStatusChangeEvent should handle false value") {
        val event = TestBooleanStatusChangeEvent(false)
        
        event.newValue shouldBe false
    }
    
    test("BooleanStatusChangeEvent should handle true value") {
        val event = TestBooleanStatusChangeEvent(true)
        
        event.newValue shouldBe true
    }
    
    test("BooleanStatusChangeEvent should handle async flag correctly") {
        val syncEvent = TestBooleanStatusChangeEvent(true, false)
        val asyncEvent = TestBooleanStatusChangeEvent(true, true)
        
        syncEvent.isAsynchronous shouldBe false
        asyncEvent.isAsynchronous shouldBe true
    }
    
    test("BooleanStatusChangeEvent should allow modification of newValue") {
        val event = TestBooleanStatusChangeEvent(true)
        
        event.newValue = false
        event.newValue shouldBe false
    }
    
    test("BooleanStatusChangeEvent should default to synchronous") {
        val event = TestBooleanStatusChangeEvent(true)
        
        event.isAsynchronous shouldBe false
    }
})