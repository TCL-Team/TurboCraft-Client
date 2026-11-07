package com.turbocraft.client.mixin;

import com.turbocraft.client.config.TurboCraftConfig;
import com.turbocraft.client.gui.TurboCraftTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swaps vanilla's title screen for {@link TurboCraftTitleScreen}.
 *
 * <p>Intercepting {@code setScreen} instead of patching {@code TitleScreen#init} is deliberate:
 * vanilla's title screen builds its buttons (including the Realms notification and the splash
 * animation) inside {@code init}, and every Minecraft update shuffles that code. Replacing the screen
 * once, at the door, survives those changes.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    private Screen currentScreen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void turbocraft$customTitleScreen(Screen newScreen, CallbackInfo ci) {
        if (!(newScreen instanceof TitleScreen)) return;
        if (!TurboCraftConfig.get().customTitleScreen) return;

        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.getWindow() == null) return;

        TurboCraftTitleScreen replacement = new TurboCraftTitleScreen();
        this.currentScreen = replacement;
        replacement.init(client, client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight());
        ci.cancel();
    }
}
