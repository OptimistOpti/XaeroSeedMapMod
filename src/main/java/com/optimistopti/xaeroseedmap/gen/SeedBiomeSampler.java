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
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.HashMap;
import java.util.Map;

/**
 * Samples the overworld biome grid for an arbitrary seed WITHOUT loading a world,
 * by re-creating the same overworld ChunkGenerator/BiomeSource vanilla uses when
 * you create a new "Default" world, then re-seeding its RandomState.
 *
 * IMPORTANT / NOT YET RUNTIME-VERIFIED AGAINST 26.1.2:
 * This compiles against real symbols pulled from decompiled 26.1.2 sources
 * (mcsrc.dev) and real 26.1.2-targeting mods on GitHub, but has not been run
 * in-game yet. Two things are known-incomplete on purpose:
 *  1. {@link #resolveRegistryAccess()} only works once the client has joined
 *     a world this session; bootstrapping a standalone RegistryAccess with no
 *     world loaded needs mirroring CreateWorldScreen's WorldLoader.InitConfig
 *     flow, which is a bigger follow-up (see README roadmap).
 *  2. The NoiseBasedChunkGenerator cast below assumes the overworld uses
 *     vanilla noise generation; that's true for a vanilla/default seed but
 *     should be guarded better if this ever needs to support flat worlds etc.
 */
public class SeedBiomeSampler {

    private final BiomeSource biomeSource;
    private final Climate.Sampler climateSampler;
    private final Map<Long, Holder<Biome>> cache = new HashMap<>();

    private SeedBiomeSampler(BiomeSource biomeSource, Climate.Sampler climateSampler) {
        this.biomeSource = biomeSource;
        this.climateSampler = climateSampler;
    }

    public static SeedBiomeSampler create(long seed) {
        RegistryAccess registryAccess = resolveRegistryAccess();

        WorldDimensions dimensions = WorldPresets.createNormalWorldDimensions(registryAccess);
        WorldDimensions.Complete complete = dimensions.bake(registryAccess.registryOrThrow(Registries.LEVEL_STEM));

        LevelStem overworldStem = complete.dimensions().get(LevelStem.OVERWORLD)
                .orElseThrow(() -> new IllegalStateException("No overworld LevelStem registered"))
                .value();

        ChunkGenerator generator = overworldStem.generator();
        BiomeSource biomeSource = generator.getBiomeSource();

        if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            throw new IllegalStateException("Overworld generator is not noise-based: " + generator.getClass());
        }

        RandomState randomState = RandomState.create(
                noiseGenerator.generatorSettings().value(),
                registryAccess.registryOrThrow(Registries.NOISE),
                seed);

        return new SeedBiomeSampler(biomeSource, randomState.sampler());
    }

    private static RegistryAccess resolveRegistryAccess() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            return client.level.registryAccess();
        }
        // TODO(verify): outside of a loaded world there is no ready-made static
        // "builtin" RegistryAccess to grab anymore. Vanilla's own "Create New
        // World" screen builds one on demand via WorldLoader.InitConfig /
        // WorldLoader.load loading vanilla's datapack from the client jar. Mirror
        // that bootstrap here instead of this placeholder.
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
