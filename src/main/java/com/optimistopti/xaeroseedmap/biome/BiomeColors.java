package com.optimistopti.xaeroseedmap.biome;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.HashMap;
import java.util.Map;

/**
 * Flat lookup table of ARGB colors per vanilla biome, in the spirit of
 * Amidst/Chunkbase seed maps. Modded/datapack biomes fall back to a neutral
 * grey. Extend this table freely - it's pure data, no world-gen coupling.
 */
public final class BiomeColors {

    private static final Map<ResourceKey<Biome>, Integer> COLORS = new HashMap<>();
    private static final int FALLBACK = 0xFF7F7F7F;
    private static final int UNRESOLVED = 0xFF3B3B3B;

    private static void put(ResourceKey<Biome> key, int rgb) {
        COLORS.put(key, 0xFF000000 | rgb);
    }

    static {
        put(Biomes.OCEAN, 0x1E4A8C);
        put(Biomes.DEEP_OCEAN, 0x142F5C);
        put(Biomes.WARM_OCEAN, 0x2A7FB0);
        put(Biomes.LUKEWARM_OCEAN, 0x1E5F92);
        put(Biomes.COLD_OCEAN, 0x1A3D6E);
        put(Biomes.FROZEN_OCEAN, 0x6E90B8);
        put(Biomes.RIVER, 0x3E6FBF);
        put(Biomes.FROZEN_RIVER, 0x8FB6E0);
        put(Biomes.BEACH, 0xE0D18C);
        put(Biomes.SNOWY_BEACH, 0xE8ECEF);
        put(Biomes.PLAINS, 0x8DB35B);
        put(Biomes.SUNFLOWER_PLAINS, 0xB5D66C);
        put(Biomes.FOREST, 0x3E7A32);
        put(Biomes.FLOWER_FOREST, 0x5FA850);
        put(Biomes.BIRCH_FOREST, 0x6FA050);
        put(Biomes.DARK_FOREST, 0x2C4A22);
        put(Biomes.OLD_GROWTH_BIRCH_FOREST, 0x5A9048);
        put(Biomes.OLD_GROWTH_PINE_TAIGA, 0x2F5A44);
        put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, 0x2A5240);
        put(Biomes.TAIGA, 0x3C6B52);
        put(Biomes.SNOWY_TAIGA, 0x6F9482);
        put(Biomes.SAVANNA, 0xB6A254);
        put(Biomes.SAVANNA_PLATEAU, 0xA69248);
        put(Biomes.WINDSWEPT_HILLS, 0x7C7C6E);
        put(Biomes.WINDSWEPT_GRAVELLY_HILLS, 0x8C8C80);
        put(Biomes.WINDSWEPT_FOREST, 0x5C7C52);
        put(Biomes.WINDSWEPT_SAVANNA, 0xA88C4C);
        put(Biomes.JUNGLE, 0x2E8C3C);
        put(Biomes.SPARSE_JUNGLE, 0x4C9C4C);
        put(Biomes.BAMBOO_JUNGLE, 0x3E9C42);
        put(Biomes.DESERT, 0xD9C36A);
        put(Biomes.BADLANDS, 0xA1601F);
        put(Biomes.ERODED_BADLANDS, 0xB06A28);
        put(Biomes.WOODED_BADLANDS, 0x8C6428);
        put(Biomes.SWAMP, 0x4C6B4A);
        put(Biomes.MANGROVE_SWAMP, 0x3E6E52);
        put(Biomes.SNOWY_PLAINS, 0xE6EEF2);
        put(Biomes.ICE_SPIKES, 0xC8E4F0);
        put(Biomes.SNOWY_SLOPES, 0xD6E4EA);
        put(Biomes.GROVE, 0x5E7C6E);
        put(Biomes.MEADOW, 0x7FBF5E);
        put(Biomes.CHERRY_GROVE, 0xE8A6C0);
        put(Biomes.FROZEN_PEAKS, 0xC8D4DC);
        put(Biomes.JAGGED_PEAKS, 0xB8C4CE);
        put(Biomes.STONY_PEAKS, 0x8C8C84);
        put(Biomes.STONY_SHORE, 0x8C8C7A);
        put(Biomes.MUSHROOM_FIELDS, 0xA05A9C);
        put(Biomes.DRIPSTONE_CAVES, 0x6E5A3E);
        put(Biomes.LUSH_CAVES, 0x2E9C6A);
        put(Biomes.DEEP_DARK, 0x1A1A22);
        put(Biomes.THE_VOID, 0x000000);
        put(Biomes.NETHER_WASTES, 0x6E2C2C);
        put(Biomes.CRIMSON_FOREST, 0x8C1E1E);
        put(Biomes.WARPED_FOREST, 0x1E8C7C);
        put(Biomes.SOUL_SAND_VALLEY, 0x4A3C34);
        put(Biomes.BASALT_DELTAS, 0x5A5A62);
        put(Biomes.THE_END, 0xC8C0DE);
        put(Biomes.END_HIGHLANDS, 0xD8CCE8);
        put(Biomes.END_MIDLANDS, 0xC8B8DC);
        put(Biomes.SMALL_END_ISLANDS, 0xB8A8CC);
        put(Biomes.END_BARRENS, 0xA898BC);
    }

    private BiomeColors() {}

    public static int colorFor(Holder<Biome> biomeHolder) {
        if (biomeHolder == null) return UNRESOLVED;
        return biomeHolder.unwrapKey()
                .map(key -> COLORS.getOrDefault(key, FALLBACK))
                .orElse(FALLBACK);
    }

    public static int colorFor(Identifier biomeId) {
        for (Map.Entry<ResourceKey<Biome>, Integer> e : COLORS.entrySet()) {
            if (e.getKey().identifier().equals(biomeId)) return e.getValue();
        }
        return FALLBACK;
    }
}
