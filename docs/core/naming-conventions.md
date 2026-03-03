# Naming And Structure Conventions

This document defines the naming and structure rules for KPaper to keep the API consistent, readable, and backward-compatible.

## Package Naming

- Use lowercase package names only.
- Keep feature-oriented package roots under `cc.modlabs.kpaper`:
  - `command`, `coroutines`, `event`, `extensions`, `file`, `game`, `inventory`, `main`, `messages`, `npc`, `party`, `scoreboard`, `util`, `visuals`, `world`.
- Prefer singular package names for feature domains (for example `event`, not `events`) unless a package already exists and changing it would break users.

## Type And File Naming

- Classes/interfaces/object names use `PascalCase`.
- Function/property names use `camelCase`.
- Constants use `UPPER_SNAKE_CASE`.
- Avoid abbreviations unless widely understood (`api`, `uuid`, `url`, `json`).
- File name should match the main public type when a file is type-centric.

## Public API Naming

- Prefer explicit verbs for functions (`parseLocation`, `locationToString`).
- Boolean names must use `is`, `has`, `can`, or `should`.
- Avoid typo-prone names and legacy shorthand (`str2Loc`, `loc2Str`).

## Backward Compatibility Policy

- Never remove/rename public API directly in normal releases.
- For naming fixes:
  1. Add the new canonical API.
  2. Keep the old API as a deprecated bridge.
  3. Use `ReplaceWith(...)` for IDE-assisted migration.
- Keep compatibility bridges for at least one full CalVer cycle unless intentionally breaking.

### Current Migration Map

- `sendEmtpyLine` -> `sendEmptyLine`
- `removePersistantDataIf` -> `removePersistentDataIf`
- `str2Loc` -> `parseLocation`
- `loc2Str` -> `locationToString`
- `loc2BlockStr` -> `locationToBlockString`
- `toSaveAbleString` -> `toSavableString`
- `toSaveAbleBlockString` -> `toSavableBlockString`
- `toSaveAbleDirectionalString` -> `toSavableDirectionalString`
- `Inventory.clone(..., shuffeld=...)` -> `Inventory.cloneCompat(..., shuffled=...)`
- `NAMESPACE_GUI_IDENTIFIER` -> `GUI_IDENTIFIER_KEY`
- `NAMESPACE_ITEM_IDENTIFIER` -> `ITEM_IDENTIFIER_KEY`

## Structure And Maintainability

- Keep helper APIs close to their domain package.
- Prefer small cohesive files over large mixed-purpose files.
- Add KDoc for all public APIs and all deprecated bridge APIs.
- Add tests for new behavior and migration bridges where feasible.

## Performance Notes

- Prefer allocation-light APIs for hot paths.
- Avoid unnecessary reflection and repeated object creation in frequently called extensions.
- Validate and cap untrusted inputs (size/time limits) in serialization and network paths.
