package com.optimistopti.xaeroseedmap.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.optimistopti.xaeroseedmap.biome.BiomeColors;
import com.optimistopti.xaeroseedmap.config.SeedMapConfig;
import com.optimistopti.xaeroseedmap.gen.SeedBiomeSampler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fullscreen pannable/zoomable biome preview for an arbitrary seed.
 * Structures/dungeons are intentionally out of scope for v1 (see roadmap in README).
 */
public class SeedMapScreen extends Screen {

    private static final ScheduledExecutorService SAMPLER_POOL =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "xaeroseedmap-sampler");
                t.setDaemon(true);
                return t;
            });

    private static final int TEXTURE_SIZE = 512;

    private final Screen parent;
    private final long seed;

    private SeedBiomeSampler sampler;
    private DynamicTexture texture;
    private ResourceLocation textureId;

    private double centerX = 0;
    private double centerZ = 0;
    /** Blocks represented by one texture pixel. Lower = more zoomed in. */
    private double blocksPerPixel = 8.0;

    private boolean dragging = false;
    private double dragStartMouseX, dragStartMouseZ;
    private double dragStartCenterX, dragStartCenterZ;

    private final AtomicBoolean regenerating = new AtomicBoolean(false);
    private volatile boolean dirty = true;

    public SeedMapScreen(Screen parent, long seed) {
        super(Component.translatable("xaeroseedmap.map.title", Long.toString(seed)));
        this.parent = parent;
        this.seed = seed;
    }

    @Override
    protected void init() {
        this.texture = new DynamicTexture(TEXTURE_SIZE, TEXTURE_SIZE, false);
        this.textureId = this.minecraft.getTextureManager()
                .register("xaeroseedmap/preview", this.texture);

        CompletableFuture.runAsync(() -> this.sampler = SeedBiomeSampler.create(this.seed), SAMPLER_POOL)
                .thenRun(() -> this.dirty = true);
    }

    @Override
    public void removed() {
        if (this.texture != null) {
            this.texture.close();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        if (this.sampler == null) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("xaeroseedmap.map.generating"),
                    this.width / 2, this.height / 2, 0xAAAAAA);
            return;
        }

        maybeRegenerate();

        int size = Math.min(this.width, this.height) - 40;
        int x = (this.width - size) / 2;
        int y = (this.height - size) / 2;
        graphics.blit(this.textureId, x, y, size, size, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        graphics.drawString(this.font,
                String.format("X: %.0f  Z: %.0f  (%.1f blocks/px)", centerX, centerZ, blocksPerPixel),
                8, this.height - 16, 0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void maybeRegenerate() {
        if (!dirty || !regenerating.compareAndSet(false, true)) {
            return;
        }
        dirty = false;

        final double bpp = this.blocksPerPixel;
        final double cx = this.centerX;
        final double cz = this.centerZ;
        final SeedBiomeSampler s = this.sampler;

        SAMPLER_POOL.execute(() -> {
            try {
                NativeImage image = this.texture.getPixels();
                if (image == null) return;

                double half = (TEXTURE_SIZE / 2.0) * bpp;
                for (int px = 0; px < TEXTURE_SIZE; px++) {
                    int worldX = (int) (cx - half + px * bpp);
                    for (int pz = 0; pz < TEXTURE_SIZE; pz++) {
                        int worldZ = (int) (cz - half + pz * bpp);
                        Holder<Biome> biome = s.getBiome(worldX, worldZ);
                        int color = BiomeColors.colorFor(biome);
                        image.setPixelRGBA(px, pz, color);
                    }
                }
                RenderSystem.recordRenderCall(this.texture::upload);
            } finally {
                regenerating.set(false);
            }
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = true;
            dragStartMouseX = mouseX;
            dragStartMouseZ = mouseY;
            dragStartCenterX = centerX;
            dragStartCenterZ = centerZ;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            centerX = dragStartCenterX - (mouseX - dragStartMouseX) * blocksPerPixel;
            centerZ = dragStartCenterZ - (mouseY - dragStartMouseZ) * blocksPerPixel;
            dirty = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double factor = scrollY > 0 ? 0.8 : 1.25;
        blocksPerPixel = Math.max(0.5, Math.min(256.0, blocksPerPixel * factor));
        dirty = true;
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
