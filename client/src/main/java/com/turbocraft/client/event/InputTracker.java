package com.turbocraft.client.event;

import net.minecraft.client.MinecraftClient;

/**
 * Tracks raw left/right clicks so the CPS HUD counts actual clicks instead of guessing.
 *
 * <p>The Mouse mixin calls {@link #onMouseButton(int)} with the GLFW button id.
 */
public final class InputTracker {
    /** Click timestamps kept in a ring buffer so the counter rolls off smoothly. */
    private static final long[] LEFT_CLICKS = new long[64];
    private static final long[] RIGHT_CLICKS = new long[64];
    private static int leftIndex;
    private static int rightIndex;

    private static int cpsLeft;
    private static int cpsRight;

    private InputTracker() {
    }

    public static void init() {
        // Force class load at startup so the first click never races initialisation.
    }

    /** Called from the Mouse mixin for every physical click press. */
    public static void onMouseButton(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) {
            LEFT_CLICKS[leftIndex++ % LEFT_CLICKS.length] = now;
        } else if (button == 1) {
            RIGHT_CLICKS[rightIndex++ % RIGHT_CLICKS.length] = now;
        }
    }

    /** Called once per client tick (20x/second) — plenty often for a CPS readout. */
    public static void onClientTick() {
        long cutoff = System.currentTimeMillis() - 1000L;
        cpsLeft = countAfter(LEFT_CLICKS, cutoff);
        cpsRight = countAfter(RIGHT_CLICKS, cutoff);
    }

    public static int getCpsLeft() {
        return cpsLeft;
    }

    public static int getCpsRight() {
        return cpsRight;
    }

    /** Cached ping of the player list entry; -1 when not in a multiplayer world. */
    public static int getPing() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return -1;
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    private static int countAfter(long[] buffer, long cutoff) {
        int count = 0;
        for (long time : buffer) {
            if (time > cutoff) count++;
        }
        return count;
    }
}
