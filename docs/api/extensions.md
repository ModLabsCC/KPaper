# Extensions

KPaper exposes many Kotlin extension helpers for Bukkit/Paper APIs. This page focuses on migration-safe naming and the current preferred API names.

## Naming Migration Quick Reference

Use the new names in new code. Old names remain available as deprecated compatibility bridges.

| Old API | Preferred API |
|---|---|
| `sendEmtpyLine()` | `sendEmptyLine()` |
| `removePersistantDataIf(...)` | `removePersistentDataIf(...)` |
| `str2Loc(serialized)` | `parseLocation(serialized)` |
| `loc2Str(location)` | `locationToString(location)` |
| `loc2BlockStr(location)` | `locationToBlockString(location)` |
| `toSaveAbleString()` | `toSavableString()` |
| `toSaveAbleBlockString()` | `toSavableBlockString()` |
| `toSaveAbleDirectionalString()` | `toSavableDirectionalString()` |
| `Inventory.clone(..., shuffeld = ...)` | `Inventory.cloneCompat(..., shuffled = ...)` |
| `NAMESPACE_GUI_IDENTIFIER` | `GUI_IDENTIFIER_KEY` |
| `NAMESPACE_ITEM_IDENTIFIER` | `ITEM_IDENTIFIER_KEY` |

## Migration Examples

```kotlin
import cc.modlabs.kpaper.extensions.*

// Before
sender.sendEmtpyLine()
val loc = str2Loc(serialized)
val raw = loc2Str(player.location)
val blockRaw = loc2BlockStr(player.location)
val key = NAMESPACE_GUI_IDENTIFIER

// After
sender.sendEmptyLine()
val loc = parseLocation(serialized)
val raw = locationToString(player.location)
val blockRaw = locationToBlockString(player.location)
val key = GUI_IDENTIFIER_KEY
```

```kotlin
import cc.modlabs.kpaper.inventory.ItemBuilder
import cc.modlabs.kpaper.extensions.cloneCompat

// Before
builder.removePersistantDataIf(key, condition = true)
val copy = inventory.clone(shuffeld = true)

// After
builder.removePersistentDataIf(key, condition = true)
val copy = inventory.cloneCompat(shuffled = true)
```

## Notes

- Deprecated bridge APIs intentionally remain for backward compatibility.
- Use IDE quick-fix (`ReplaceWith`) to migrate quickly.
- For naming and package policy, see [Core Naming Conventions](../core/naming-conventions.md).
