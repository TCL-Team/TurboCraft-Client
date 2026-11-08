package com.turbocraft.client.mixin;

import com.turbocraft.client.perf.ParticleBudget;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies {@link ParticleBudget} to every particle spawn.
 *
 * <p>1.20.1 signatures: {@code addParticle(ParticleEffect, double, double, double)} is {@code void},
 * and the 6-double overload returns {@code Particle}. Both are hooked; every injector uses
 * {@code require = 0} together with a matching {@code expect = 0} so a Yarn rename disables the cap
 * instead of crashing the game on someone's phone.
 *
 * <p>No gameplay effect: only visual sparks are dropped, never entity logic or explosion damage.
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            expect = 0
    )
    private void turbocraft$budgetShort(ParticleEffect parameters, double x, double y, double z,
                                        CallbackInfo ci) {
        if (!ParticleBudget.allowSpawn()) ci.cancel();
    }

    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            expect = 0
    )
    private void turbocraft$budgetFull(ParticleEffect parameters, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ,
                                       CallbackInfo ci) {
        if (!ParticleBudget.allowSpawn()) ci.cancel();
    }
}
