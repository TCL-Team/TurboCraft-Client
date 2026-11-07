package com.turbocraft.client.core;

/**
 * Permanent links, compiled into the mod.
 *
 * <p>These are the defaults. {@code config/turbocraft.json} can override them
 * ({@code discordUrl} / {@code storeUrl}), and a blank config value falls back to these — so the
 * Discord button always works, even if the config file is deleted or reset by the launcher.
 *
 * <p><b>To change the Discord invite, edit the one line below and push.</b> Nothing else needs to
 * change: title screen, in-game menu and the Links tab all read from here.
 */
public final class Links {

    /** ← EDIT THIS LINE to your own server invite. */
    public static final String DISCORD = "https://discord.gg/turbocraft";

    /** Store / cosmetics checkout. Empty string = button shows a "not set up yet" hint. */
    public static final String STORE = "";

    /** Project page, shown in the About tab. */
    public static final String GITHUB = "https://github.com/TCL-Team/TurboCraft-Client";

    private Links() {
    }

    /** Returns {@code override} when it is a real URL, otherwise the compiled-in default. */
    public static String pick(String override, String fallback) {
        if (override != null && !override.isBlank()) return override;
        return fallback;
    }

    public static boolean isSet(String url) {
        return url != null && !url.isBlank();
    }
}
