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
 * <p>Both overloads are hooked; {@code require = 0} keeps the game running if a future 1.20.x patch
 * changes one of the descriptors (the cap silently stops working instead of crashing the game).
 * No gameplay effect: this only drops visual sparks, never entity logic or explosion damage.
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
    private void turbocraft$budgetFullShort(ParticleEffect parameters, double x, double y, double z,
                                            CallbackInfoReturnable<Particle> cir) {
        if (!ParticleBudget.allowSpawn()) cir.setReturnValue(null);
    }
}
