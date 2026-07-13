# Paper 26.2 APIs

KPaper targets Paper 26.2 and Java 25.

## Region-safe scheduling

Use `KPaperScheduler` for global, region, entity, or asynchronous execution:

```kotlin
KPaperScheduler.entity(player) {
    player.sendMessage("Runs on the player's owning region")
}

KPaperScheduler.region(location) {
    location.block.type = Material.STONE
}
```

Tasks owned by a `KPlugin` are cancelled during plugin shutdown.

## Data components and item actions

`ItemBuilder.data`, `unsetData`, and `resetData` expose Paper's public Data Component API. Stable click actions use persistent item data:

```kotlin
val item = ItemBuilder(Material.COMPASS)
    .action("open-menu") { event -> event.isCancelled = true }
    .build()
```

## Native dialogs

```kotlin
val dialog = noticeDialog(Component.text("Welcome")) {
    canCloseWithEscape(true)
    pause(false)
}
player.openDialog(dialog)
```

## Commands, registries, and ticks

Call `registerCommand` during `KPlugin.load()` to register through Paper's lifecycle API. Use `registryValue` for dynamic registry-backed values. `TickService.onStart` and `onEnd` return closeable subscriptions and are cleared on shutdown.

## Serialization migration

Inventory and item serialization now uses bounded `ItemStack.serializeAsBytes()` data. The unsafe Java object-stream format is no longer accepted. Existing stored values must be migrated with a trusted older build in an offline conversion step before upgrading.
