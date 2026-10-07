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

    /**
     * defaultMin 的 lambda：计算某等级所需最低威力。
     * 对 extra_level 标签内、且超过原始最高等级的附魔，改用线性公式：
     *   威力 = minCost(原始上限) + 步长 × (目标级 - 原始上限)
     * 其余附魔放行原版 ^1.6 曲线。
     */
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
