package com.turbocraft.client.mixin;

import com.turbocraft.client.event.InputTracker;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds physical clicks into {@link InputTracker} for the CPS counter.
 *
 * <p>{@code require = 0} is intentional: if a future Yarn build renames this method, the mod keeps
 * loading (CPS just stays at 0) instead of crashing the game on a phone where reading logs is hard.
 */
@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), require = 0)
    private void turbocraft$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        // action 1 == GLFW_PRESS
        if (action == 1) {
            InputTracker.onMouseButton(button);
        }
    }
}
