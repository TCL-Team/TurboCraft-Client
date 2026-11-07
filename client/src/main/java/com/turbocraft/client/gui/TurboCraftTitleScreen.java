package com.turbocraft.client.gui;

import com.turbocraft.client.TurboCraftClient;
import com.turbocraft.client.config.TurboCraftConfig;
import com.turbocraft.client.gui.widget.TurboButton;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;

/**
 * TurboCraft custom title screen — replaces vanilla's {@code TitleScreen} when enabled in config.
 *
 * <p>Visual approach is Android-first: no panorama (vanilla renders a 6-face cubemap every frame,
 * which is genuinely expensive on a phone), no blur, no textured buttons. Instead a vertical gradient
 * built from ~28 {@code fill} quads plus flat-colour buttons.
 *
 * <p><b>Layout is fully responsive.</b> Gui scale on a phone can leave as little as ~250 logical
 * pixels of height, so every vertical size is derived from {@link #height} and the whole stack
 * shrinks (and the logo drops to 1x) when space runs out. Fixed offsets is exactly how the first
 * version ended up drawing the logo off-screen.
 */
public class TurboCraftTitleScreen extends Screen {
    private static final int ACCENT = 0xFF3DD6D0;
    private static final int PANEL = 0xB80A0F16;
    private static final int MUTED = 0xFF9FB3BC;
    private static final int FAINT = 0xFF607078;

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

    // ---- layout, computed in init() ----
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int btnH;
    private int gap;
    private int logoScale;

    public TurboCraftTitleScreen() {
        super(Text.literal("TurboCraft"));
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        int rows = 5;
        btnH = 20;
        gap = 4;

        // shrink vertically if the logical resolution is small (common on phones with Gui scale 3+)
        int blockH = rows * btnH + (rows - 1) * gap;
        int brandSpace = 44;
        if (blockH + brandSpace + 40 > this.height) {
            btnH = 16;
            gap = 3;
            blockH = rows * btnH + (rows - 1) * gap;
        }
        if (blockH + brandSpace + 40 > this.height) {
            brandSpace = 24;
        }

        // 2x logo whenever at least ~46 logical px fit above the panel, else 1x
        panelW = Math.min(232, this.width - 24);
        int padding = 12;
        panelH = blockH + padding * 2;
        panelX = this.width / 2 - panelW / 2;
        panelY = Math.max(brandSpace + 4, (this.height - panelH) / 2 + 6);

        // 2x logo whenever at least ~46 logical px fit above the panel, else 1x
        logoScale = (panelY - 6) >= 46 ? 2 : 1;

        int x = this.width / 2 - (panelW - padding * 2) / 2;
        int innerW = panelW - padding * 2;
        int y = panelY + padding;

        this.addDrawableChild(new TurboButton(x, y, innerW, btnH,
                Text.literal("Singleplayer"),
                () -> this.client.setScreen(new SelectWorldScreen(this))));
        y += btnH + gap;

        this.addDrawableChild(new TurboButton(x, y, innerW, btnH,
                Text.literal("Multiplayer"),
                () -> this.client.setScreen(new MultiplayerScreen(this))));
        y += btnH + gap;

        this.addDrawableChild(new TurboButton(x, y, innerW, btnH,
                Text.literal("TurboCraft Settings"),
                () -> this.client.setScreen(new ClientMenuScreen(this)), true));
        y += btnH + gap + 2;

        int halfW = innerW / 2 - 2;
        this.addDrawableChild(new TurboButton(x, y, halfW, btnH,
                Text.literal("Options"),
                () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new TurboButton(x + innerW - halfW, y, halfW, btnH,
                Text.literal("Discord"),
                this::openDiscord));
        y += btnH + gap + 2;

        this.addDrawableChild(new TurboButton(x, y, innerW, btnH,
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        drawGradient(context);
        drawBranding(context);
        drawPanel(context);
        super.render(context, mouseX, mouseY, delta); // widgets
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

        // slow drifting highlight band — 2 quads, gives the screen life without GPU cost
        float phase = (System.currentTimeMillis() % 12000L) / 12000f;
        int bandY = (int) (this.height * (phase * 1.4f - 0.2f));
        int bandH = Math.max(40, this.height / 6);
        context.fill(0, bandY, this.width, bandY + bandH, BAND);
        context.fill(0, bandY + bandH, this.width, bandY + bandH + 20, 0x0C3DD6D0);
    }

    /** Card behind the buttons. */
    private void drawPanel(DrawContext context) {
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        context.fill(panelX, panelY, panelX + panelW, panelY + 1, 0x803DD6D0);
        context.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0x303DD6D0);
        context.fill(panelX, panelY, panelX + 1, panelY + panelH, 0x303DD6D0);
        context.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0x303DD6D0);
    }

    /** Logo + tagline, always above the panel and always fully on screen. */
    private void drawBranding(DrawContext context) {
        int centerX = this.width / 2;
        String logo = "TURBOCRAFT";

        int logoH = this.textRenderer.fontHeight * logoScale;
        int logoW = this.textRenderer.getWidth(logo) * logoScale;

        // place the logo so it ends ~8px above the panel, but never above y=4
        int logoY = panelY - 10 - logoH;
        logoY = Math.max(4, logoY);

        int logoX = centerX - logoW / 2;

        if (logoScale > 1) {
            context.getMatrices().push();
            context.getMatrices().translate(logoX, logoY, 0);
            context.getMatrices().scale(logoScale, logoScale, 1.0f);
            context.drawText(this.textRenderer, logo, 1, 1, 0x403DD6D0, false);
            context.drawText(this.textRenderer, logo, -1, -1, 0x403DD6D0, false);
            context.drawText(this.textRenderer, logo, 0, 0, ACCENT, true);
            context.getMatrices().pop();
        } else {
            context.drawText(this.textRenderer, logo, logoX, logoY, ACCENT, true);
        }

        // tagline only when it genuinely fits between logo and panel
        int taglineY = logoY + logoH + 3;
        if (taglineY + 9 <= panelY - 2) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.tagline),
                    centerX, taglineY, MUTED);
        }
    }

    private void drawFooter(DrawContext context) {
        String clientVersion = FabricLoader.getInstance().getModContainer(TurboCraftClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
        // FabricLoader's "minecraft" container is the only reliable source here:
        // MinecraftClient#getGameVersion() returns the launcher profile name ("Fabric") in dev.
        // Version looks like "1.20.1+build.10", so keep only the release part.
        String mcVersion = FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
        int plus = mcVersion.indexOf('+');
        if (plus > 0) mcVersion = mcVersion.substring(0, plus);
        if (mcVersion.isBlank()) mcVersion = "1.20.1";

        int footerTop = this.height - 24;
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Minecraft " + mcVersion + " · Fabric"), 4, footerTop, MUTED);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("TurboCraft Client " + clientVersion), 4, footerTop + 10, ACCENT);

        Text disclaimer = Text.literal("Not affiliated with Mojang or Microsoft");
        context.drawTextWithShadow(this.textRenderer, disclaimer,
                this.width - this.textRenderer.getWidth(disclaimer) - 4, footerTop + 10, FAINT);
    }

    // ------------------------------------------------------------------ behaviour

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
}
