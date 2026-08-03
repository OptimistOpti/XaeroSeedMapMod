package com.optimistopti.xaeroseedmap;

import com.mojang.blaze3d.platform.InputConstants;
import com.optimistopti.xaeroseedmap.config.SeedMapConfig;
import com.optimistopti.xaeroseedmap.gui.SeedMapScreen;
import com.optimistopti.xaeroseedmap.gui.SeedMapSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XaeroSeedMapClient implements ClientModInitializer {

    public static final String MOD_ID = "xaeroseedmap";
    public static final Logger LOGGER = LoggerFactory.getLogger("Xaero SeedMap");

    private static KeyMapping openSettingsKey;
    private static KeyMapping openSeedMapKey;

    @Override
    public void onInitializeClient() {
        SeedMapConfig.INSTANCE = SeedMapConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        openSettingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.xaeroseedmap.open_settings",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        openSeedMapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.xaeroseedmap.open_seedmap",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettingsKey.consumeClick()) {
                openSettings(client);
            }
            while (openSeedMapKey.consumeClick()) {
                openSeedMap(client);
            }
        });

        LOGGER.info("Xaero SeedMap initialised");
    }

    public static void openSettings(Minecraft client) {
        Screen current = client.screen;
        client.gui.setScreen(new SeedMapSettingsScreen(current));
    }

    public static void openSeedMap(Minecraft client) {
        if (!SeedMapConfig.INSTANCE.hasSeed()) {
            openSettings(client);
            return;
        }
        Screen current = client.screen;
        client.gui.setScreen(new SeedMapScreen(current, SeedMapConfig.INSTANCE.parsedSeed()));
    }
}
