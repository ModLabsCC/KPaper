# KPaper Testing Infrastructure

This document describes the testing infrastructure setup for KPaper using Kotest and MockK as the primary testing frameworks.

## Overview

KPaper now includes comprehensive testing infrastructure to ensure stable releases and feature validation. The testing setup follows modern Kotlin testing practices using:

- **[Kotest](https://kotest.io/)** - The testing framework providing various testing styles and matchers
- **[MockK](https://mockk.io/)** - The mocking framework for Kotlin, used for testing Bukkit/Paper dependent code

## Test Structure

Tests are organized following the same package structure as the main source code:

```
src/test/kotlin/cc/modlabs/kpaper/
├── TestInfrastructureTest.kt         # Basic framework validation
├── event/
│   └── custom/
│       └── BooleanStatusChangeEventTest.kt
├── game/
│   └── countdown/
│       └── CountdownTest.kt          # Abstract class testing with MockK
├── util/
│   ├── IdentityTest.kt               # Interface testing with mocking
│   ├── LoggerFunctionsTest.kt        # Logger validation
│   ├── RandomFunctionsTest.kt        # Pure function testing
│   └── TextUtilsTest.kt              # Extension function testing
└── world/
    └── DirectionTest.kt              # Enum testing
```

## Testing Patterns

### 1. Pure Function Testing
For utility functions with no external dependencies:

```kotlin
test("getRandomIntAt should be deterministic for same coordinates and seed") {
    val seed = 12345L
    val x = 10
    val y = 20
    val max = 100
    
    val result1 = getRandomIntAt(x, y, seed, max)
    val result2 = getRandomIntAt(x, y, seed, max)
    
    result1 shouldBe result2
}
```

### 2. Extension Function Testing
For extension functions that extend existing types:

```kotlin
test("Component.toLegacy should convert colored text component to legacy string with color codes") {
    val component = Component.text("Red Text", NamedTextColor.RED)
    val legacy = component.toLegacy()
    
    legacy shouldBe "§cRed Text"
}
```

### 3. MockK for Bukkit Dependencies
For testing classes that depend on Bukkit/Paper APIs:

```kotlin
test("Countdown should initialize with correct default duration") {
    val mockGame = mockk<GamePlayers>()
    val defaultDuration = 60
    
    val countdown = TestCountdown(mockGame, defaultDuration)
    
    countdown.defaultDuration shouldBe defaultDuration
    countdown.game shouldBe mockGame
}
```

### 4. Abstract Class Testing
For testing abstract classes by creating concrete test implementations:

```kotlin
class TestCountdown(game: GamePlayers, defaultDuration: Int) : Countdown(game, defaultDuration) {
    var startCalled = false
    var stopCalled = false
    
    override fun start() {
        startCalled = true
    }
    
    override fun stop() {
        stopCalled = true
    }
}
```

### 5. Interface Testing
For testing interface implementations:

```kotlin
class TestIdentity(override val identityKey: Key) : Identity<String>

test("Identity should return correct namespace from identityKey") {
    val key = Key.key("minecraft", "stone")
    val identity = TestIdentity(key)
    
    identity.namespace() shouldBe "minecraft"
}
```

## Running Tests

To run the tests, use the Gradle test task:

```bash
./gradlew test
```

To run tests for a specific package:

```bash
./gradlew test --tests "cc.modlabs.kpaper.util.*"
```

## Coverage Areas

The current test suite covers:

- **Utility Functions**: Deterministic behavior validation, edge case handling
- **Extension Functions**: Proper integration with existing Kotlin/Java types
- **Enum Classes**: Value validation and access patterns
- **Abstract Classes**: Core functionality through concrete test implementations
- **Interface Implementations**: Contract compliance and behavior validation
- **Event Classes**: Proper initialization and state management
- **Logger Functions**: Instance creation and naming validation

## Best Practices

1. **Test Naming**: Use descriptive test names that explain the behavior being tested
2. **Arrange-Act-Assert**: Structure tests with clear setup, execution, and validation phases
3. **Mock Responsibly**: Use mocks for external dependencies, avoid mocking the class under test
4. **Pure Functions First**: Test pure functions without mocks when possible
5. **Edge Cases**: Include tests for boundary conditions and error scenarios
6. **Deterministic Tests**: Ensure tests produce consistent results across runs

## Future Enhancements

- Integration tests for complete feature workflows
- Performance benchmarks for critical path functions
- Property-based testing for complex algorithms
- Test containers for database/file system dependent features

## Dependencies

The testing infrastructure uses the following versions (configured in `build.gradle.kts`):

```kotlin
val koTestVersion = "6.0.0.M1"
val mockkVersion = "1.13.16"

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}
```

Tests are configured to run with JUnit Platform:

```kotlin
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```