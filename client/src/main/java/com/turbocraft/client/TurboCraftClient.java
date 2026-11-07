package com.turbocraft.client;

import com.turbocraft.client.config.TurboCraftConfig;
import com.turbocraft.client.event.InputTracker;
import com.turbocraft.client.gui.ClientMenuScreen;
import com.turbocraft.client.hud.HudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TurboCraft Client entrypoint.
 *
 * <p>Everything in this mod is client-side only: it never touches the server, so it is safe to
 * join any vanilla Java server (including Hypixel, which runs 1.8.9 — see docs/client-strategy.md).
 */
public class TurboCraftClient implements ClientModInitializer {
    public static final String MOD_ID = "turbocraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("TurboCraft");

    /** Right Shift by default — same muscle memory as Lunar / Feather. */
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        TurboCraftConfig.load();
        InputTracker.init();
        HudRenderer.init();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.turbocraft.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.categories.turbocraft"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            InputTracker.onClientTick();
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClientMenuScreen(null));
                }
            }
        });

        LOGGER.info("[TurboCraft] Client loaded — press Right Shift in-game for the menu.");
    }
}
