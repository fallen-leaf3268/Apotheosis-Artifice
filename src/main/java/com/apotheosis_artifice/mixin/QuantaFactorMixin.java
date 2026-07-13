package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;

@Mixin(RealEnchantmentHelper.class)
public class QuantaFactorMixin {

    /**
     * 原版 getQuantaFactor 返回 quanta*factor/100，factor∈[-1,1]。
     * quanta>100 时结果可低于 -1，使 selectEnchantment 的倍率 (1+结果) 变负，把威力打到 1。
     * 下限保护到 -0.9，保证倍率 ≥ 0.1，高 quanta 只放大正向波动、不再归零。
     */
    @Inject(method = "getQuantaFactor", at = @At("RETURN"), cancellable = true, remap = false)
    private static void artifice_clampQuantaFactor(net.minecraft.util.RandomSource rand, float quanta, float rectification, CallbackInfoReturnable<Float> cir) {
        float v = cir.getReturnValueF();
        if (v < -0.9F) cir.setReturnValue(-0.9F);
    }
}
