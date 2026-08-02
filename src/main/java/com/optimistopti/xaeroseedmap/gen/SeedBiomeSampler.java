package com.optimistopti.xaeroseedmap.gen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.HashMap;
import java.util.Map;

/**
 * Samples the overworld biome grid for an arbitrary seed WITHOUT loading a world,
 * by re-creating the same overworld ChunkGenerator/BiomeSource vanilla uses when
 * you create a new "Default" world, then re-seeding it.
 *
 * IMPORTANT / NOT YET VERIFIED AGAINST 26.1.2 DECOMPILED SOURCE:
 * Minecraft 26.1.2 dropped Yarn in favour of Mojang's official mappings, and the
 * exact class/method names below are our best reconstruction based on the last
 * few Yarn-mapped releases (they were historically very close to Mojang's own
 * names for world-gen). Before shipping, cross-check every symbol used here
 * against https://mcsrc.dev for 26.1.2 and fix anything that doesn't compile -
 * this is the single riskiest file in the mod. Everything else (config, GUI,
 * mixin hook into Xaero's World Map) does not depend on world-gen internals
 * and should be much more stable.
 *
 * Caching: biome lookups are grouped per {@link com.optimistopti.xaeroseedmap.config.SeedMapConfig#sampleChunkSize}
 * chunks to keep the preview responsive; each cell samples a single biome
 * column at its center rather than every block.
 */
public class SeedBiomeSampler {

    private final ChunkGenerator chunkGenerator;
    private final BiomeSource biomeSource;
    private final Climate.Sampler climateSampler;
    private final Map<Long, Holder<Biome>> cache = new HashMap<>();

    private SeedBiomeSampler(ChunkGenerator chunkGenerator, BiomeSource biomeSource, Climate.Sampler climateSampler) {
        this.chunkGenerator = chunkGenerator;
        this.biomeSource = biomeSource;
        this.climateSampler = climateSampler;
    }

    /**
     * Builds a sampler for the given seed using the client's currently loaded
     * dynamic registries (available as soon as the client has connected to any
     * world at least once this session; RegistryAccess is otherwise the
     * built-in vanilla set, which is fine since we only need default overworld
     * generation settings, not any specific server's datapacks).
     */
    public static SeedBiomeSampler create(long seed) {
        RegistryAccess registryAccess = resolveRegistryAccess();

        WorldDimensions dimensions = WorldPresets.createNormalWorldDimensions(registryAccess);
        WorldDimensions.Complete complete = dimensions.bake(registryAccess.registryOrThrow(Registries.LEVEL_STEM));
        WorldOptions worldOptions = new WorldOptions(seed, true, false);

        // Re-seed every generator that references randomised structure/biome
        // placement so results match "Create New World" with this seed.
        WorldDimensions.Complete seeded = complete.withOverworldSeed(seed);
        LevelStem overworldStem = seeded.dimensions().get(LevelStem.OVERWORLD);

        ChunkGenerator generator = overworldStem.generator();
        BiomeSource biomeSource = generator.getBiomeSource();
        Climate.Sampler climateSampler = generator.climateSampler();

        return new SeedBiomeSampler(generator, biomeSource, climateSampler);
    }

    private static RegistryAccess resolveRegistryAccess() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            return client.level.registryAccess();
        }
        // TODO(verify): outside of a loaded world there is no ready-made static
        // "builtin" RegistryAccess to grab anymore (that field was removed years
        // ago). Vanilla's own "Create New World" screen builds one on demand via
        // WorldOpenFlows / WorldLoader.InitConfig loading vanilla's datapack from
        // the client jar. Mirror that bootstrap here (see CreateWorldScreen /
        // WorldOpenFlows in the 26.1.2 client sources on mcsrc.dev) instead of
        // this placeholder, which will not compile as-is.
        throw new IllegalStateException(
                "SeedBiomeSampler needs a standalone RegistryAccess bootstrap when no world is loaded - " +
                "see TODO above. Join any world once this session as a temporary workaround.");
    }

    /**
     * Returns the biome at the given block column (world X/Z, biome sampled at y=64
     * which is a reasonable overworld surface-ish default for a 2D preview map).
     */
    public Holder<Biome> getBiome(int blockX, int blockZ) {
        int quartX = QuartPos.fromBlock(blockX);
        int quartY = QuartPos.fromBlock(64);
        int quartZ = QuartPos.fromBlock(blockZ);
        long key = (((long) quartX) << 32) ^ (quartZ & 0xffffffffL);
        return cache.computeIfAbsent(key, k -> biomeSource.getNoiseBiome(quartX, quartY, quartZ, climateSampler));
    }

    public ResourceKey<Biome> getBiomeKey(int blockX, int blockZ) {
        return getBiome(blockX, blockZ).unwrapKey().orElse(null);
    }

    public void clearCache() {
        cache.clear();
    }
}
