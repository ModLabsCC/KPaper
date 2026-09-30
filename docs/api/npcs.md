# Managed NPCs and skin library

## Responsibility comparison (#14)

Reviewed KPaper's `npc` package, Bedrockia's `npc` and `skins` packages
(local revision `f05417348e2fbe1f69b9a613f81dc8a5ec3da0b5`), and
VoidRoomsBase's `providers/npc` and `data/model/NpcFeatures`.

| Capability | Shared KPaper responsibility | Consuming plugin responsibility |
| --- | --- | --- |
| Rendering | Bedrockia-style virtual players, viewer tracking, packet rotation, equipment, poses and seats | Furniture seat positions and Bedrock-player detection |
| Definitions | IDs, registry, durable storage, world availability, reload and shutdown | Importing existing project records; database ownership |
| Appearance | Names, skins, skin layers, poses, equipment and animations | Curated skin presets, resource-pack assets and branded voices |
| Skins | Central named library, aliases, signed textures, player/MineSkin imports, profile helpers | Geyser authentication/conversion, custom-skull registration and corruption rendering |
| Movement | Bounded pathfinding, weighted waypoints, roaming, patrol, dance and idle animation | Protection-region lookup and custom music detection |
| Interaction | Click/proximity triggers, entry detection, cooldowns, dialogue and registered action callbacks | Permissions, economy, nickname changes, car/investment dealers, advancements |
| VoidRooms behavior | Generic action and trait data passed through the API | MEG_GUARD, merchants, quest bindings, levels, teleports and facilities |
| Administration | Opt-in commands and contextual Brigadier suggestions | Choosing permissions and importing existing NPCs |

The existing `NPC`/`NPCBuilder` API wraps real mannequins. It remains available.
The managed system uses virtual players and shared runtime tasks, as Bedrockia does;
it does not spawn a mannequin or start an AI-monitoring task for every NPC.

Implementation order: #14 comparison, #15 definitions/storage, #17 rendering and
skin library, #18 action API, #13 proximity dialogue, #16 commands, #19 adapters.
Migration of live Bedrockia/VoidRooms storage and renderer ownership is a separate,
explicit consumer change: never run both renderers for the same imported NPCs.

## Enable and use

```kotlin
class MyPlugin : KPlugin() {
    override val featureConfig = featureConfig {
        enableNpcFeatures = true
        enablePacketEventsFeatures = true
    }

    override fun load() {
        npcs.registerTrait("myplugin:guide")
        npcs.registerAction("myplugin:greet") { player, definition, trigger ->
            player.sendMessage(Component.text("Hello from ${definition.id}"))
            true // Continue with the configured generic dialogue.
        }
        // Configure integration hooks here, before NPCs start rendering.
    }

    override fun startup() {
        if (npcs.get("guide") == null) {
            npcs.create("guide", server.worlds.first().spawnLocation)
            npcs.update("guide") { it.copy(
                name = "Guide",
                hologram = "<gold>Welcome guide",
                lookClose = true,
                dialogLines = listOf("<aqua>Welcome!", "Ask me for directions."),
                dialogTrigger = NpcDialogTrigger.BOTH,
                traits = mapOf("myplugin:guide" to "true"),
                action = "myplugin:greet",
            ) }
        }
    }
}
```

Types live in `cc.modlabs.kpaper.npc.managed` and `cc.modlabs.kpaper.skins`.
`NpcService` methods and hooks run on the server thread. Action callbacks receive
defensive copies; use `npcs.update(id) { ... }` to change stored state. Return
`false` when an action opens its own UI or should suppress generic dialogue.
Unregistered actions fail closed. Permission, price, inventory and quest checks
belong inside the consuming plugin's action handler.

`Feature.NPCS` enables the skin store and both commands. A plugin that only needs
skins can set `enableSkinFeatures = true`, without enabling packet rendering.
Both features are disabled by default, preserving existing consumers' commands.
`KPlugin` loads the library and registry before `startup()` and cleans up their
tasks, virtual entities, action callbacks and listeners on disable.

## Storage and skins

`npcs.yml` contains authored positions, appearance, dialogue, movement, namespaced
actions, action data and trait data. Positions use KPaper's existing
`StringLocation`; unloaded worlds remain in the registry and render on WorldLoad.
Runtime walking does not overwrite the authored home position. Changes and
deletes save before changing the registry. Malformed configuration throws;
reload retains the current registry and does not replace bad input with an empty
file. Saves replace files using a temporary file in the same directory.

The skin library reads the consuming plugin's bundled `npc-skins.yml`, its legacy
data-folder `npc-skins.yml`, and then `skins.yml`. KPaper ships no project skin
presets. The layout matches Bedrockia's skin library (`skins.<id>.texture`,
`signature`, `name`, `source`, `mineskin-id`, `player-name`, `created-at` and
`aliases`). `renameId` preserves old IDs as aliases, including repeated renames.
Network imports do not run on the server thread and reuse `MineSkinFetcher`.
The existing `MINESKIN_API_KEY` setup applies; failed imports complete
exceptionally. Pending imports cannot save after library shutdown.

```kotlin
val stored = skins.rememberPlayerProfile(player.name, player.playerProfile)
npcs.setSkin("guide", stored.id)
val profile = SkinProfiles.create(stored) // Also usable for player heads.
val mineSkin = skins.getOrImportMineSkin("existingMineSkinId", "Town Guide")
// Attach completion/error handling; schedule NPC changes on the server thread.
```

