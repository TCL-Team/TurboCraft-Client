package com.turbocraft.client.hud;

import com.turbocraft.client.config.TurboCraftConfig;
import com.turbocraft.client.event.InputTracker;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Draws the TurboCraft HUD.
 *
 * <p>Only vanilla text + {@code DrawContext#fill} are used — no custom fonts, textures or shaders.
 * On Android every extra asset means more VRAM pressure on the GL4ES / MobileGlues path, and this
 * HUD has to stay readable at 30 FPS.
 */
public final class HudRenderer {
    private static final int PADDING = 3;
    private static final int LINE_HEIGHT = 10;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BG = 0x90000000;
    private static final int ACCENT = 0xFF55FFFF;

    private static long fpsWindowStart = System.currentTimeMillis();
    private static int fpsFrames;
    private static int cachedFps;

    private HudRenderer() {
    }

    public static void init() {
        HudRenderCallback.EVENT.register(HudRenderer::onHudRender);
    }

    private static void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TurboCraftConfig config = TurboCraftConfig.get();

        trackFps();

        if (!config.hudEnabled) return;
        if (client.options.hudHidden) return;
        if (client.player == null) return;

        int y = config.hudY;

        if (config.showFps) {
            drawLine(context, client, config.hudX, y, "FPS: " + cachedFps, config);
            y += LINE_HEIGHT;
        }

        if (config.showCps) {
            drawLine(context, client, config.hudX, y,
                    "CPS: " + InputTracker.getCpsLeft() + " | " + InputTracker.getCpsRight(), config);
            y += LINE_HEIGHT;
        }

        if (config.showPing && client.getNetworkHandler() != null) {
            int ping = InputTracker.getPing();
            if (ping >= 0) {
                drawLine(context, client, config.hudX, y, "Ping: " + ping + "ms", config);
                y += LINE_HEIGHT;
            }
        }

        if (config.showCoords) {
            var pos = client.player.getBlockPos();
            drawLine(context, client, config.hudX, y,
                    "XYZ: " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), config);
            y += LINE_HEIGHT;
        }

        if (config.showKeystrokes && client.currentScreen == null) {
            y += 2;
            drawKeystrokes(context, client, config.hudX, y, config);
        }
    }

    /** WASD + space, Lunar style. */
    private static void drawKeystrokes(DrawContext context, MinecraftClient client, int x, int y,
                                       TurboCraftConfig config) {
        int keyW = 16;
        int keyH = 16;
        int gap = 2;

        drawKey(context, client, x + keyW + gap, y, keyW, keyH, "W",
                isDown(client, GLFW.GLFW_KEY_W), config);
        drawKey(context, client, x, y + keyH + gap, keyW, keyH, "A",
                isDown(client, GLFW.GLFW_KEY_A), config);
        drawKey(context, client, x + keyW + gap, y + keyH + gap, keyW, keyH, "S",
                isDown(client, GLFW.GLFW_KEY_S), config);
        drawKey(context, client, x + (keyW + gap) * 2, y + keyH + gap, keyW, keyH, "D",
                isDown(client, GLFW.GLFW_KEY_D), config);

        int spaceWidth = keyW * 3 + gap * 2;
        drawKey(context, client, x, y + (keyH + gap) * 2, spaceWidth, keyH, "",
                isDown(client, GLFW.GLFW_KEY_SPACE), config);
    }

    private static void drawKey(DrawContext context, MinecraftClient client, int x, int y,
                                int width, int height, String label, boolean pressed,
                                TurboCraftConfig config) {
        int bg = pressed ? 0xB0FFFFFF : (config.hudBackground ? 0x90000000 : 0x40000000);
        context.fill(x, y, x + width, y + height, bg);
        if (!label.isEmpty()) {
            int textColor = pressed ? 0xFF000000 : WHITE;
            int textX = x + (width - client.textRenderer.getWidth(label)) / 2;
            int textY = y + (height - 8) / 2 + 1;
            context.drawText(client.textRenderer, label, textX, textY, textColor, true);
        }
    }

    private static void drawLine(DrawContext context, MinecraftClient client, int x, int y,
                                 String text, TurboCraftConfig config) {
        if (config.hudBackground) {
            context.fill(x, y, x + client.textRenderer.getWidth(text) + PADDING * 2,
                    y + LINE_HEIGHT, BG);
        }
        context.drawText(client.textRenderer, text, x + PADDING, y + 1, ACCENT, true);
    }

    private static boolean isDown(MinecraftClient client, int glfwKey) {
        if (client.currentScreen != null || client.getWindow() == null) return false;
        return InputUtil.isKeyPressed(client.getWindow().getHandle(), glfwKey);
    }

    private static void trackFps() {
        fpsFrames++;
        long now = System.currentTimeMillis();
        long elapsed = now - fpsWindowStart;
        if (elapsed >= 1000L) {
            cachedFps = (int) (fpsFrames * 1000L / elapsed);
            fpsFrames = 0;
            fpsWindowStart = now;
        }
    }
}
