package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Constant;

import com.apotheosis_artifice.ApotheosisConfig;
import com.apotheosis_artifice.enchant.RavenEnchantScreen;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantScreen;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;

@Mixin(ApothEnchantScreen.class)
public class EnchantScreenEternaMaxMixin {

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry;getAbsoluteMaxEterna()F",
            ordinal = 0,
            remap = false))
    private float artifice_eternaTooltipMax() {
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
    }

    // 原版附魔台进度条：位阶条按上限缩放；渡鸦台已自行预缩放，返回原值避免双重缩放
    @Redirect(
        method = "renderBg",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry;getAbsoluteMaxEterna()F",
            ordinal = 0,
            remap = false))
    private float artifice_eternaBarMax() {
        if ((Object) this instanceof RavenEnchantScreen) return EnchantingStatRegistry.getAbsoluteMaxEterna();
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
    }

    @ModifyConstant(method = "renderBg", constant = @Constant(floatValue = 100f, ordinal = 0))
    private float artifice_quantaBarMax(float orig) {
        if ((Object) this instanceof RavenEnchantScreen) return orig;
        return ApotheosisConfig.MAX_QUANTA.get();
    }

    @ModifyConstant(method = "renderBg", constant = @Constant(floatValue = 100f, ordinal = 1))
    private float artifice_arcanaBarMax(float orig) {
        if ((Object) this instanceof RavenEnchantScreen) return orig;
        return ApotheosisConfig.MAX_ARCANA.get();
    }

    // 主界面「威力范围」tooltip 的 *4 天花板（render 内 getAbsoluteMaxEterna 第2、3次）
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry;getAbsoluteMaxEterna()F",
            ordinal = 1,
            remap = false))
    private float artifice_powerMinCap() {
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry;getAbsoluteMaxEterna()F",
            ordinal = 2,
            remap = false))
    private float artifice_powerMaxCap() {
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
    }
}
