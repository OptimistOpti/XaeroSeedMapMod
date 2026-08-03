package com.optimistopti.xaeroseedmap.biome;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.HashMap;
import java.util.Map;

/**
 * ARGB lookup table of colors per vanilla biome, in the spirit of
 * Amidst/Chunkbase seed maps. Modded/datapack biomes fall back to a neutral
 * grey. Values adapted from a real, actively-maintained sibling project
 * (github.com/TGGamesYT/xaero-seedmap, cubiomes-based, older MC/Yarn) which
 * itself mirrors vanilla's own map coloring - accurate real-world colors,
 * just re-keyed here onto 26.1.x's current biome id set and darkened the
 * same way vanilla's map renderer subtly shades most land biomes.
 */
public final class BiomeColors {

    private static final Map<ResourceKey<Biome>, Integer> COLORS = new HashMap<>();
    private static final int FALLBACK = 0xFF7F7F7F;
    private static final int UNRESOLVED = 0xFF3B3B3B;

    private static void put(ResourceKey<Biome> key, int rgb) {
        COLORS.put(key, 0xFF000000 | rgb);
    }

    private static void putDark(ResourceKey<Biome> key, int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        put(key, (r << 16) | (g << 8) | b);
    }

    static {
        // Oceans
        putDark(Biomes.OCEAN, 0x1F65A6, 0.75f);
        putDark(Biomes.FROZEN_OCEAN, 0x1A5C9E, 0.70f);
        putDark(Biomes.DEEP_OCEAN, 0x1F65A6, 0.62f);
        putDark(Biomes.WARM_OCEAN, 0x2E6EA4, 0.85f);
        putDark(Biomes.LUKEWARM_OCEAN, 0x2561A4, 0.80f);
        putDark(Biomes.COLD_OCEAN, 0x225CB2, 0.80f);
        putDark(Biomes.DEEP_LUKEWARM_OCEAN, 0x2561A4, 0.63f);
        putDark(Biomes.DEEP_COLD_OCEAN, 0x225CB2, 0.63f);
        putDark(Biomes.DEEP_FROZEN_OCEAN, 0x1F65A6, 0.58f);

        // Rivers
        putDark(Biomes.RIVER, 0x3F76E4, 0.85f);
        putDark(Biomes.FROZEN_RIVER, 0x3F76E4, 0.80f);

        // Plains / grassland
        putDark(Biomes.PLAINS, 0x91BD59, 0.80f);
        putDark(Biomes.SUNFLOWER_PLAINS, 0xA5C16C, 0.80f);

        // Desert
        putDark(Biomes.DESERT, 0xBDB25F, 0.80f);

        // Mountains
        putDark(Biomes.WINDSWEPT_HILLS, 0x606B3E, 0.80f);
        putDark(Biomes.WINDSWEPT_GRAVELLY_HILLS, 0x4E5C3E, 0.80f);
        putDark(Biomes.WINDSWEPT_FOREST, 0x5C7C52, 0.80f);
        putDark(Biomes.WINDSWEPT_SAVANNA, 0xE5E04A, 0.80f);

        // Forest
        putDark(Biomes.FOREST, 0x507A32, 0.80f);
        putDark(Biomes.FLOWER_FOREST, 0x6DAA3B, 0.80f);
        putDark(Biomes.BIRCH_FOREST, 0x88BB67, 0.80f);
        putDark(Biomes.OLD_GROWTH_BIRCH_FOREST, 0x9DCE72, 0.80f);
        putDark(Biomes.DARK_FOREST, 0x29571B, 0.80f);
        putDark(Biomes.PALE_GARDEN, 0xD0D0C0, 1.0f);

        // Taiga
        putDark(Biomes.TAIGA, 0x31554A, 0.80f);
        putDark(Biomes.SNOWY_TAIGA, 0x31554A, 0.85f);
        putDark(Biomes.OLD_GROWTH_PINE_TAIGA, 0x596651, 0.80f);
        putDark(Biomes.OLD_GROWTH_SPRUCE_TAIGA, 0x6E7F65, 0.80f);

        // Snowy
        put(Biomes.SNOWY_PLAINS, 0xEEEEEE);
        put(Biomes.ICE_SPIKES, 0xCCCCCC);

        // Swamp
        putDark(Biomes.SWAMP, 0x4C763C, 0.80f);
        putDark(Biomes.MANGROVE_SWAMP, 0x567845, 0.80f);

        // Jungle
        putDark(Biomes.JUNGLE, 0x59C93C, 0.80f);
        putDark(Biomes.SPARSE_JUNGLE, 0x64C73F, 0.80f);
        putDark(Biomes.BAMBOO_JUNGLE, 0x47E01A, 0.80f);

        // Savanna
        putDark(Biomes.SAVANNA, 0xBFB755, 0.80f);
        putDark(Biomes.SAVANNA_PLATEAU, 0xC4C15A, 0.80f);

        // Badlands
        put(Biomes.BADLANDS, 0xC77044);
        put(Biomes.WOODED_BADLANDS, 0xB86B40);
        put(Biomes.ERODED_BADLANDS, 0xC44A3A);

        // Beach / shore
        put(Biomes.BEACH, 0xC9C98C);
        put(Biomes.SNOWY_BEACH, 0xC9C98C);
        put(Biomes.STONY_SHORE, 0x9A9A97);

        // Mushroom
        putDark(Biomes.MUSHROOM_FIELDS, 0x2C4205, 0.80f);

        // Nether
        put(Biomes.NETHER_WASTES, 0x6B2B2B);
        put(Biomes.SOUL_SAND_VALLEY, 0x4A6A8A);
        put(Biomes.CRIMSON_FOREST, 0xCC2200);
        put(Biomes.WARPED_FOREST, 0x1A8C8C);
        put(Biomes.BASALT_DELTAS, 0x505050);

        // The End
        put(Biomes.THE_END, 0xC8C4A0);
        put(Biomes.SMALL_END_ISLANDS, 0x0D0D1A);
        put(Biomes.END_MIDLANDS, 0xB8B490);
        put(Biomes.END_HIGHLANDS, 0xD6D2B0);
        put(Biomes.END_BARRENS, 0x9A9676);

        // Caves
        put(Biomes.DRIPSTONE_CAVES, 0x595959);
        putDark(Biomes.LUSH_CAVES, 0x4CBF00, 0.80f);
        put(Biomes.DEEP_DARK, 0x0D0D1D);

        // 1.18+ mountain highlands
        putDark(Biomes.MEADOW, 0x8DB360, 0.80f);
        putDark(Biomes.GROVE, 0x96AF79, 0.80f);
        put(Biomes.SNOWY_SLOPES, 0xDDDDDD);
        put(Biomes.JAGGED_PEAKS, 0xEEEEEE);
        put(Biomes.FROZEN_PEAKS, 0xE8F0FF);
        put(Biomes.STONY_PEAKS, 0x9A9A9A);

        // 1.20+
        putDark(Biomes.CHERRY_GROVE, 0xE8A0B0, 0.85f);

        // Misc
        put(Biomes.THE_VOID, 0x000000);
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
