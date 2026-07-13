package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import dev.shadowsoffire.apotheosis.util.CommonTooltipUtil;

@Mixin(CommonTooltipUtil.class)
public class CommonTooltipUtilMixin {

    @Redirect(
        method = "appendTableStats",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry;getAbsoluteMaxEterna()F",
            ordinal = 0,
            remap = false),
        remap = false)
    private static float artifice_tableEternaMax() {
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
    }

    @Redirect(
        method = "appendTableStats",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F", ordinal = 0),
        remap = false)
    private static float artifice_tableQuantaNoCap(float a, float b) {
        return b;
    }

    @Redirect(
        method = "appendTableStats",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F", ordinal = 1),
        remap = false)
    private static float artifice_tableArcanaNoCap(float a, float b) {
        return b;
    }
}
