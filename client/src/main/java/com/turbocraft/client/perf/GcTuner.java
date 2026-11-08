package com.turbocraft.client.perf;

import com.turbocraft.client.TurboCraftClient;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Reports what the JVM is actually doing, and what the launcher got wrong.
 *
 * <p>A client mod <b>cannot</b> change {@code -Xmx} or the GC algorithm at runtime — those are
 * launcher arguments. What it can do is measure, so the user stops guessing. On Android the two
 * classic mistakes are:
 * <ul>
 *   <li><b>Heap too large</b> (4-6 GB on a phone): every GC has to scan a huge old generation, so
 *       pauses get longer, not shorter. 2-3 GB is the sweet spot for 1.20.1.</li>
 *   <li><b>Heap too small</b> (&lt; 1.5 GB): constant full GCs, the game freezes every few seconds.</li>
 * </ul>
 *
 * <p>Everything here is read-only and safe to call every second.
 */
public final class GcTuner {

    /** Snapshot of GC + heap state for the PERF tab and /turbocraft diag. */
    public record Stats(long maxMb, long usedMb, long gcCount, long gcTimeMs, String gcNames) {
        public long avgPauseMs() {
            return gcCount <= 0 ? 0 : gcTimeMs / gcCount;
        }

        public int heapPercent() {
            return maxMb <= 0 ? 0 : (int) (usedMb * 100 / maxMb);
        }
    }

    private GcTuner() {
    }

    public static Stats snapshot() {
        Runtime rt = Runtime.getRuntime();
        long maxMb = rt.maxMemory() / (1024L * 1024L);
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);

        long count = 0;
        long time = 0;
        StringBuilder names = new StringBuilder();
        try {
            List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean bean : beans) {
                if (bean.getCollectionCount() > 0) count += bean.getCollectionCount();
                if (bean.getCollectionTime() > 0) time += bean.getCollectionTime();
                if (names.length() > 0) names.append(", ");
                names.append(bean.getName());
            }
        } catch (Throwable ignored) {
            // Android's JVM sometimes hides the MXBeans; the heap numbers above still work
        }
        return new Stats(maxMb, usedMb, count, time, names.toString());
    }

    /**
     * Human advice for the current heap, or {@code null} when it looks fine.
     * The value comes from {@code -Xmx}, so it is a launcher setting, not something the mod sets.
     */
    public static String advice(Stats stats) {
        long max = stats.maxMb();
        if (max <= 0) return null;
        if (max > 4096) {
            return String.format(
                    "Heap is %d MB — too big for a phone. 2048-3072 MB gives shorter GC pauses.", max);
        }
        if (max < 1200) {
            return String.format(
                    "Heap is %d MB — too small, expect freezes. Use 2048-3072 MB.", max);
        }
        if (stats.avgPauseMs() > 120) {
            return String.format(
                    "Avg GC pause %d ms — stutter source. Lower render distance or free RAM.", stats.avgPauseMs());
        }
        return null;
    }

    /** JVM args this mod recommends; shown in the PERF tab so users can paste them in the launcher. */
    public static String suggestedLauncherArgs() {
        boolean isAndroid = FabricLoader.getInstance().getModContainer("minecraft").isPresent()
                && System.getProperty("java.vm.vendor", "").toLowerCase().contains("android")
                || System.getProperty("os.name", "").toLowerCase().contains("android");
        if (isAndroid) {
            // SerialGC starts faster and has smaller pauses than G1 on small phone heaps
            return "-Xmx2048m -XX:+UseSerialGC -XX:MaxGCPauseMillis=30";
        }
        return "-Xmx3072m -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:G1NewSizePercent=20";
    }

    public static void logOnce() {
        Stats stats = snapshot();
        TurboCraftClient.LOGGER.info(
                "[TurboCraft] heap={}MB/{}MB gc={} avgPause={}ms collectors=[{}]",
                stats.usedMb(), stats.maxMb(), stats.gcCount(), stats.avgPauseMs(), stats.gcNames());
        String advice = advice(stats);
        if (advice != null) TurboCraftClient.LOGGER.warn("[TurboCraft] {}", advice);
    }
}
