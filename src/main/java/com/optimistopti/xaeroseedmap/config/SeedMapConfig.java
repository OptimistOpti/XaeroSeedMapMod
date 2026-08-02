package com.optimistopti.xaeroseedmap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON-backed config, kept intentionally dependency-free (no Cloth Config)
 * so the mod only needs Fabric API to run.
 */
public class SeedMapConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("xaeroseedmap.json");

    public static SeedMapConfig INSTANCE = load();

    // --- persisted fields ---
    public String rawSeed = "";
    public int sampleChunkSize = 4;   // biome samples grouped per N x N chunks per pixel (perf/quality tradeoff)
    public int viewRadiusChunks = 512; // how far the seed map view extends from 0,0 by default

    public static SeedMapConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                SeedMapConfig cfg = GSON.fromJson(reader, SeedMapConfig.class);
                if (cfg != null) {
                    return cfg;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new SeedMapConfig();
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mirrors vanilla's seed text field parsing: numeric strings become longs,
     * anything else is hashed with String#hashCode, empty string -> random seed
     * behaviour is not applicable here since we require an explicit seed.
     */
    public long parsedSeed() {
        return parseSeed(rawSeed);
    }

    public static long parseSeed(String raw) {
        if (raw == null || raw.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return raw.hashCode();
        }
    }

    public boolean hasSeed() {
        return rawSeed != null && !rawSeed.isBlank();
    }
}
