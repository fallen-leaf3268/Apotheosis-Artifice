package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.ench.EnchModule;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;

@Mixin(EnchModule.class)
public class PowerCapEnchModuleMixin {

    @Redirect(
        method = "getDefaultMax",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry;getAbsoluteMaxEterna()F",
            remap = false),
        remap = false)
    private static float artifice_powerCap() {
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
    }
}
