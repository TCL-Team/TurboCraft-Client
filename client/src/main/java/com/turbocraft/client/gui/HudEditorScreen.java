package com.turbocraft.client.gui;

import com.turbocraft.client.config.TurboCraftConfig;
import com.turbocraft.client.hud.HudRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Drag-to-move HUD editor.
 *
 * <p>Opened from the TurboCraft menu. The HUD is drawn in preview mode (sample values) so it can be
 * positioned before joining a server. Position and scale are saved to {@code config/turbocraft.json}.
 */
public class HudEditorScreen extends Screen {
    private final Screen parent;

    private boolean dragging;
    private double grabX;
    private double grabY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("TurboCraft — HUD Editor"));
        this.parent = parent;
    }

    private static TurboCraftConfig config() {
        return TurboCraftConfig.get();
    }

    @Override
    protected void init() {
        HudRenderer.suppressed = true;

        int y = this.height - 28;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
                .dimensions(this.width / 2 - 152, y, 96, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> {
            TurboCraftConfig c = config();
            c.hudX = 4;
            c.hudY = 4;
            c.hudScale = 1.0f;
            TurboCraftConfig.save();
        }).dimensions(this.width / 2 - 48, y, 96, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> changeScale(-0.1f))
                .dimensions(this.width / 2 + 56, y, 46, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> changeScale(0.1f))
                .dimensions(this.width / 2 + 106, y, 46, 20).build());
    }

    private void changeScale(float delta) {
        TurboCraftConfig c = config();
        float next = c.hudScale + delta;
        next = Math.round(next * 10f) / 10f;
        c.hudScale = Math.max(0.5f, Math.min(2.0f, next));
        TurboCraftConfig.save();
    }

    private int[] boxSize() {
        int[] size = HudRenderer.measure(this.client, config());
        TurboCraftConfig c = config();
        return new int[]{(int) (size[0] * c.hudScale), (int) (size[1] * c.hudScale)};
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        TurboCraftConfig c = config();
        int[] box = boxSize();

        // outline around the drag area
        int outline = 0x80FFFFFF;
        context.fill(c.hudX - 2, c.hudY - 2, c.hudX + box[0] + 2, c.hudY - 1, outline);
        context.fill(c.hudX - 2, c.hudY + box[1] + 1, c.hudX + box[0] + 2, c.hudY + box[1] + 2, outline);
        context.fill(c.hudX - 2, c.hudY - 2, c.hudX - 1, c.hudY + box[1] + 2, outline);
        context.fill(c.hudX + box[0] + 1, c.hudY - 2, c.hudX + box[0] + 2, c.hudY + box[1] + 2, outline);

        HudRenderer.drawHud(context, this.client, c, c.hudX, c.hudY, true);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 12, 0xFF55FFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Drag the HUD to move • +/- to resize • auto-saved"),
                this.width / 2, 26, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(String.format("scale: %.1fx", c.hudScale)),
                this.width / 2, 38, 0xFFAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            TurboCraftConfig c = config();
            int[] box = boxSize();
            if (mouseX >= c.hudX && mouseX <= c.hudX + box[0]
                    && mouseY >= c.hudY && mouseY <= c.hudY + box[1]) {
                dragging = true;
                grabX = mouseX - c.hudX;
                grabY = mouseY - c.hudY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            TurboCraftConfig c = config();
            c.hudX = (int) Math.max(0, Math.min(this.width - 8, mouseX - grabX));
            c.hudY = (int) Math.max(0, Math.min(this.height - 8, mouseY - grabY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            TurboCraftConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        HudRenderer.suppressed = false;
        TurboCraftConfig.save();
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false; // keep the world running so the HUD can be positioned against real gameplay
    }
}
