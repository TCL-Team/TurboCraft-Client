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
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.net.URI;

/**
 * TurboCraft custom title screen — FastClient/Lunar-style layout, neon green on black.
 *
 * <p>Layout: full-bleed background image with a dark overlay, small logo + wordmark, centred stacked
 * button column, Quit at the bottom, Discord card bottom-right, version line under the column.
 *
 * <p>Android notes: the background is <b>static</b> (vanilla's panorama re-renders a 6-face cubemap
 * every frame, which is genuinely expensive on a phone), and the darkening overlay + all panels are
 * {@code fill} quads, not textures.
 */
public class TurboCraftTitleScreen extends Screen {
    private static final Identifier BACKGROUND =
            new Identifier(TurboCraftClient.MOD_ID, "textures/gui/title_background.png");

    private static final String[] TAGLINES = {
            "neon green. black glass. no lag.",
            "HUD that actually tells the truth",
            "1% low > big FPS number",
            "made for phones, not ported to them",
            "right shift opens the good stuff",
    };

    private final String tagline = TAGLINES[(int) (Math.random() * TAGLINES.length)];

    // ---- layout, computed in init() ----
    private int btnW;
    private int btnH;
    private int colX;
    private int colTop;

    private int discordX;
    private int discordY;
    private int discordW;
    private int discordH;

    private int logoY;
    private boolean discordVisible = true;

    public TurboCraftTitleScreen() {
        super(Text.literal("TurboCraft"));
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        btnW = Math.min(236, this.width - 40);
        btnH = this.height < 300 ? 18 : 22;
        int gap = btnH < 20 ? 3 : 4;

        // 5 rows: singleplayer, multiplayer, cosmetics, store, quit -> quit is separated
        int rows = 4;
        colTop = this.height / 2 - (rows * btnH + (rows - 1) * gap) / 2 + 6;
        colTop = Math.max(74, colTop);
        colX = this.width / 2 - btnW / 2;

        logoY = Math.max(16, colTop - 58);

        int y = colTop;
        this.addDrawableChild(new TurboButton(colX, y, btnW, btnH,
                Text.literal("Singleplayer"),
                () -> this.client.setScreen(new SelectWorldScreen(this)),
                TurboButton.Style.NORMAL, "SP"));
        y += btnH + gap;

        this.addDrawableChild(new TurboButton(colX, y, btnW, btnH,
                Text.literal("Multiplayer"),
                () -> this.client.setScreen(new MultiplayerScreen(this)),
                TurboButton.Style.NORMAL, "MP"));
        y += btnH + gap;

        this.addDrawableChild(new TurboButton(colX, y, btnW, btnH,
                Text.literal("TurboCraft Settings"),
                () -> this.client.setScreen(new ClientMenuScreen(this)),
                TurboButton.Style.NORMAL, ">_"));
        y += btnH + gap;

        this.addDrawableChild(new TurboButton(colX, y, btnW, btnH,
                Text.literal("Store / Cosmetics"),
                this::openStore,
                TurboButton.Style.ACCENT, "ST"));

        // Quit, floating at the bottom of the column
        int quitY = Math.min(this.height - 46, y + btnH + 26);
        this.addDrawableChild(new TurboButton(colX, quitY, btnW, btnH,
                Text.literal("Quit Game"),
                () -> this.client.scheduleStop(),
                TurboButton.Style.DANGER));

        // Discord card, bottom-right. It must never overlap the centred column: on narrow screens
        // the column and the card would otherwise collide (exactly what happened at 1000x562).
        discordW = 122;
        discordH = 62;
        int cardRight = this.width - 12;
        discordX = cardRight - discordW;
        discordY = this.height - discordH - 26;

        boolean cardFits = discordX > colX + btnW + 8;
        if (!cardFits) {
            // narrow / phone-portrait: shrink and push it below the column's last button
            discordW = Math.min(118, this.width - 24);
            discordH = 46;
            discordX = this.width - discordW - 10;
            discordY = this.height - discordH - 6;
        }
        discordVisible = cardFits || (discordY > quitY + btnH + 4);
        if (!discordVisible) {
            discordW = 0;
            discordH = 0;
        }
    }

    private void openStore() {
        openUrl(TurboCraftConfig.get().storeUrl);
    }

    private void openDiscord() {
        openUrl(TurboCraftConfig.get().discordUrl);
    }

