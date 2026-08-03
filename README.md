# Xaero SeedMap

Addon for **Xaero's World Map** (Fabric, Minecraft **26.1.2**). Enter a seed in
the mod's settings, then open a pannable/zoomable preview of that seed's
overworld biomes — without loading the world.

## Status: early scaffold (v0.1.0)

This is a working project skeleton, not a finished, tested mod. What's in place:

- ✅ Mod settings screen with a seed input (`GRAVE`-less keybind, bind it in
  Controls; also reachable via `key.xaeroseedmap.open_settings`).
- ✅ Standalone fullscreen "Seed Map" screen: pan with left-click drag, zoom
  with scroll wheel, live-samples biome colors into a texture.
- ✅ Config persisted to `config/xaeroseedmap.json`.
- ✅ GitHub Actions build (Gradle 9.4.0 (see gradle.properties for exact plugin versions) / JDK 25 / Fabric Loom 1.17-SNAPSHOT, official
  Mojang mappings — 26.1.2 no longer uses Yarn).
- ✅ **`SeedBiomeSampler` (`gen/`) now has a real, complete implementation.**
  It bootstraps a standalone `RegistryAccess` via vanilla's own
  `WorldLoader.InitConfig`/`WorldLoader.load` machinery (the same mechanism
  `CreateWorldScreen` uses internally) when no world is loaded, then builds the
  vanilla overworld `ChunkGenerator`/`BiomeSource` via
  `WorldPresets.getNormalOverworld(...)` and re-seeds it with a fresh
  `RandomState` for the requested seed - no need to join a world first. This
  was cross-checked against real decompiled 26.1.1 sources plus a real,
  actively-maintained 26.2-targeting utility (see code comments for both
  sources). It has **not been run in-game by me** (I only have a build
  sandbox, not a Minecraft client) - if something's off at runtime, this file
  and its bootstrap method are the first place to look.
- ✅ Accurate biome color palette (adapted from a real sibling project, see
  Credits below), hover tooltip showing biome name + coordinates under the
  cursor, and camera position/zoom persist across reopening the map.
- ❌ No button injected into Xaero's actual World Map screen yet. Xaero's
  mods don't publish a documented plugin/addon API for adding UI — reaching
  into their screen needs a Mixin against their real (Fabric, official
  mappings) class name, which needs to be read out of the installed jar
  (e.g. with a decompiler) since it isn't publicly documented. For now, use
  the mod's own keybind (`key.xaeroseedmap.open_seedmap`) to jump straight
  to the seed map. Wiring a button into Xaero's screen is tracked as a
  follow-up.
- ❌ Structures/dungeons/spawners — out of scope for v1 by design (see the
  chat where this was scoped down to biomes-only first).

## Building

Requires JDK 25.

```
gradle build
```

(No committed Gradle wrapper yet — CI uses `gradle/actions/setup-gradle`
directly. Feel free to run `gradle wrapper --gradle-version 9.4.0` locally
and commit the wrapper if you'd rather use `./gradlew`.)

## Runtime dependencies

Install these yourself — they are `compileOnly` here, not bundled:

- Fabric API `0.155.2+26.1.2`
- Xaero's Minimap `26.1.0`+ (Fabric, MC 26.1.2)
- Xaero's World Map `1.41.0`+ (Fabric, MC 26.1.2)

## Credits

The biome color palette in `BiomeColors.java` is adapted from
[TGGamesYT/xaero-seedmap](https://github.com/TGGamesYT/xaero-seedmap), a
sibling project doing the same kind of thing for an older MC/Yarn build. Its
approach is also worth revisiting for the structures roadmap item below - it
uses [cubiomes](https://github.com/Cubitect/cubiomes) (a native C library,
via JNI) to compute structure placements algorithmically and instantly,
without any chunk generation, which would likely be a better fit than trying
to drive vanilla's own structure-placement code for this than what's
described in item 3 below.

## Roadmap

1. ~~Finish `SeedBiomeSampler`'s `RegistryAccess` bootstrap~~ - done; needs
   in-game verification since I can't run a Minecraft client myself.
2. Find & Mixin into Xaero's real World Map screen class to add the in-map
   "Seed Map" button (currently: `HudMod.INSTANCE` is the known 26.1.2 entry
   point for the minimap side; the world map screen class still needs to be
   identified from the installed jar). **Deliberately deferred for now** -
   use the mod's own keybind or the settings screen's "Open Seed Map" button
   in the meantime.
3. Structure/dungeon overlay (villages, strongholds, spawners, etc.) via
   `ChunkGenerator#findNearestMapStructure`-style lookups on the same
   generator instance.
4. Cache sampled regions to disk so re-opening the same seed is instant, and
   cache the bootstrapped `RegistryAccess` across game restarts if the
   ~1s one-time bootstrap cost turns out to be noticeable.
