package com.turbocraft.client.perf;

import com.turbocraft.client.TurboCraftClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

/**
 * Real, measurable FPS work — no fake counters.
 *
 * <p>Minecraft's cost on a phone is dominated by, in this order:
 * <ol>
 *   <li><b>Particles</b> — every particle is a separate draw + tick. TNT/explosion tests spawn
 *       thousands; this is the single biggest cause of the 0-1 FPS screenshots.</li>
 *   <li><b>Render distance</b> — chunk meshes and chunk rebuilds.</li>
 *   <li><b>Simulation distance</b> — entity ticking.</li>
 *   <li><b>Clouds / smooth lighting / mipmaps</b> — per-frame GPU and texture memory.</li>
 * </ol>
 *
 * <p>Everything here changes <b>vanilla game options</b> through their public API, so it is exactly
 * what the player could do by hand in Video Settings — it just takes one click, and it survives
 * because the options file is written afterwards.
 *
 * <p><b>Honest note:</b> a client cannot make a GPU render faster than it can. It can only stop the
 * game from asking for work that is invisible anyway. Anyone showing 3000+ FPS on a phone is either
 * measuring GUI-only frames, skipping frames, or inflating the number — at 0.25 ms/frame the GPU
 * would have to be an order of magnitude faster than it is.
 */
public final class PerformanceProfile {

    /** Immutable view of the settings that actually move the needle. */
    public record Snapshot(int viewDistance, int simulationDistance, String graphics, String particles,
                           String clouds, int biomeBlend, int mipmaps) {
    }

    private PerformanceProfile() {
    }

    public static Snapshot current(MinecraftClient client) {
        return new Snapshot(
                client.options.getViewDistance().getValue(),
                client.options.getSimulationDistance().getValue(),
                client.options.getGraphicsMode().getValue().name(),
                client.options.getParticles().getValue().name(),
                client.options.getCloudRenderMode().getValue().name(),
                client.options.getBiomeBlendRadius().getValue(),
                client.options.getMipmapLevels().getValue()
        );
    }

    /**
     * Maximum FPS: everything that costs frames and is barely visible gets turned off.
     * Keeps the render distance at 6 — below that, chunks visibly pop in.
     */
    public static void applyPotato(MinecraftClient client) {
        set(client, 6, 5, GraphicsMode.FAST, ParticlesMode.MINIMAL,
                CloudRenderMode.OFF, 0, 0);
    }

    /**
     * Middle ground for decent phones: fewer particles, no clouds, fast graphics, mipmaps off,
     * but a playable render distance.
     */
    public static void applyBalanced(MinecraftClient client) {
        set(client, 8, 6, GraphicsMode.FAST, ParticlesMode.DECREASED,
                CloudRenderMode.OFF, 0, 0);
    }

    /** Back to the launcher's own values, in case a user wants the pretty version. */
    public static void applyVanilla(MinecraftClient client) {
        set(client, 12, 10, GraphicsMode.FANCY, ParticlesMode.ALL,
                CloudRenderMode.FANCY, 2, 4);
    }

    private static void set(MinecraftClient client, int viewDistance, int simulationDistance,
                            GraphicsMode graphics, ParticlesMode particles,
                            CloudRenderMode clouds, int biomeBlend, int mipmaps) {
        try {
            client.options.getViewDistance().setValue(viewDistance);
            client.options.getSimulationDistance().setValue(simulationDistance);
            client.options.getGraphicsMode().setValue(graphics);
            client.options.getParticles().setValue(particles);
            client.options.getCloudRenderMode().setValue(clouds);
            client.options.getBiomeBlendRadius().setValue(biomeBlend);
            client.options.getMipmapLevels().setValue(mipmaps);
            client.options.write();

            // render distance / mipmaps only take effect after the world renderer rebuilds
            if (client.worldRenderer != null) {
                client.worldRenderer.reload();
            }
            TurboCraftClient.LOGGER.info(
                    "[TurboCraft] Applied profile: view={} sim={} graphics={} particles={} clouds={}",
                    viewDistance, simulationDistance, graphics, particles, clouds);
        } catch (Throwable t) {
            TurboCraftClient.LOGGER.warn("[TurboCraft] Could not apply performance profile", t);
        }
    }

    /**
     * Rough guidance shown in the menu. These are the launcher-side settings a mod cannot change,
     * so the user has to fix them in the launcher.
     */
    public static String[] launcherAdvice(long maxMb, long usedMb, int rendererId) {
        String ram = maxMb > 0
                ? String.format("RAM: %d MB allocated", maxMb)
                : "RAM: check launcher allocation";
        String ramHint = maxMb > 4096
                ? "-> too much! 2500-3000 MB is faster (bigger heap = longer GC pauses)"
                : (maxMb > 0 && maxMb < 1500 ? "-> too little, use 2500-3000 MB" : "-> good");

        return new String[]{
                ram,
                ramHint,
                "Renderer: " + (rendererId >= 0 ? "ok" : "set MobileGlues (Snapdragon) or GL4ES (Mali)"),
                "",
                "At 6 GB the JVM scans a huge heap on every GC;",
                "that is what produces 0-1 FPS spikes in TNT tests,",
                "not the client mod.",
        };
    }
}
