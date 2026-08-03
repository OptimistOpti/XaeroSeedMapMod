package com.optimistopti.xaeroseedmap.gui;

import com.optimistopti.xaeroseedmap.config.SeedMapConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SeedMapSettingsScreen extends Screen {

    private final Screen parent;
    private EditBox seedField;

    public SeedMapSettingsScreen(Screen parent) {
        super(Component.translatable("xaeroseedmap.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldWidth = 220;

        this.seedField = new EditBox(this.font, centerX - fieldWidth / 2, 60, fieldWidth, 20,
                null, Component.translatable("xaeroseedmap.settings.seed"));
        this.seedField.setMaxLength(256);
        this.seedField.setValue(SeedMapConfig.INSTANCE.rawSeed);
        this.seedField.setHint(Component.translatable("xaeroseedmap.settings.seed.placeholder"));
        this.addRenderableWidget(this.seedField);
        this.setInitialFocus(this.seedField);

        this.addRenderableWidget(Button.builder(Component.translatable("xaeroseedmap.settings.open_map"), b -> {
                    saveSeed();
                    if (SeedMapConfig.INSTANCE.hasSeed()) {
                        this.minecraft.setScreen(new SeedMapScreen(this, SeedMapConfig.INSTANCE.parsedSeed()));
                    }
                })
                .bounds(centerX - fieldWidth / 2, 95, fieldWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("xaeroseedmap.settings.save"), b -> {
                    saveSeed();
                    this.minecraft.setScreen(this.parent);
                })
                .bounds(centerX - fieldWidth / 2, 125, fieldWidth, 20)
                .build());
    }

    private void saveSeed() {
        SeedMapConfig.INSTANCE.rawSeed = this.seedField.getValue();
        SeedMapConfig.INSTANCE.save();
    }

    @Override
    public void onClose() {
        saveSeed();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 25, 0xFFFFFFFF);
    }
}
