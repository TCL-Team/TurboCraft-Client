package com.turbocraft.client.gui;

import com.turbocraft.client.config.TurboCraftConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * The TurboCraft menu (Right Shift in-game).
 *
 * <p>Built from vanilla {@link ButtonWidget}s on purpose: custom widgets mean custom rendering,
 * and this has to stay legible at 30 FPS on a phone.
 */
public class ClientMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;

    private final Screen parent;

    public ClientMenuScreen(Screen parent) {
        super(Text.literal("TurboCraft Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TurboCraftConfig config = TurboCraftConfig.get();
        int centerX = this.width / 2;
        int startY = this.height / 2 - 80;

        int y = startY;
        y = toggleButton("HUD: %s", config.hudEnabled, v -> config.hudEnabled = v, centerX, y);
        y = toggleButton("FPS counter: %s", config.showFps, v -> config.showFps = v, centerX, y);
        y = toggleButton("CPS counter: %s", config.showCps, v -> config.showCps = v, centerX, y);
        y = toggleButton("Coordinates: %s", config.showCoords, v -> config.showCoords = v, centerX, y);
        y = toggleButton("Keystrokes: %s", config.showKeystrokes, v -> config.showKeystrokes = v, centerX, y);
        y = toggleButton("Ping: %s", config.showPing, v -> config.showPing = v, centerX, y);
        y = toggleButton("HUD background: %s", config.hudBackground, v -> config.hudBackground = v, centerX, y);

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Done"),
                        button -> this.close())
                .dimensions(centerX - BUTTON_WIDTH / 2, y + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    /** Adds a button whose label shows its current state; returns the next Y. */
    private int toggleButton(String label, boolean initial, Consumer<Boolean> setter,
                             int centerX, int y) {
        boolean[] state = {initial};
        ButtonWidget button = ButtonWidget.builder(
                        Text.literal(format(label, state[0])),
                        b -> {
                            state[0] = !state[0];
                            setter.accept(state[0]);
                            b.setMessage(Text.literal(format(label, state[0])));
                            TurboCraftConfig.save();
                        })
                .dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addDrawableChild(button);
        return y + BUTTON_HEIGHT + GAP;
    }

    private static String format(String label, boolean on) {
        return String.format(label, on ? "ON" : "OFF");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 2 - 110, 0xFF55FFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Right Shift to toggle • settings save automatically"),
                this.width / 2, this.height - 24, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false; // on a server, opening the menu should not freeze the world
    }
}
