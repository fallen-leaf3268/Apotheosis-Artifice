package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.ench.table.EnchantingInfoScreen;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;

@Mixin(EnchantingInfoScreen.class)
public class EnchantingInfoScreenMixin {

    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry;getAbsoluteMaxEterna()F",
            remap = false))
    private float artifice_infoPowerCap() {
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
    }
}
