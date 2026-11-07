package com.turbocraft.client.hud;

import com.turbocraft.client.config.TurboCraftConfig;
import com.turbocraft.client.event.InputTracker;
import com.turbocraft.client.gui.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

/**
 * Draws the TurboCraft HUD.
 *
 * <p><b>Honest note:</b> this class does not improve FPS. It adds a handful of {@code fill()} calls
 * and text draws per frame — a fraction of a millisecond. Any FPS you gain on Android comes from the
 * renderer (MobileGlues / Zink), Sodium/Lithium, resolution scale and render distance, not from here.
 * The FPS graph and 1% low exist so those claims can actually be measured instead of guessed.
 *
 * <p>Only vanilla text + {@code DrawContext#fill} are used — no custom fonts, textures or shaders,
 * because on Android every extra asset means more VRAM pressure on the GL4ES / MobileGlues path.
 */
public final class HudRenderer {
    public static final int LINE = 10;
    public static final int KEY_SIZE = 16;
    public static final int KEY_GAP = 2;
    public static final int GRAPH_WIDTH = 60;
    public static final int GRAPH_HEIGHT = 14;

    private static final int PADDING = 3;
    private static final int WHITE = Theme.TEXT;
    private static final int BG = Theme.PANEL;
    private static int accent() { return Theme.accent(); }
    private static final int MUTED = Theme.TEXT_DIM;

    // ---- frame timing (for FPS, graph and 1% low) ----
    private static final int SAMPLES = 240;
    private static final long[] FRAME_NANOS = new long[SAMPLES];
    private static int frameIndex;
    private static long lastFrameNanos;
    private static long fpsWindowStart = System.currentTimeMillis();
    private static int fpsFrames;
    private static int cachedFps;
    private static int cachedOnePercentLow;

    /** Set while the HUD editor is open so the live HUD does not draw twice. */
    public static boolean suppressed;

    private HudRenderer() {
    }

    public static void init() {
        HudRenderCallback.EVENT.register(HudRenderer::onHudRender);
    }

    private static void onHudRender(DrawContext context, float tickDelta) {
        trackFrame();

        MinecraftClient client = MinecraftClient.getInstance();
        TurboCraftConfig config = TurboCraftConfig.get();

        if (suppressed) return;
        if (!config.hudEnabled) return;
        if (client.options.hudHidden || client.player == null) return;

        drawHud(context, client, config, config.hudX, config.hudY, false);
    }

