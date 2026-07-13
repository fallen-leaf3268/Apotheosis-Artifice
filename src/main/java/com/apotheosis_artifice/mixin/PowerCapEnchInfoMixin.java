package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.ench.EnchantmentInfo;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;

@Mixin(EnchantmentInfo.class)
public class PowerCapEnchInfoMixin {

    @Redirect(
        method = "lambda$defaultMax$0",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry;getAbsoluteMaxEterna()F",
            remap = false),
        remap = false)
    private static float artifice_powerCap() {
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
    }
}
