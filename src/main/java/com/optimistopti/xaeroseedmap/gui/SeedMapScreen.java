package com.optimistopti.xaeroseedmap.gui;

import com.optimistopti.xaeroseedmap.biome.BiomeColors;
import com.optimistopti.xaeroseedmap.gen.SeedBiomeSampler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fullscreen pannable/zoomable biome preview for an arbitrary seed, drawn as a
 * grid of filled rectangles (deliberately avoids DynamicTexture/NativeImage -
 * those APIs also changed in 26.1.2 and a flat color grid needs none of it).
 * Structures/dungeons are out of scope for v1 (see roadmap in README).
 */
public class SeedMapScreen extends Screen {

    private static final ScheduledExecutorService SAMPLER_POOL =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "xaeroseedmap-sampler");
                t.setDaemon(true);
                return t;
            });

    private static final int GRID_SIZE = 96;

    private final Screen parent;
    private final long seed;

    private SeedBiomeSampler sampler;
    private volatile int[] colorGrid;

    private double centerX = 0;
    private double centerZ = 0;
    /** Blocks represented by one grid cell. Lower = more zoomed in. */
    private double blocksPerCell = 16.0;

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
        CompletableFuture.runAsync(() -> this.sampler = SeedBiomeSampler.create(this.seed), SAMPLER_POOL)
                .thenRun(() -> this.dirty = true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (this.sampler == null) {
            graphics.centeredText(this.font,
                    Component.translatable("xaeroseedmap.map.generating"),
                    this.width / 2, this.height / 2, 0xFFAAAAAA);
            return;
        }

        maybeRegenerate();

        int[] grid = this.colorGrid;
        if (grid != null) {
            int size = Math.min(this.width, this.height) - 40;
            int cellPx = Math.max(1, size / GRID_SIZE);
            int originX = (this.width - cellPx * GRID_SIZE) / 2;
            int originY = (this.height - cellPx * GRID_SIZE) / 2;

            for (int gz = 0; gz < GRID_SIZE; gz++) {
                for (int gx = 0; gx < GRID_SIZE; gx++) {
                    int color = grid[gz * GRID_SIZE + gx];
                    int x = originX + gx * cellPx;
                    int y = originY + gz * cellPx;
                    graphics.fill(x, y, x + cellPx, y + cellPx, color);
                }
            }
        }

        graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        graphics.text(this.font,
                String.format("X: %.0f  Z: %.0f  (%.1f blocks/cell)", centerX, centerZ, blocksPerCell),
                8, this.height - 16, 0xFFAAAAAA, false);
    }

    private void maybeRegenerate() {
        if (!dirty || !regenerating.compareAndSet(false, true)) {
            return;
        }
        dirty = false;

        final double bpc = this.blocksPerCell;
        final double cx = this.centerX;
        final double cz = this.centerZ;
        final SeedBiomeSampler s = this.sampler;

        SAMPLER_POOL.execute(() -> {
            try {
                int[] grid = new int[GRID_SIZE * GRID_SIZE];
                double half = (GRID_SIZE / 2.0) * bpc;
                for (int gz = 0; gz < GRID_SIZE; gz++) {
                    int worldZ = (int) (cz - half + gz * bpc);
                    for (int gx = 0; gx < GRID_SIZE; gx++) {
                        int worldX = (int) (cx - half + gx * bpc);
                        Holder<Biome> biome = s.getBiome(worldX, worldZ);
                        grid[gz * GRID_SIZE + gx] = BiomeColors.colorFor(biome);
                    }
                }
                this.colorGrid = grid;
            } catch (Exception e) {
                // Sampler bootstrap not finished yet (see SeedBiomeSampler TODO) - leave grid as-is.
            } finally {
                regenerating.set(false);
            }
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            dragging = true;
            dragStartMouseX = event.x();
            dragStartMouseZ = event.y();
            dragStartCenterX = centerX;
            dragStartCenterZ = centerZ;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            dragging = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (dragging) {
            centerX = dragStartCenterX - (event.x() - dragStartMouseX) * blocksPerCell;
            centerZ = dragStartCenterZ - (event.y() - dragStartMouseZ) * blocksPerCell;
            dirty = true;
            return true;
        }
        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double factor = verticalAmount > 0 ? 0.8 : 1.25;
        blocksPerCell = Math.max(0.5, Math.min(256.0, blocksPerCell * factor));
        dirty = true;
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
