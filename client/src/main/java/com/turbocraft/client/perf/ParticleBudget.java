package com.turbocraft.client.perf;

import com.turbocraft.client.config.TurboCraftConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Hard cap on how many particles the client is allowed to spawn per tick.
 *
 * <p>This is the one honest optimisation that matters for explosion/TNT tests. A mass-TNT scene
 * spawns tens of thousands of particles in a handful of ticks; every one is a separate quad, a
 * separate tick, and a separate entry in the render queue. The GPU is not the bottleneck there —
 * the single-threaded particle tick is. Skipping the spawns past the budget costs a few visuals and
 * buys back 10-40 FPS.
 *
 * <p>Everything stays client-side: the server still runs the explosions, block damage and knockback
 * exactly as before. Only the visual sparks are thinned out, which is what "particle settings" in
 * vanilla do anyway — just with a per-tick budget instead of an on/off switch.
 *
 * <p>Budget is per tick (20 per second), so the number is comparable across devices:
 * {@code 0 = unlimited}, {@code 300-600 = sane phone values}, {@code 1200+ = practically vanilla}.
 */
public final class ParticleBudget {

    private static int tokens;
    private static int spawnedThisTick;
    private static int spawnedThisSecond;
    private static int cachedPerSecond;
    private static long secondWindowStart = System.currentTimeMillis();
    private static int skippedThisSecond;
    private static int cachedSkippedPerSecond;

    private ParticleBudget() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            spawnedThisTick = 0;
            tokens = Math.max(0, TurboCraftConfig.get().particleBudget);
        });
    }

    /**
     * Called from the {@code ParticleManager} mixin before a particle is created.
     *
     * @return {@code true} to allow the spawn, {@code false} to skip it
     */
    public static boolean allowSpawn() {
        int budget = TurboCraftConfig.get().particleBudget;
        if (budget <= 0) {
            count();
            return true;
        }
        if (tokens <= 0) {
            skippedThisSecond++;
            rollSecond();
            return false;
        }
        tokens--;
        count();
        return true;
    }

    private static void count() {
        spawnedThisTick++;
        spawnedThisSecond++;
        rollSecond();
    }

    private static void rollSecond() {
        long now = System.currentTimeMillis();
        long elapsed = now - secondWindowStart;
        if (elapsed >= 1000L) {
            cachedPerSecond = (int) (spawnedThisSecond * 1000L / elapsed);
            cachedSkippedPerSecond = (int) (skippedThisSecond * 1000L / elapsed);
            spawnedThisSecond = 0;
            skippedThisSecond = 0;
            secondWindowStart = now;
        }
    }

    /** Particles actually created in the last second. */
    public static int perSecond() {
        return cachedPerSecond;
    }

    /** Particles the budget refused in the last second — the measurable saving. */
    public static int skippedPerSecond() {
        return cachedSkippedPerSecond;
    }

    public static int spawnedThisTick() {
        return spawnedThisTick;
    }

    /** For the HUD/PERF tab: is the cap actually doing anything right now? */
    public static boolean isCapping() {
        return TurboCraftConfig.get().particleBudget > 0 && cachedSkippedPerSecond > 0;
    }
}
