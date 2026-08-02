package com.optimistopti.xaeroseedmap;

import com.mojang.blaze3d.platform.InputConstants;
import com.optimistopti.xaeroseedmap.config.SeedMapConfig;
import com.optimistopti.xaeroseedmap.gui.SeedMapScreen;
import com.optimistopti.xaeroseedmap.gui.SeedMapSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

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

        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.xaeroseedmap.open_settings",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_UNKNOWN.getValue(),
                "key.category.xaeroseedmap"
        ));

        openSeedMapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.xaeroseedmap.open_seedmap",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_UNKNOWN.getValue(),
                "key.category.xaeroseedmap"
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
        client.setScreen(new SeedMapSettingsScreen(current));
    }

    public static void openSeedMap(Minecraft client) {
        if (!SeedMapConfig.INSTANCE.hasSeed()) {
            openSettings(client);
            return;
        }
        Screen current = client.screen;
        client.setScreen(new SeedMapScreen(current, SeedMapConfig.INSTANCE.parsedSeed()));
    }
}
