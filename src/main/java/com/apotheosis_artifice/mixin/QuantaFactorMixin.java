package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;

@Mixin(RealEnchantmentHelper.class)
public class QuantaFactorMixin {

    @Inject(method = "getQuantaFactor", at = @At("RETURN"), cancellable = true, remap = false)
    private static void artifice_clampQuantaFactor(net.minecraft.util.RandomSource rand, float quanta, float rectification, CallbackInfoReturnable<Float> cir) {
        float v = cir.getReturnValueF();
        if (v < -0.9F) cir.setReturnValue(-0.9F);
    }
}
