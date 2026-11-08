package com.turbocraft.client.mixin;

import com.turbocraft.client.perf.ParticleBudget;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies {@link ParticleBudget} to every particle spawn.
 *
 * <p>Yarn 1.20.1 has exactly one spawn entry point that matters:
 * {@code addParticle(ParticleEffect, double, double, double, double, double, double) -> Particle}.
 * (There is no 3-coordinate overload in this version — that is what broke the previous build.)
 *
 * <p>{@code require = 0}: if a future 1.20.x patch renames this, the particle cap silently turns off
 * instead of crashing the game. A performance feature must never be a crash source.
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void turbocraft$budget(ParticleEffect parameters, double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ,
                                   CallbackInfoReturnable<Particle> cir) {
        if (!ParticleBudget.allowSpawn()) cir.setReturnValue(null);
    }
}
