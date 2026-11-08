package com.turbocraft.client.gui;

/**
 * TurboCraft colour palette — neon on black, switchable at runtime.
 *
 * <p>The accent is <b>not</b> a compile-time constant: the Cosmetics tab swaps the whole palette
 * (Neon Green / Cyan Ice / Blood Red / Void Purple / Gold) and every screen picks it up on the next
 * frame, with no restart. Fixed neutrals (panels, text) stay constant so contrast never breaks.
 */
public final class Theme {

    /** One selectable accent set. */
    public record Palette(String id, String name, int accent, int accentBright, int accentDark) {
    }

    public static final Palette NEON = new Palette("neon", "Neon Green", 0xFF39FF14, 0xFF7CFF6B, 0xFF04120A);
    public static final Palette CYAN = new Palette("cyan", "Cyan Ice", 0xFF00E5FF, 0xFF7BF0FF, 0xFF00161C);
    public static final Palette BLOOD = new Palette("blood", "Blood Red", 0xFFFF3B30, 0xFFFF8A80, 0xFF1C0503);
    public static final Palette VOID = new Palette("void", "Void Purple", 0xFFB14CFF, 0xFFD69BFF, 0xFF120320);
    public static final Palette GOLD = new Palette("gold", "Gold", 0xFFFFC436, 0xFFFFE08A, 0xFF1C1400);

    public static final Palette[] PALETTES = {NEON, CYAN, BLOOD, VOID, GOLD};

    private static Palette active = NEON;

    // ---- fixed neutrals ----
    public static final int PANEL = 0xD9050A05;
    public static final int PANEL_HOVER = 0xF00B1409;
    public static final int PANEL_SOLID = 0xF0020403;
    public static final int BACKDROP = 0xB0000000;

    public static final int TEXT = 0xFFE9F7E6;
    public static final int TEXT_DIM = 0xFF8FA394;
    public static final int TEXT_FAINT = 0xFF4E5A50;

    public static final int DANGER = 0xFF7A1F16;
    public static final int DANGER_HOVER = 0xFFA82C1F;
    public static final int DANGER_TEXT = 0xFFFFB4A6;

    private Theme() {
    }

    public static Palette active() {
        return active;
    }

    public static void set(Palette palette) {
        if (palette != null) active = palette;
    }

    /** Resolves a stored config id; unknown ids fall back to neon. */
    public static void setById(String id) {
        if (id == null) return;
        for (Palette p : PALETTES) {
            if (p.id().equalsIgnoreCase(id)) {
                active = p;
                return;
            }
        }
        active = NEON;
    }

    // ---- accent accessors (called per frame, so changes apply instantly) ----

    public static int accent() {
        return active.accent();
    }

    public static int accentBright() {
        return active.accentBright();
    }

    public static int textOnAccent() {
        return active.accentDark();
    }

    public static int accentDim() {
        return withAlpha(active.accent(), 0x55);
    }

    public static int accentFaint() {
        return withAlpha(active.accent(), 0x22);
    }

    public static int border() {
        return withAlpha(active.accent(), 0x66);
    }

    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
