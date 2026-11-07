package com.turbocraft.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.turbocraft.client.TurboCraftClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON config at {@code config/turbocraft.json}.
 *
 * <p>Deliberately hand-rolled instead of using Cloth Config: on Android every extra dependency is
 * one more thing that can break inside the Pojav/Amethyst JVM, and this file is tiny.
 */
public class TurboCraftConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("turbocraft.json");

    private static TurboCraftConfig instance;

    // ---- HUD toggles ----
    public boolean hudEnabled = true;
    public boolean showFps = true;
    public boolean showCoords = true;
    public boolean showKeystrokes = true;
    public boolean showCps = true;
    public boolean showPing = true;
    public boolean showGraph = true;   // frame-time graph + 1% low

    // ---- HUD placement ----
    public int hudX = 4;
    public int hudY = 4;
    public float hudScale = 1.0f;
    public boolean hudBackground = true;

    // ---- Title screen ----
    public boolean customTitleScreen = true;
    /** Discord invite opened by the title screen button. Change it in config/turbocraft.json. */
    public String discordUrl = "https://discord.gg/turbocraft";

    // ---- Misc ----
    public boolean chatPrefix = true;

    public static TurboCraftConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                instance = GSON.fromJson(reader, TurboCraftConfig.class);
            } catch (Exception e) {
                TurboCraftClient.LOGGER.warn("[TurboCraft] Could not read config, using defaults", e);
            }
        }
        if (instance == null) instance = new TurboCraftConfig();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException e) {
            TurboCraftClient.LOGGER.warn("[TurboCraft] Could not save config", e);
        }
    }
}
