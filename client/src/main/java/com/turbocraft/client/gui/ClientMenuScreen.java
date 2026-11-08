package com.turbocraft.client.gui;

import com.turbocraft.client.config.TurboCraftConfig;
import com.turbocraft.client.core.Links;
import com.turbocraft.client.gui.widget.TurboButton;
import com.turbocraft.client.gui.widget.TurboToggle;
import com.turbocraft.client.hud.HudRenderer;
import com.turbocraft.client.perf.ParticleBudget;
import com.turbocraft.client.perf.PerformanceProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;

/**
 * TurboCraft in-game menu — FastClient-style: left category rail, right settings pane.
 *
 * <p>Right Shift opens it in-game, and it is also reachable from the title screen. Layout is
 * width-driven: on narrow phone screens the rail shrinks and the pane takes the remaining space,
 * rather than the two overlapping.
 */
public class ClientMenuScreen extends Screen {
    private enum Tab {
        HUD("HUD", "FPS, CPS, keystrokes, coords"),
        PERF("PERF", "One-click FPS profiles"),
        EDITOR("EDITOR", "Drag & scale the HUD"),
        COSMETICS("COSMETICS", "Capes, trails, accents"),
        LINKS("LINKS", "Discord, store, source"),
        ABOUT("ABOUT", "Version and credits");

        final String label;
        final String subtitle;

        Tab(String label, String subtitle) {
            this.label = label;
            this.subtitle = subtitle;
        }
    }

    private static final int ROW_H = 22;
    private static final int GAP = 3;

    private final Screen parent;
    private Tab tab = Tab.HUD;

    private int railX;
    private int railY;
    private int railW;
    private int railH;
    private int paneX;
    private int paneY;
    private int paneW;
    private int paneH;

    public ClientMenuScreen(Screen parent) {
        super(Text.literal("TurboCraft"));
        this.parent = parent;
    }

    private static TurboCraftConfig config() {
        return TurboCraftConfig.get();
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        HudRenderer.suppressed = false;

        boolean narrow = this.width < 460;
        railW = narrow ? 74 : 104;
        railX = Math.max(6, this.width / 2 - 170);
        railY = Math.max(34, this.height / 2 - 96);
        railH = Math.min(this.height - railY - 26, 200);

        paneX = railX + railW + 8;
        paneW = Math.min(narrow ? this.width - paneX - 8 : 232, this.width - paneX - 8);
        paneY = railY;
        paneH = railH;

        buildTabs();
        buildPane();
    }

    private void buildTabs() {
        int y = railY;
        for (Tab t : Tab.values()) {
            Tab target = t;
            this.addDrawableChild(new TurboButton(railX, y, railW, 18, Text.literal(t.label),
                    () -> {
                        this.tab = target;
                        this.clearAndInit();
                    },
                    t == this.tab ? TurboButton.Style.SELECTED : TurboButton.Style.NORMAL));
            y += 18 + GAP;
        }
    }

