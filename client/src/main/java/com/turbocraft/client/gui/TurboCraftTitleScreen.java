package com.turbocraft.client.gui;

import com.turbocraft.client.TurboCraftClient;
import com.turbocraft.client.config.TurboCraftConfig;
import com.turbocraft.client.gui.widget.TurboButton;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.List;

/**
 * TurboCraft custom title screen — replaces vanilla's {@code TitleScreen} when enabled in config.
 *
 * <p>Visual approach is Android-first: no panorama (vanilla renders a 6-face cubemap every frame,
 * which is genuinely expensive on a phone), no blur, no textured buttons. Instead a vertical gradient
 * built from ~28 {@code fill} quads plus flat-colour buttons. That is a couple hundred vertices total
 * instead of a few thousand textured ones, and it still reads as a "client" title screen.
 */
public class TurboCraftTitleScreen extends Screen {
    private static final int ACCENT = 0xFF3DD6D0;
    private static final int PANEL = 0xB80A0F16;
    private static final int MUTED = 0xFF9FB3BC;

    /** Background gradient endpoints (deep navy → teal-black). */
    private static final int BG_TOP = 0xFF0A1420;
    private static final int BG_BOTTOM = 0xFF04161A;
    private static final int BAND = 0x143DD6D0;

    private static final String[] TAGLINES = {
            "HUD that actually tells the truth",
            "1% low > big FPS number",
            "Made for phones, not ported to them",
            "Right Shift opens the good stuff",
            "Sodium in, lag out",
    };

    private final String tagline = TAGLINES[(int) (Math.random() * TAGLINES.length)];

    public TurboCraftTitleScreen() {
        super(Text.literal("TurboCraft"));
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        int btnW = 220;
        int btnH = 22;
        int gap = 5;
        int x = this.width / 2 - btnW / 2;
        int y = this.height / 2 - 34;

        this.addDrawableChild(new TurboButton(x, y, btnW, btnH,
                Text.literal("Singleplayer"),
                () -> this.client.setScreen(new SelectWorldScreen(this))));
        y += btnH + gap;

        this.addDrawableChild(new TurboButton(x, y, btnW, btnH,
                Text.literal("Multiplayer"),
                () -> this.client.setScreen(new MultiplayerScreen(this))));
        y += btnH + gap;

        this.addDrawableChild(new TurboButton(x, y, btnW, btnH,
                Text.literal("TurboCraft Settings"),
                () -> this.client.setScreen(new ClientMenuScreen(this)), true));
        y += btnH + gap + 3;

        int halfW = btnW / 2 - 2;
        this.addDrawableChild(new TurboButton(x, y, halfW, btnH,
                Text.literal("Options"),
                () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new TurboButton(x + btnW - halfW, y, halfW, btnH,
                Text.literal("Discord"),
                this::openDiscord));
        y += btnH + gap + 3;

        this.addDrawableChild(new TurboButton(x, y, btnW, btnH,
                Text.literal("Quit Game"),
                () -> this.client.scheduleStop()));
    }

    private void openDiscord() {
        String url = TurboCraftConfig.get().discordUrl;
        if (url == null || url.isBlank()) return;
        try {
            Util.getOperatingSystem().open(URI.create(url));
        } catch (Exception e) {
            TurboCraftClient.LOGGER.warn("[TurboCraft] Could not open Discord link: {}", url, e);
        }
    }

    // ------------------------------------------------------------------ rendering

    /** We paint our own background, so vanilla's dirt/blur/panorama is disabled. */
    @Override
    public void renderBackground(DrawContext context) {
        // intentionally empty — see render()
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // intentionally empty — see render()
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        drawGradient(context);
        drawPanel(context);
        super.render(context, mouseX, mouseY, delta); // widgets
        drawBranding(context);
        drawFooter(context);
    }

    /** Vertical gradient: 28 strips. Cheap enough for a 30 FPS phone GPU. */
    private void drawGradient(DrawContext context) {
        int strips = 28;
        for (int i = 0; i < strips; i++) {
            int y0 = this.height * i / strips;
            int y1 = this.height * (i + 1) / strips;
            context.fill(0, y0, this.width, y1, lerpColor(BG_TOP, BG_BOTTOM, i / (float) (strips - 1)));
        }

        // slow drifting highlight band — 2 quads, gives the screen some life without GPU cost
        long time = System.currentTimeMillis();
        float phase = (time % 12000L) / 12000f;
        int bandY = (int) (this.height * (phase * 1.4f - 0.2f));
        int bandH = Math.max(40, this.height / 6);
        context.fill(0, bandY, this.width, bandY + bandH, BAND);
        context.fill(0, bandY + bandH, this.width, bandY + bandH + 20, 0x0C3DD6D0);
    }

    /** Card behind the buttons. */
    private void drawPanel(DrawContext context) {
        int panelW = 248;
        int panelH = 232;
        int x = this.width / 2 - panelW / 2;
        int y = this.height / 2 - 84;

        context.fill(x, y, x + panelW, y + panelH, PANEL);
        context.fill(x, y, x + panelW, y + 1, 0x803DD6D0);
        context.fill(x, y + panelH - 1, x + panelW, y + panelH, 0x303DD6D0);
        context.fill(x, y, x + 1, y + panelH, 0x303DD6D0);
        context.fill(x + panelW - 1, y, x + panelW, y + panelH, 0x303DD6D0);
    }

    /** Logo, tagline and version line above the panel. */
    private void drawBranding(DrawContext context) {
        int centerX = this.width / 2;
        int logoY = this.height / 2 - 150;

        context.getMatrices().push();
        context.getMatrices().translate(centerX, logoY, 0);
        context.getMatrices().scale(2.0f, 2.0f, 1.0f);

        // soft glow (two offset copies at low alpha), then the real text
        String logo = "TURBOCRAFT";
        int halfWidth = this.textRenderer.getWidth(logo) / 2;
        context.drawText(this.textRenderer, logo, -halfWidth + 1, 1, 0x403DD6D0, false);
        context.drawText(this.textRenderer, logo, -halfWidth - 1, -1, 0x403DD6D0, false);
        context.drawText(this.textRenderer, logo, -halfWidth, 0, ACCENT, true);
        context.getMatrices().pop();

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.tagline),
                centerX, logoY + 26, MUTED);
    }

    private void drawFooter(DrawContext context) {
        String clientVersion = FabricLoader.getInstance().getModContainer(TurboCraftClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
        String mcVersion = this.client != null ? this.client.getGameVersion() : "?";

        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Minecraft " + mcVersion + " · Fabric"), 4, this.height - 22, MUTED);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("TurboCraft Client " + clientVersion), 4, this.height - 12, ACCENT);

        Text disclaimer = Text.literal("Not affiliated with Mojang or Microsoft");
        context.drawTextWithShadow(this.textRenderer, disclaimer,
                this.width - this.textRenderer.getWidth(disclaimer) - 4, this.height - 12, 0xFF607078);
    }

    // ------------------------------------------------------------------ behaviour

    /** {@code DrawContext} takes ARGB ints here; alpha is kept fully opaque. */
    private static int lerpColor(int from, int to, float t) {
        int a = (int) (((from >>> 24) & 0xFF) * (1 - t) + ((to >>> 24) & 0xFF) * t);
        int r = (int) (((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = (int) (((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int) ((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void close() {
        // Esc on the title screen must not close it (there is nothing behind it)
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static List<String> taglines() {
        return List.of(TAGLINES);
    }
}
