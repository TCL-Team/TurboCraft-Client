package com.turbocraft.client.gui.widget;

import com.turbocraft.client.gui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * A settings row: label on the left, neon switch on the right.
 *
 * <p>Drawn entirely with {@code fill} quads — no textures, no vanilla widget atlas — because that is
 * the cheapest thing to rasterise on Android's GL4ES / MobileGlues path.
 */
public class TurboToggle extends PressableWidget {
    private static final int TRACK_W = 26;
    private static final int TRACK_H = 12;

    private final String label;
    private final String description;
    private final Consumer<Boolean> onChange;
    private final TextRenderer renderer;
    private boolean value;

    public TurboToggle(int x, int y, int width, int height, String label, String description,
                       boolean initial, Consumer<Boolean> onChange) {
        super(x, y, width, height, Text.literal(label));
        this.label = label;
        this.description = description;
        this.value = initial;
        this.onChange = onChange;
        this.renderer = MinecraftClient.getInstance().textRenderer;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean v) {
        this.value = v;
    }

    @Override
    public void onPress() {
        value = !value;
        if (onChange != null) onChange.accept(value);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = mouseX >= this.getX() && mouseY >= this.getY()
                && mouseX < this.getX() + this.getWidth()
                && mouseY < this.getY() + this.getHeight();

        int x = this.getX();
        int y = this.getY();
        int right = x + this.getWidth();
        int bottom = y + this.getHeight();

        context.fill(x, y, right, bottom, hovered ? Theme.PANEL_HOVER : Theme.PANEL);
        context.fill(x, y, x + 1, bottom, hovered ? Theme.accent() : Theme.border());

        // label
        int textY = description == null
                ? y + (this.getHeight() - 8) / 2
                : y + 5;
        context.drawTextWithShadow(this.renderer, Text.literal(label), x + 7, textY,
                value ? Theme.TEXT : Theme.TEXT_DIM);
        if (description != null) {
            context.drawTextWithShadow(this.renderer, Text.literal(description), x + 7, y + 15,
                    Theme.TEXT_FAINT);
        }

        // switch
        int trackX = right - TRACK_W - 7;
        int trackY = y + (this.getHeight() - TRACK_H) / 2;
        int trackColor = value ? Theme.accent() : 0x33FFFFFF;
        context.fill(trackX, trackY, trackX + TRACK_W, trackY + TRACK_H, trackColor);
        context.fill(trackX, trackY, trackX + TRACK_W, trackY + 1, Theme.withAlpha(0xFFFFFFFF, 0x22));
        context.fill(trackX, trackY + TRACK_H - 1, trackX + TRACK_W, trackY + TRACK_H,
                Theme.withAlpha(0x00000000, 0x44));

        int knobSize = TRACK_H - 4;
        int knobX = value ? trackX + TRACK_W - knobSize - 2 : trackX + 2;
        // The knob is a solid block: dark when the track is bright, light when the track is dim.
        int knobColor = value ? Theme.textOnAccent() : 0xFFB0BCB4;
        context.fill(knobX, trackY + 2, knobX + knobSize, trackY + 2 + knobSize, knobColor);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
