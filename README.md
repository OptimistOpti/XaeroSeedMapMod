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
- ✅ GitHub Actions build (Gradle 9.4.0 / JDK 25 / Fabric Loom 1.15, official
  Mojang mappings — 26.1.2 no longer uses Yarn).
- ⚠️ **`SeedBiomeSampler` (`gen/`) is unfinished on purpose.** Building a
  `BiomeSource`/`ChunkGenerator` for an arbitrary seed *without* a loaded
  world normally means bootstrapping a `RegistryAccess` the way vanilla's
  "Create New World" screen does (see `WorldOpenFlows` / `CreateWorldScreen`
  in the 26.1.2 client sources — browse them at https://mcsrc.dev). I didn't
  have access to the actual 26.1.2 client jar while scaffolding this, so
  that bootstrap is stubbed with a `TODO` and throws instead of guessing
  wrong method names. Everything downstream (GUI, texture rendering, biome
  color table, config, keybinds) is not version-sensitive and should work
  once that one method is filled in.
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

- Fabric API `0.146.1+26.1.2`
- Xaero's Minimap `26.1.0`+ (Fabric, MC 26.1.2)
- Xaero's World Map `1.41.0`+ (Fabric, MC 26.1.2)

## Roadmap

1. Finish `SeedBiomeSampler`'s `RegistryAccess` bootstrap so biome sampling
   actually runs without joining a world first.
2. Find & Mixin into Xaero's real World Map screen class to add the in-map
   "Seed Map" button (currently: `HudMod.INSTANCE` is the known 26.1.2 entry
   point for the minimap side; the world map screen class still needs to be
   identified from the installed jar).
3. Structure/dungeon overlay (villages, strongholds, spawners, etc.) via
   `ChunkGenerator#findNearestMapStructure`-style lookups on the same
   generator instance.
4. Cache sampled regions to disk so re-opening the same seed is instant.
