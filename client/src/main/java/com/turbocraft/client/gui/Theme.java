package com.turbocraft.client.gui;

/**
 * TurboCraft colour palette — neon green on black.
 *
 * <p>One place for colours so the title screen, HUD and menus never drift apart. Kept as plain ints
 * (ARGB) because that is what {@code DrawContext#fill} takes; no allocations, no config lookups per frame.
 */
public final class Theme {
    /** Primary neon green accent. */
    public static final int ACCENT = 0xFF39FF14;
    public static final int ACCENT_BRIGHT = 0xFF7CFF6B;
    public static final int ACCENT_DIM = 0x5539FF14;
    public static final int ACCENT_FAINT = 0x2239FF14;

    /** Black glass panels. */
    public static final int PANEL = 0xD9050A05;
    public static final int PANEL_HOVER = 0xF00B1409;
    public static final int PANEL_SOLID = 0xF0050A05;
    public static final int BACKDROP = 0x99000000;

    public static final int TEXT = 0xFFE9F7E6;
    public static final int TEXT_DIM = 0xFF8FA394;
    public static final int TEXT_FAINT = 0xFF4E5A50;
    public static final int TEXT_ON_ACCENT = 0xFF04120A;

    public static final int DANGER = 0xFF7A1F16;
    public static final int DANGER_HOVER = 0xFFA82C1F;
    public static final int DANGER_TEXT = 0xFFFFB4A6;

    public static final int BORDER = 0x6639FF14;

    private Theme() {
    }
}
