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
 * <p>Yarn 1.20.1 has two overloads; the one every caller funnels through is
 * {@code addParticle(ParticleEffect, double x, double y, double z, double vx, double vy, double vz)}
 * returning {@code Particle}. A second 3-coordinate overload is also hooked for safety.
 *
 * <p>{@code require = 0} means a future rename disables the cap instead of crashing the game —
 * the mod's contract is that a missing particle cap must never take the client down.
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void turbocraft$budgetFull(ParticleEffect parameters, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ,
                                       CallbackInfoReturnable<Particle> cir) {
        if (!ParticleBudget.allowSpawn()) cir.setReturnValue(null);
    }

    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void turbocraft$budgetShort(ParticleEffect parameters, double x, double y, double z,
                                        CallbackInfoReturnable<Particle> cir) {
        if (!ParticleBudget.allowSpawn()) cir.setReturnValue(null);
    }
}
