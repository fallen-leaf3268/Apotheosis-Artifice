package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;

import net.minecraft.util.Mth;

@Mixin(ApothEnchantmentMenu.TableStats.class)
public class TableStatsClampMixin {

    @Redirect(
        method = "<init>(FFFFILjava/util/Set;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 0))
    private float artifice_clampEterna(float value, float min, float max) {
        return Mth.clamp(value, min, Math.max(max, ApotheosisConfig.MAX_ETERNA.get()));
    }

    @Redirect(
        method = "<init>(FFFFILjava/util/Set;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 1))
    private float artifice_clampQuanta(float value, float min, float max) {
        return Mth.clamp(value, min, Math.max(max, ApotheosisConfig.MAX_QUANTA.get()));
    }

    @Redirect(
        method = "<init>(FFFFILjava/util/Set;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 2))
    private float artifice_clampArcana(float value, float min, float max) {
        return Mth.clamp(value, min, Math.max(max, ApotheosisConfig.MAX_ARCANA.get()));
    }
}