    /**
     * Draws the whole HUD.
     *
     * @param preview true in the HUD editor: sample values are shown instead of live ones so the
     *                layout can be positioned before joining a world
     */
    public static void drawHud(DrawContext context, MinecraftClient client, TurboCraftConfig config,
                               int x, int y, boolean preview) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0);
        matrices.scale(config.hudScale, config.hudScale, 1.0f);

        int cursorY = 0;

        if (config.showFps) {
            drawLine(context, client, 0, cursorY, "FPS: " + (preview ? 144 : cachedFps), config);
            cursorY += LINE;
        }

        if (config.showGraph) {
            drawGraph(context, client, 0, cursorY, config, preview);
            cursorY += GRAPH_HEIGHT + 2;
        }

        if (config.showCps) {
            String cps = preview
                    ? "9 | 6"
                    : InputTracker.getCpsLeft() + " | " + InputTracker.getCpsRight();
            drawLine(context, client, 0, cursorY, "CPS: " + cps, config);
            cursorY += LINE;
        }

        boolean multiplayer = preview || (!client.isInSingleplayer() && client.getNetworkHandler() != null);
        if (config.showPing && multiplayer) {
            int ping = preview ? 23 : InputTracker.getPing();
            if (ping >= 0) {
                drawLine(context, client, 0, cursorY, "Ping: " + ping + "ms", config);
                cursorY += LINE;
            }
        }

        if (config.showCoords) {
            String coords = preview
                    ? "128 71 -240"
                    : client.player.getBlockPos().getX() + " "
                      + client.player.getBlockPos().getY() + " "
                      + client.player.getBlockPos().getZ();
            drawLine(context, client, 0, cursorY, "XYZ: " + coords, config);
            cursorY += LINE;
        }

        if (config.showKeystrokes) {
            cursorY += 2;
            drawKeystrokes(context, client, 0, cursorY, config, preview);
        }

        matrices.pop();
    }

    /** Approximate on-screen size at scale 1 — used by the HUD editor for the drag box. */
    public static int[] measure(MinecraftClient client, TurboCraftConfig config) {
        int width = 0;
        int height = 0;

        if (config.showFps) {
            width = Math.max(width, textWidth(client, "FPS: 144") + PADDING * 2);
            height += LINE;
        }
        if (config.showGraph) {
            width = Math.max(width, GRAPH_WIDTH + textWidth(client, "1%: 999") + 10);
            height += GRAPH_HEIGHT + 2;
        }
        if (config.showCps) {
            width = Math.max(width, textWidth(client, "CPS: 12 | 12") + PADDING * 2);
            height += LINE;
        }
        if (config.showPing) {
            width = Math.max(width, textWidth(client, "Ping: 999ms") + PADDING * 2);
            height += LINE;
        }
        if (config.showCoords) {
            width = Math.max(width, textWidth(client, "XYZ: -256 100 404") + PADDING * 2);
            height += LINE;
        }
        if (config.showKeystrokes) {
            width = Math.max(width, KEY_SIZE * 3 + KEY_GAP * 2);
            height += 2 + (KEY_SIZE + KEY_GAP) * 3;
        }
        return new int[]{Math.max(width, 40), Math.max(height, 12)};
    }

    private static int textWidth(MinecraftClient client, String text) {
        return client == null || client.textRenderer == null ? 60 : client.textRenderer.getWidth(text);
    }

    /** Frame-time bars + 1% low. Red = worse than 30 FPS, amber = worse than 50 FPS. */
    private static void drawGraph(DrawContext context, MinecraftClient client, int x, int y,
                                  TurboCraftConfig config, boolean preview) {
        if (config.hudBackground) {
            context.fill(x, y, x + GRAPH_WIDTH + 2, y + GRAPH_HEIGHT + 2, BG);
        }
        for (int i = 0; i < GRAPH_WIDTH; i++) {
            int index = frameIndex - GRAPH_WIDTH + i;
            if (index < 0) continue;
            long nanos = FRAME_NANOS[Math.floorMod(index, SAMPLES)];
            if (nanos <= 0) continue;

            double ms = preview ? 16.0 + (i % 7) : nanos / 1_000_000.0;
            int barHeight = (int) Math.round(GRAPH_HEIGHT - Math.min(GRAPH_HEIGHT, ms / 2.0));
            barHeight = Math.max(1, Math.min(GRAPH_HEIGHT, barHeight));
            int color = ms > 33 ? 0xFFFF4D4D : ms > 20 ? 0xFFFFC436 : accent();
            context.fill(x + 1 + i, y + 1 + (GRAPH_HEIGHT - barHeight),
                    x + 2 + i, y + 1 + GRAPH_HEIGHT, color);
        }

        int low = preview ? 96 : cachedOnePercentLow;
        context.drawText(client.textRenderer, "1%: " + low,
                x + GRAPH_WIDTH + 5, y + 4, MUTED, true);
    }

    /** WASD + space, Lunar style. */
    private static void drawKeystrokes(DrawContext context, MinecraftClient client, int x, int y,
                                       TurboCraftConfig config, boolean preview) {
        drawKey(context, client, x + KEY_SIZE + KEY_GAP, y, KEY_SIZE, KEY_SIZE, "W",
                preview || isDown(client, GLFW.GLFW_KEY_W), config);
        drawKey(context, client, x, y + KEY_SIZE + KEY_GAP, KEY_SIZE, KEY_SIZE, "A",
                !preview && isDown(client, GLFW.GLFW_KEY_A), config);
        drawKey(context, client, x + KEY_SIZE + KEY_GAP, y + KEY_SIZE + KEY_GAP,
                KEY_SIZE, KEY_SIZE, "S", !preview && isDown(client, GLFW.GLFW_KEY_S), config);
        drawKey(context, client, x + (KEY_SIZE + KEY_GAP) * 2, y + KEY_SIZE + KEY_GAP,
                KEY_SIZE, KEY_SIZE, "D", !preview && isDown(client, GLFW.GLFW_KEY_D), config);

        int spaceWidth = KEY_SIZE * 3 + KEY_GAP * 2;
        drawKey(context, client, x, y + (KEY_SIZE + KEY_GAP) * 2, spaceWidth, KEY_SIZE, "",
                !preview && isDown(client, GLFW.GLFW_KEY_SPACE), config);
    }

    private static void drawKey(DrawContext context, MinecraftClient client, int x, int y,
                                int width, int height, String label, boolean pressed,
                                TurboCraftConfig config) {
        int bg = pressed ? Theme.accent() : (config.hudBackground ? Theme.PANEL : Theme.accentFaint());
        context.fill(x, y, x + width, y + height, bg);
        if (!label.isEmpty()) {
            int textColor = pressed ? Theme.textOnAccent() : WHITE;
            int textX = x + (width - client.textRenderer.getWidth(label)) / 2;
            int textY = y + (height - 8) / 2 + 1;
            context.drawText(client.textRenderer, label, textX, textY, textColor, true);
        }
    }

    private static void drawLine(DrawContext context, MinecraftClient client, int x, int y,
                                 String text, TurboCraftConfig config) {
        if (config.hudBackground) {
            context.fill(x, y, x + client.textRenderer.getWidth(text) + PADDING * 2,
                    y + LINE, BG);
        }
        context.drawText(client.textRenderer, text, x + PADDING, y + 1, accent(), true);
    }

    private static boolean isDown(MinecraftClient client, int glfwKey) {
        if (client.currentScreen != null || client.getWindow() == null) return false;
        return InputUtil.isKeyPressed(client.getWindow().getHandle(), glfwKey);
    }

    // ---------------------------------------------------------------- frame stats

    private static void trackFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0) {
            FRAME_NANOS[Math.floorMod(frameIndex++, SAMPLES)] = now - lastFrameNanos;
        }
        lastFrameNanos = now;

        fpsFrames++;
        long elapsed = System.currentTimeMillis() - fpsWindowStart;
        if (elapsed >= 1000L) {
            cachedFps = (int) (fpsFrames * 1000L / elapsed);
            fpsFrames = 0;
            fpsWindowStart = System.currentTimeMillis();
            recomputeOnePercentLow();
        }
    }

    /**
     * 1% low = average of the slowest 1% frames in the sample window, expressed as FPS.
     * This is the number that actually tells you whether the game feels smooth.
     */
    private static void recomputeOnePercentLow() {
        long[] sorted = FRAME_NANOS.clone();
        Arrays.sort(sorted);

        int valid = 0;
        for (long value : sorted) {
            if (value > 0) valid++;
        }
        if (valid < 20) {
            cachedOnePercentLow = 0;
            return;
        }

        int worstCount = Math.max(1, valid / 100);
        long sum = 0;
        for (int i = sorted.length - worstCount; i < sorted.length; i++) {
            sum += sorted[i];
        }
        long averageNanos = sum / worstCount;
        cachedOnePercentLow = averageNanos <= 0 ? 0 : (int) (1_000_000_000L / averageNanos);
    }
}