    private void buildPane() {
        TurboCraftConfig c = config();
        int rowW = Math.max(60, paneW);
        int y = paneY;
        int rowH = ROW_H - 3;

        switch (this.tab) {
            case HUD -> {
                y = toggle("Master HUD", null, c.hudEnabled, v -> c.hudEnabled = v, paneX, y, rowW, rowH);
                y = toggle("FPS counter", null, c.showFps, v -> c.showFps = v, paneX, y, rowW, rowH);
                y = toggle("Frame graph + 1% low", null, c.showGraph, v -> c.showGraph = v,
                        paneX, y, rowW, rowH);
                y = toggle("CPS counter", null, c.showCps, v -> c.showCps = v, paneX, y, rowW, rowH);
                y = toggle("Coordinates", null, c.showCoords, v -> c.showCoords = v, paneX, y, rowW, rowH);
                y = toggle("Keystrokes", null, c.showKeystrokes, v -> c.showKeystrokes = v,
                        paneX, y, rowW, rowH);
                y = toggle("Ping", null, c.showPing, v -> c.showPing = v, paneX, y, rowW, rowH);
                y = toggle("HUD background", null, c.hudBackground, v -> c.hudBackground = v,
                        paneX, y, rowW, rowH);
            }
            case PERF -> {
                int half = rowW / 2 - 2;
                this.addDrawableChild(new TurboButton(paneX, y, rowW, 20,
                        Text.literal("MAX FPS  (potato mode)"),
                        () -> PerformanceProfile.applyPotato(this.client),
                        TurboButton.Style.ACCENT));
                y += 24;
                this.addDrawableChild(new TurboButton(paneX, y, half, 18,
                        Text.literal("Balanced"),
                        () -> PerformanceProfile.applyBalanced(this.client)));
                this.addDrawableChild(new TurboButton(paneX + rowW - half, y, half, 18,
                        Text.literal("Vanilla"), () -> PerformanceProfile.applyVanilla(this.client)));
                y += 24;

                this.addDrawableChild(new TurboButton(paneX, y, rowW, 16,
                        Text.literal("Frame time: " + (config().showFrameTime ? "ON" : "OFF")),
                        () -> {
                            TurboCraftConfig cc = config();
                            cc.showFrameTime = !cc.showFrameTime;
                            TurboCraftConfig.save();
                            this.clearAndInit();
                        }));
                y += 20;

                // ---- particle budget ----
                int budget = config().particleBudget;
                String budgetLabel = budget <= 0 ? "unlimited" : (budget * 20) + "/s";
                this.addDrawableChild(new TurboButton(paneX, y, rowW, 16,
                        Text.literal("Particle cap: " + budgetLabel),
                        () -> {
                            TurboCraftConfig cc = config();
                            cc.particleBudget = nextBudget(cc.particleBudget);
                            cc.showParticleBudget = cc.particleBudget > 0;
                            TurboCraftConfig.save();
                            this.clearAndInit();
                        },
                        budget > 0 ? TurboButton.Style.SELECTED : TurboButton.Style.NORMAL));
                y += 20;

                this.addDrawableChild(new TurboButton(paneX, y, rowW, 14,
                        Text.literal("Live: " + ParticleBudget.perSecond() + "/s"
                                + (ParticleBudget.skippedPerSecond() > 0
                                    ? "   skipping " + ParticleBudget.skippedPerSecond() : "")),
                        () -> this.clearAndInit()));
            }
            case EDITOR -> {
                int half = rowW / 2 - 2;
                this.addDrawableChild(new TurboButton(paneX, y, rowW, 20,
                        Text.literal("Open HUD editor"),
                        () -> this.client.setScreen(new HudEditorScreen(this)),
                        TurboButton.Style.ACCENT));
                y += 24;

                this.addDrawableChild(new TurboButton(paneX, y, half, 18, Text.literal("Scale -"),
                        () -> changeScale(-0.1f)));
                this.addDrawableChild(new TurboButton(paneX + rowW - half, y, half, 18,
                        Text.literal("Scale +"), () -> changeScale(0.1f)));
                y += 22;

                this.addDrawableChild(new TurboButton(paneX, y, rowW, 18, Text.literal("Reset position"),
                        () -> {
                            TurboCraftConfig cc = config();
                            cc.hudX = 4;
                            cc.hudY = 4;
                            cc.hudScale = 1.0f;
                            TurboCraftConfig.save();
                        }));
            }
            case COSMETICS -> {
                this.addDrawableChild(new TurboButton(paneX, y, rowW, 18,
                        Text.literal("Accent: " + Theme.active().name()),
                        () -> {
                            cycleAccent();
                            this.clearAndInit();
                        },
                        TurboButton.Style.SELECTED));
                y += 22;

                for (Theme.Palette p : Theme.PALETTES) {
                    int color = p.accent();
                    String name = p.name();
                    this.addDrawableChild(new TurboButton(paneX, y, rowW, 16,
                            Text.literal("  " + name),
                            () -> {
                                Theme.set(p);
                                config().accent = p.id();
                                TurboCraftConfig.save();
                                this.clearAndInit();
                            },
                            Theme.active() == p ? TurboButton.Style.SELECTED : TurboButton.Style.NORMAL));
                    // swatch strip drawn by the button icon slot is not colour-capable; draw in render
                    y += 18;
                    if (y > paneY + paneH - 30) break;
                }

                y = paneY + paneH - 26;
                this.addDrawableChild(new TurboButton(paneX, y, rowW, 18,
                        Text.literal("Get capes & trails"),
                        () -> open(Links.pick(config().storeUrl, Links.STORE)),
                        TurboButton.Style.ACCENT));
            }
            case LINKS -> {
                this.addDrawableChild(new TurboButton(paneX, y, rowW, 20,
                        Text.literal("Join Discord"),
                        () -> open(Links.pick(config().discordUrl, Links.DISCORD)),
                        TurboButton.Style.ACCENT));
                y += 24;
                this.addDrawableChild(new TurboButton(paneX, y, rowW, 18, Text.literal("Store"),
                        () -> open(Links.pick(config().storeUrl, Links.STORE))));
                y += 22;
                this.addDrawableChild(new TurboButton(paneX, y, rowW, 18, Text.literal("GitHub source"),
                        () -> open(Links.GITHUB)));
            }
            case ABOUT -> {
                // text only — drawn in render()
            }
        }
    }