Skin changes refresh active NPCs without losing their current movement position.
Native nameplates retain Minecraft's 16-character formatted-name limit; use an
optional `hologram` for a longer label. Dialogue/labels accept formatting tags
without clickable command tags. Sitting uses a virtual mount; labels have
pose-specific heights. Java profile entries are removed after skin loading;
Bedrock viewers keep their entries when the `isBedrock` hook identifies them.

## Administration

Commands require `kpaper.npc.admin` or `kpaper.skins.admin`. Player-location and
held-item operations reject console senders; the remaining commands support
console use. Suggestions filter by the current prefix and include existing NPCs,
skins/aliases, actions, registered traits, waypoints and enum values.

```text
/npc create <id>
/npc list [page] | info <id> | delete <id> | reload | here <id>
/npc name <id> <formatted-name>
/npc hologram <id> <formatted-label> | clear
/npc name-visible <id> <true|false>
/npc skin <id> <skin-id> | clear
/npc skin-layer <id> <cape|jacket|left_sleeve|right_sleeve|left_pants|right_pants|hat|all> <true|false>
/npc equipment <id> <hand|offhand|head|chest|legs|feet> <held|clear>
/npc pose <id> <standing|crouching|sitting|lying|crawling|fall_flying|dying>
/npc rotation <id> <yaw> <pitch>
/npc lookclose <id> <true|false> <range>
/npc animation <id> <talking|arm-swing|idle-look> <true|false>
/npc action <id> <plugin:action> <click|proximity|both> | clear
/npc action-data <id> <key> <value>
/npc trait <id> <plugin:trait> <value> | clear
/npc click-sound <id> <sound-key> <pitch> | clear
/npc dialog <id> add <line> | remove <1-based-line> | clear
/npc dialog <id> trigger <click|proximity|both> | selection <all|random> | range <blocks>
/npc dialog <id> sound <sound-key> [volume pitch] | sound-clear
/npc movement <id> mode <off|roaming|random-return|random|path-circle|path-pingpong|dancing>
/npc movement <id> speed <blocks-per-second> | wait <ticks> | radius <blocks>
/npc movement <id> step <none|stairs-slabs|full-blocks> | unwatched <true|false>
/npc movement <id> region <region-id> | clear
/npc movement <id> music <radius> | off
/npc movement <id> point-add <name> <weight> | point-remove <name> | point-weight <name> <weight>
/skins list [page] | reload
/skins player <Java-name>
/skins mineskin <MineSkin-id> <display-name>
/skins rename <skin-id> <display-name>
/skins rename-id <skin-id> <new-id>
```

Proximity fires on entry into a sphere, once per player/NPC until exit. Dialogue
has a per-player/NPC cooldown; clicks/actions also debounce duplicate packets and
rapid re-entry. Random dialogue uses a shuffle bag to avoid immediate repeats.
Viewer culling is within 96 blocks in the same world; clicks additionally require
visibility and a distance within six blocks. Viewers and proximity membership
reset on world changes, respawn and quit. Queued dialogue is cancelled on edits,
delete, reload, quit and shutdown.

## Project adapters and migration (#19)

The hooks are concrete functions; optional integrations add no dependency to
KPaper:

```kotlin
npcs.hooks.isBedrock = { player -> GeyserPinPrompt.isBedrock(player) }
npcs.hooks.seatLocation = { location -> dj.furnitureSeatLocation(location) }
npcs.hooks.musicPlaying = { location, radius -> dj.isMusicPlayingNear(location, radius) }
npcs.hooks.regionContains = { id, location -> protectionRegions.contains(id, location) }
npcs.hooks.regionKeys = { /* plugin region IDs */ emptyList() }
npcs.hooks.canSee = { player, id -> /* plugin level/viewer rule */ true }

npcs.registerAction("bedrockia:car_dealer") { player, _, _ ->
    cars.menus.openDealership(player)
    false
}
```

The hook example runs in Bedrockia's context; KPaper never calls those services
directly. Register nickname/investment/advancement actions the same way, delegating
to existing handlers with their existing permission and economy checks. Use
`actionData` for project record IDs or parameters, rather than adding city/price
fields to KPaper's definition. Brand voice sounds remain configurable sound keys.
Default music detection covers loaded jukeboxes; custom DJ detection adds to it.
Region-bound roaming is disabled outside the supplied region callback.

For VoidRooms, register an action that resolves its existing NPC/quest record
by `definition.id` or `actionData["source-id"]`, then calls the existing
`QuestService.handleNpcInteraction(player, projectDefinition)`. Keep group,
MEG_GUARD rules, merchant stock, level membership, quest bindings, teleports and
facility actions in VoidRoomsBase. Its level/viewer policy belongs in `canSee`.

Migration sequence:

1. Register project hooks and action callbacks in `load()`.
2. Reuse/import the named skin library. Convert NPC records into `NpcDefinition`
   and call `npcs.put` once during an explicit migration. `skins.put` and
   `npcs.put` allow importing already-fetched signed textures without network work.
3. Map project action names to namespaced callback keys; preserve original record
   IDs in action data. Map common appearance/dialogue/movement fields explicitly.
4. Disable the old renderer for migrated IDs before enabling the managed one.
   Keep the old database and backups until the consumer migration is verified.
5. Test restart/reload, click and proximity behavior, seated labels, movement,
   late world loading, Java skins and Geyser viewers in game.

Bedrock authentication/conversion, skin corruption, Geyser custom-skull sync and
consumer database migrations remain consumer work. No live server changes are
part of this library implementation. Compilation and unit tests do not establish
in-game visual correctness or a measured performance improvement.
