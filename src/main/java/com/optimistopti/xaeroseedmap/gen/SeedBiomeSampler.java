package com.optimistopti.xaeroseedmap.gen;

import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.DirectoryValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Samples the overworld biome grid for an arbitrary seed WITHOUT loading a world,
 * by re-creating the same overworld ChunkGenerator/BiomeSource vanilla uses for a
 * default "Normal" world, then computing a RandomState for the requested seed.
 *
 * The world-gen call chain (WorldPresets.getNormalOverworld, RegistryAccess,
 * NoiseBasedChunkGenerator, RandomState, BiomeSource) was verified against the
 * real decompiled 26.1.1 sources (github.com/ohnodev/decompiled-minecraft-26-1-1).
 * The standalone RegistryAccess bootstrap below (no world loaded) is adapted
 * from a real, actively-maintained utility class targeting the very close
 * 26.2 build (github.com/EngineHub/MCUtils, GameSetupUtils#getServerRegistries) -
 * this is the same trick vanilla's own "Create New World" screen uses
 * (WorldLoader.InitConfig + WorldLoader.load), just without needing an actual
 * CreateWorldScreen instance around it. Still not run in-game by us - if a
 * class/method here doesn't match 26.1.2 exactly, this is the file to check
 * first.
 */
public class SeedBiomeSampler {

    private static final Lock BOOTSTRAP_LOCK = new ReentrantLock();
    private static volatile RegistryAccess CACHED_REGISTRY_ACCESS;

    private final BiomeSource biomeSource;
    private final Climate.Sampler climateSampler;
    private final Map<Long, Holder<Biome>> cache = new HashMap<>();

    private SeedBiomeSampler(BiomeSource biomeSource, Climate.Sampler climateSampler) {
        this.biomeSource = biomeSource;
        this.climateSampler = climateSampler;
    }

    /**
     * Builds a sampler for the given seed. Safe to call from a background
     * thread (recommended - the first call in a session bootstraps vanilla's
     * data packs, which is not instant).
     */
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

    /**
     * Prefers the currently loaded world's real registries (covers server
     * datapacks) when one is loaded; otherwise bootstraps a standalone,
     * vanilla-only RegistryAccess from scratch and caches it for the rest of
     * the game session, since building it isn't free.
     */
    private static RegistryAccess resolveRegistryAccess() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            return client.level.registryAccess();
        }

        RegistryAccess cached = CACHED_REGISTRY_ACCESS;
        if (cached != null) {
            return cached;
        }

        BOOTSTRAP_LOCK.lock();
        try {
            if (CACHED_REGISTRY_ACCESS != null) {
                return CACHED_REGISTRY_ACCESS;
            }

            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();

            PackRepository packRepository = new PackRepository(new ServerPacksSource(new DirectoryValidator(path -> true)));
            WorldDataConfiguration dataConfiguration = new WorldDataConfiguration(DataPackConfig.DEFAULT, FeatureFlags.DEFAULT_FLAGS);
            WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, dataConfiguration, false, true);
            WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(
                    packConfig, Commands.CommandSelection.INTEGRATED, PermissionSet.ALL_PERMISSIONS);

            WorldStem worldStem = Util.blockUntilDone(executor -> WorldLoader.load(
                    initConfig,
                    dataLoadContext -> {
                        LevelSettings dataGenLevel = new LevelSettings(
                                "Xaero SeedMap preview",
                                GameType.CREATIVE,
                                LevelSettings.DifficultySettings.DEFAULT,
                                true,
                                dataLoadContext.dataConfiguration());
                        RegistryAccess.Frozen immutable = dataLoadContext.datapackDimensions().freeze();
                        PrimaryLevelData saveProperties = new PrimaryLevelData(
                                dataGenLevel, PrimaryLevelData.SpecialWorldProperty.FLAT, Lifecycle.stable());
                        return new WorldLoader.DataLoadOutput<>(saveProperties, immutable);
                    },
                    (resources, managers, registries, cookie) -> {
                        resources.close();
                        return new WorldStem(resources, managers, registries, null);
                    },
                    Util.backgroundExecutor(),
                    executor
            )).join();

            RegistryAccess resolved = worldStem.registries().compositeAccess();
            CACHED_REGISTRY_ACCESS = resolved;
            return resolved;
        } finally {
            BOOTSTRAP_LOCK.unlock();
        }
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