    private int toggle(String label, String desc, boolean initial,
                       java.util.function.Consumer<Boolean> setter,
                       int x, int y, int w, int h) {
        this.addDrawableChild(new TurboToggle(x, y, w, h, label, desc, initial, v -> {
            setter.accept(v);
            TurboCraftConfig.save();
        }));
        return y + h + GAP;
    }

    /** Cycles through sane per-tick budgets: unlimited -> 150/s -> 300/s -> 600/s -> 1200/s. */
    private static int nextBudget(int current) {
        int[] values = {0, 8, 16, 32, 64};
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[(i + 1) % values.length];
        }
        return 16;
    }

    private void changeScale(float delta) {
        TurboCraftConfig c = config();
        float next = Math.round((c.hudScale + delta) * 10f) / 10f;
        c.hudScale = Math.max(0.5f, Math.min(2.0f, next));
        TurboCraftConfig.save();
    }

    private void cycleAccent() {
        Theme.Palette[] all = Theme.PALETTES;
        int next = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i] == Theme.active()) {
                next = (i + 1) % all.length;
                break;
            }
        }
        Theme.set(all[next]);
        config().accent = all[next].id();
        TurboCraftConfig.save();
    }

    private void open(String url) {
        if (!Links.isSet(url)) return;
        try {
            Util.getOperatingSystem().open(URI.create(url));
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // rail + pane panels
        context.fill(railX - 6, railY - 22, railX + railW + 6, railY + railH + 6, Theme.BACKDROP);
        context.fill(paneX - 6, paneY - 22, paneX + paneW + 6, paneY + paneH + 6, Theme.PANEL);
        context.fill(paneX - 6, paneY - 22, paneX + paneW + 6, paneY - 21, Theme.accent());

        // branding
        context.drawTextWithShadow(this.textRenderer, Text.literal("TURBOCRAFT"),
                railX - 4, railY - 17, Theme.accent());
        context.drawTextWithShadow(this.textRenderer, Text.literal(this.tab.subtitle),
                paneX, paneY - 17, Theme.TEXT_DIM);

        if (this.tab == Tab.ABOUT) {
            drawAbout(context);
        }
        if (this.tab == Tab.COSMETICS) {
            drawSwatches(context);
        }
        if (this.tab == Tab.PERF) {
            drawPerfInfo(context);
        }

        super.render(context, mouseX, mouseY, delta);

        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Right Shift to close"), railX - 4, railY + railH + 10, Theme.TEXT_FAINT);
    }

    private void drawAbout(DrawContext context) {
        int x = paneX;
        int y = paneY;
        String[] lines = {
                "TurboCraft Client",
                "Minecraft 1.20.1 · Fabric",
                "",
                "Client-side only — safe on",
                "vanilla servers.",
                "",
                "Not affiliated with Mojang",
                "or Microsoft.",
        };
        for (String line : lines) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(line), x, y,
                    line.startsWith("TurboCraft") ? Theme.accent() : Theme.TEXT_DIM);
            y += 10;
            if (y > paneY + paneH - 10) break;
        }
    }

    /** Live memory + launcher advice. Reads the JVM, so it reports what the launcher actually set. */
    private void drawPerfInfo(DrawContext context) {
        Runtime rt = Runtime.getRuntime();
        long maxMb = rt.maxMemory() / (1024L * 1024L);
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);

        int x = paneX;
        int y = paneY + 74;
        // JVM heap only. Launchers often show *device* RAM in their overlay, which is ~7 GB on a
        // phone and has nothing to do with the -Xmx value; reading Runtime here avoids that mix-up.
        String[] lines = PerformanceProfile.launcherAdvice(maxMb, usedMb, 0);
        for (String line : lines) {
            int color = line.startsWith("->") ? Theme.accent() : Theme.TEXT_DIM;
            context.drawTextWithShadow(this.textRenderer, Text.literal(line), x, y, color);
            y += 9;
            if (y > paneY + paneH - 10) break;
        }
    }

    /** Colour chips next to the theme rows. */
    private void drawSwatches(DrawContext context) {
        int y = paneY + 22 + 2;
        for (Theme.Palette p : Theme.PALETTES) {
            context.fill(paneX + 5, y + 5, paneX + 7, y + 11, p.accent());
            y += 18;
            if (y > paneY + paneH - 30) break;
        }
    }

    // ------------------------------------------------------------------ behaviour

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
