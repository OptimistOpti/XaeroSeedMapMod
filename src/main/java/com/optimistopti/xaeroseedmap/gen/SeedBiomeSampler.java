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
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.HashMap;
import java.util.Map;

/**
 * Samples the overworld biome grid for an arbitrary seed WITHOUT loading a world,
 * by re-creating the same overworld ChunkGenerator/BiomeSource vanilla uses for a
 * default "Normal" world, then computing a RandomState for the requested seed.
 *
 * Verified against the real decompiled 26.1.1 sources
 * (github.com/ohnodev/decompiled-minecraft-26-1-1, close enough to 26.1.2 for
 * these worldgen classes which aren't part of the GUI/rendering rework) -
 * RegistryAccess#lookupOrThrow, WorldPresets#getNormalOverworld,
 * NoiseBasedChunkGenerator#generatorSettings, RandomState#create/#sampler,
 * BiomeSource#getNoiseBiome and ResourceKey#identifier all confirmed to exist
 * with these exact signatures. The one still-unverified piece is
 * {@link #resolveRegistryAccess()} - see its TODO.
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

        LevelStem overworldStem = WorldPresets.getNormalOverworld(registryAccess);
        ChunkGenerator generator = overworldStem.generator();
        BiomeSource biomeSource = generator.getBiomeSource();

        if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            throw new IllegalStateException("Overworld generator is not noise-based: " + generator.getClass());
        }

        RandomState randomState = RandomState.create(
                noiseGenerator.generatorSettings().value(),
                registryAccess.lookupOrThrow(Registries.NOISE),
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
