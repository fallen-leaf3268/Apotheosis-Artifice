package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.ench.EnchantmentInfo;
import net.minecraft.world.item.enchantment.Enchantment;

@Mixin(EnchantmentInfo.class)
public class DefaultMinCurveMixin {

    @Inject(method = "lambda$defaultMin$1", at = @At("HEAD"), cancellable = true, remap = false)
    private static void artifice_linearMinPower(Enchantment ench, int level, CallbackInfoReturnable<Integer> cir) {
        int baseMax = ench.getMaxLevel();
        if (level <= baseMax || level <= 1) return;
        if (!ApotheosisConfig.canExtendEnchantment(ench)) return;
        int step = ApotheosisConfig.getExtraLevelPowerPerLevel();
        int power = ench.getMinCost(baseMax) + step * (level - baseMax);
        cir.setReturnValue(power);
    }
}
