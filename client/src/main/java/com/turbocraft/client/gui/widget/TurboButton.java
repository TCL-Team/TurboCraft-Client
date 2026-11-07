package com.turbocraft.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

/**
 * TurboCraft styled button.
 *
 * <p>Extends {@link PressableWidget} and overrides {@code renderWidget} completely, so we never touch
 * vanilla's 200x20 button texture atlas. Everything is drawn with {@code DrawContext#fill}, which is
 * the cheapest path on Android's GL4ES / MobileGlues renderers (no texture bindings per frame).
 */
public class TurboButton extends PressableWidget {
    private static final int ACCENT = 0xFF3DD6D0;
    private static final int ACCENT_DIM = 0x553DD6D0;
    private static final int FILL_NORMAL = 0xC80D1420;
    private static final int FILL_HOVER = 0xF01A2734;
    private static final int FILL_ACCENT = 0xF03DD6D0;
    private static final int TEXT_NORMAL = 0xFFE8F4F4;
    private static final int TEXT_ACCENT_BTN = 0xFF06141A;

    private final Runnable action;
    private final boolean accent;

    public TurboButton(int x, int y, int width, int height, Text message, Runnable action) {
        this(x, y, width, height, message, action, false);
    }

    public TurboButton(int x, int y, int width, int height, Text message, Runnable action,
                       boolean accent) {
        super(x, y, width, height, message);
        this.action = action;
        this.accent = accent;
    }

    @Override
    public void onPress() {
        if (action != null) action.run();
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered() && this.active;
        int x = this.getX();
        int y = this.getY();
        int right = x + this.getWidth();
        int bottom = y + this.getHeight();

        int fill;
        int textColor;
        int border;

        if (!this.active) {
            fill = 0x600D1420;
            textColor = 0xFF6B7C86;
            border = 0x406B7C86;
        } else if (accent) {
            fill = hovered ? 0xFF6BF0E9 : FILL_ACCENT;
            textColor = TEXT_ACCENT_BTN;
            border = 0xFFFFFFFF;
        } else if (hovered) {
            fill = FILL_HOVER;
            textColor = 0xFFFFFFFF;
            border = ACCENT;
        } else {
            fill = FILL_NORMAL;
            textColor = TEXT_NORMAL;
            border = ACCENT_DIM;
        }

        context.fill(x, y, right, bottom, fill);

        // 1px border, drawn as four thin quads (no rounded corners: keeps it cheap and crisp)
        context.fill(x, y, right, y + 1, border);
        context.fill(x, bottom - 1, right, bottom, border);
        context.fill(x, y + 1, x + 1, bottom - 1, border);
        context.fill(right - 1, y + 1, right, bottom - 1, border);

        // accent key-line on the left edge
        if (this.active) {
            context.fill(x + 1, y + 1, x + 3, bottom - 1, accent ? 0xFFFFFFFF : ACCENT);
        }

        int textY = y + (this.getHeight() - 8) / 2 + 1;
        context.drawCenteredTextWithShadow(this.getTextRenderer(), this.getMessage(),
                x + this.getWidth() / 2, textY, textColor);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