    private void openUrl(String url) {
        if (url == null || url.isBlank()) return;
        try {
            Util.getOperatingSystem().open(URI.create(url));
        } catch (Exception e) {
            TurboCraftClient.LOGGER.warn("[TurboCraft] Could not open link: {}", url, e);
        }
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(DrawContext context) {
        // intentionally empty — we paint our own background in render()
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        drawBackground(context);
        drawBranding(context);
        super.render(context, mouseX, mouseY, delta); // buttons
        drawVersionLine(context);
        drawDiscordCard(context, mouseX, mouseY);
        drawFooter(context);
    }

    /** Static image, cover-fit, plus vertical darkening so text stays readable. */
    private void drawBackground(DrawContext context) {
        int imgW = 1280;
        int imgH = 720;
        float scale = Math.max((float) this.width / imgW, (float) this.height / imgH);
        int drawW = Math.round(imgW * scale);
        int drawH = Math.round(imgH * scale);
        int drawX = (this.width - drawW) / 2;
        int drawY = (this.height - drawH) / 2;

        context.drawTexture(BACKGROUND, drawX, drawY, 0, 0, drawW, drawH, imgW, imgH);

        // darken in horizontal bands, heavier at the bottom so the button column pops.
        // Kept light at the top: the generated art is already dark, and over-darkening it made the
        // whole screen look like a flat black rectangle.
        int bands = 18;
        for (int i = 0; i < bands; i++) {
            int y0 = this.height * i / bands;
            int y1 = this.height * (i + 1) / bands;
            float t = i / (float) (bands - 1);
            int alpha = (int) (0x10 + 0x3C * t);
            context.fill(0, y0, this.width, y1, alpha << 24);
        }
        // faint neon wash from the top-left corner
        context.fill(0, 0, this.width, this.height, 0x0839FF14);
    }

    /** Logo glyph + wordmark + tagline. */
    private void drawBranding(DrawContext context) {
        int centerX = this.width / 2;

        // logo mark: green cube outline drawn from fills (no texture, scales cleanly)
        int markSize = 18;
        int markX = centerX - 62;
        drawCubeMark(context, markX, logoY, markSize);

        context.getMatrices().push();
        context.getMatrices().translate(markX + markSize + 8, logoY + 2, 0);
        context.getMatrices().scale(1.5f, 1.5f, 1.0f);
        context.drawText(this.textRenderer, "TURBOCRAFT", 0, 0, Theme.ACCENT, true);
        context.getMatrices().pop();

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(tagline),
                centerX, logoY + 30, Theme.TEXT_DIM);
    }

    /** A blocky cube outline + inner highlight, all from {@code fill} quads. */
    private static void drawCubeMark(DrawContext context, int x, int y, int size) {
        // outer square
        context.fill(x, y, x + size, y + 1, Theme.ACCENT);
        context.fill(x, y + size - 1, x + size, y + size, Theme.ACCENT);
        context.fill(x, y + 1, x + 1, y + size - 1, Theme.ACCENT);
        context.fill(x + size - 1, y + 1, x + size, y + size - 1, Theme.ACCENT);

        // top face + right face suggestion
        context.fill(x + 4, y + 4, x + size - 4, y + 5, Theme.ACCENT_DIM);
        context.fill(x + size - 5, y + 4, x + size - 4, y + size - 4, Theme.ACCENT_DIM);
        context.fill(x + 4, y + size - 5, x + size - 4, y + size - 4, Theme.ACCENT_FAINT);
    }

    private void drawVersionLine(DrawContext context) {
        String clientVersion = FabricLoader.getInstance().getModContainer(TurboCraftClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
        // FabricLoader's "minecraft" container is the only reliable source here:
        // MinecraftClient#getGameVersion() returns the launcher profile name ("Fabric") in dev.
        String mcVersion = FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("1.20.1");
        int plus = mcVersion.indexOf('+');
        if (plus > 0) mcVersion = mcVersion.substring(0, plus);

        Text line = Text.literal("TurboCraft " + clientVersion + "  |  mc " + mcVersion);
        context.drawCenteredTextWithShadow(this.textRenderer, line,
                this.width / 2, this.height - 11, Theme.TEXT_FAINT);
    }

    /** Discord promo card — clickable region, not a widget, so it can sit anywhere. */
    private void drawDiscordCard(DrawContext context, int mouseX, int mouseY) {
        if (!discordVisible) return;
        boolean hovered = mouseX >= discordX && mouseX < discordX + discordW
                && mouseY >= discordY && mouseY < discordY + discordH;

        context.fill(discordX, discordY, discordX + discordW, discordY + discordH,
                hovered ? Theme.PANEL_HOVER : Theme.PANEL_SOLID);
        int border = hovered ? Theme.ACCENT : Theme.BORDER;
        context.fill(discordX, discordY, discordX + discordW, discordY + 1, border);
        context.fill(discordX, discordY + discordH - 1, discordX + discordW, discordY + discordH, border);
        context.fill(discordX, discordY + 1, discordX + 1, discordY + discordH - 1, border);
        context.fill(discordX + discordW - 1, discordY + 1, discordX + discordW, discordY + discordH - 1, border);

        // simple "chat bubble" glyph built from fills
        int iconSize = Math.min(22, discordH - 34);
        int iconX = discordX + (discordW - iconSize) / 2;
        int iconY = discordY + 8;
        context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize - 4, Theme.ACCENT);
        context.fill(iconX + 4, iconY + iconSize - 4, iconX + iconSize - 4, iconY + iconSize, Theme.ACCENT);
        context.fill(iconX + 5, iconY + 5, iconX + 7, iconY + 7, Theme.TEXT_ON_ACCENT);
        context.fill(iconX + 10, iconY + 5, iconX + 12, iconY + 7, Theme.TEXT_ON_ACCENT);
        context.fill(iconX + 15, iconY + 5, iconX + 17, iconY + 7, Theme.TEXT_ON_ACCENT);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("JOIN OUR DISCORD"),
                discordX + discordW / 2, discordY + iconSize + 12, Theme.ACCENT);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("CLICK HERE"),
                discordX + discordW / 2, discordY + iconSize + 21, Theme.TEXT_DIM);
    }

    private void drawFooter(DrawContext context) {
        Text disclaimer = Text.literal("Not affiliated with Mojang or Microsoft");
        context.drawTextWithShadow(this.textRenderer, disclaimer,
                6, this.height - 11, Theme.TEXT_FAINT);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= discordX && mouseX < discordX + discordW
                && mouseY >= discordY && mouseY < discordY + discordH) {
            openDiscord();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ------------------------------------------------------------------ behaviour

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
