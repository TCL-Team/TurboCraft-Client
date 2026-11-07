package com.turbocraft.client.gui.widget;

import com.turbocraft.client.gui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

/**
 * TurboCraft styled button — black glass with neon green accents.
 *
 * <p>Extends {@link PressableWidget} and overrides {@code renderButton} completely, so we never touch
 * vanilla's 200x20 button texture atlas. Everything is drawn with {@code DrawContext#fill}, which is
 * the cheapest path on Android's GL4ES / MobileGlues renderers (no texture bindings per frame).
 */
public class TurboButton extends PressableWidget {
    public enum Style {
        /** Default: black glass, green left key-line, white text. */
        NORMAL,
        /** Filled green — for the "main" action. */
        ACCENT,
        /** Muted red — for Quit. */
        DANGER,
        /** Currently-open entry in a sidebar/nav. */
        SELECTED
    }

    private final Runnable action;
    private final Style style;
    private final String icon;

    /**
     * Text renderer captured at construction time — {@code PressableWidget} does not expose one, and
     * in 1.20.1 the global {@code MinecraftClient.getInstance().textRenderer} is not yet initialised
     * at the moment the title screen widgets are created.
     */
    private final TextRenderer renderer;

    public TurboButton(int x, int y, int width, int height, Text message, Runnable action) {
        this(x, y, width, height, message, action, Style.NORMAL, "");
    }

    public TurboButton(int x, int y, int width, int height, Text message, Runnable action,
                       Style style) {
        this(x, y, width, height, message, action, style, "");
    }

    /**
     * @param icon drawn before the label, e.g. "SP", "MP", "ST". Plain ASCII on purpose — the default
     *             font has no emoji, and a missing glyph shows up as a black square on phones.
     */
    public TurboButton(int x, int y, int width, int height, Text message, Runnable action,
                       Style style, String icon) {
        super(x, y, width, height, message);
        this.action = action;
        this.style = style;
        this.icon = icon;
        this.renderer = MinecraftClient.getInstance().textRenderer;
    }

    /** Declared as public: {@code PressableWidget#onPress} is package-private in 1.20.1. */
    public void onPress() {
        if (action != null) action.run();
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = (mouseX >= this.getX() && mouseY >= this.getY()
                && mouseX < this.getX() + this.getWidth()
                && mouseY < this.getY() + this.getHeight()) && this.active;

        int x = this.getX();
        int y = this.getY();
        int right = x + this.getWidth();
        int bottom = y + this.getHeight();
        int textColor;
        int fill;
        int border;

        switch (this.style) {
            case ACCENT -> {
                fill = hovered ? Theme.accentBright() : Theme.accent();
                textColor = Theme.textOnAccent();
                border = Theme.accentBright();
            }
            case SELECTED -> {
                fill = hovered ? Theme.PANEL_HOVER : Theme.withAlpha(Theme.accent(), 0x2E);
                textColor = Theme.accent();
                border = Theme.accent();
            }
            case DANGER -> {
                fill = hovered ? Theme.DANGER_HOVER : Theme.DANGER;
                textColor = Theme.DANGER_TEXT;
                border = 0x66FF6B5A;
            }
            default -> {
                fill = hovered ? Theme.PANEL_HOVER : Theme.PANEL;
                textColor = hovered ? Theme.accent() : Theme.TEXT;
                border = hovered ? Theme.accent() : Theme.border();
            }
        }

        if (!this.active) {
            fill = Theme.PANEL;
            textColor = Theme.TEXT_FAINT;
            border = Theme.accentFaint();
        }

        context.fill(x, y, right, bottom, fill);

        // 1px border
        context.fill(x, y, right, y + 1, border);
        context.fill(x, bottom - 1, right, bottom, border);
        context.fill(x, y + 1, x + 1, bottom - 1, border);
        context.fill(right - 1, y + 1, right, bottom - 1, border);

        // left key-line (the FastClient-ish detail)
        if (this.active) {
            int keyLine = this.style == Style.ACCENT ? Theme.accentBright() : Theme.accent();
            context.fill(x + 1, y + 1, x + 3, bottom - 1, keyLine);
        }

        int textY = y + (this.getHeight() - 8) / 2 + 1;
        int labelWidth = this.renderer.getWidth(this.getMessage());
        int iconWidth = this.icon.isEmpty() ? 0 : this.renderer.getWidth(this.icon) + 5;
        int contentWidth = labelWidth + iconWidth;
        int contentX = x + (this.getWidth() - contentWidth) / 2;

        if (!this.icon.isEmpty()) {
            context.drawTextWithShadow(this.renderer, this.icon, contentX, textY,
                    this.style == Style.SELECTED ? Theme.accent() : textColor);
        }
        context.drawTextWithShadow(this.renderer, this.getMessage(),
                contentX + iconWidth, textY, textColor);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
