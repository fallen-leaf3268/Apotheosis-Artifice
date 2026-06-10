package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.attributeslib.api.ALCombatRules;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 根据 ApotheosisConfig 的开关，控制是否应用 Apothic Attributes 的护甲/保护公式修改。
 * 关闭时：穿透/护甲削减仍然生效，仅伤害减免计算降回原版公式。
 */
@Mixin(value = ALCombatRules.class, remap = false)
public class ALCombatRulesMixin {

    @Inject(method = "getDamageAfterArmor", at = @At("HEAD"), cancellable = true)
    private static void apotheosis_artifice_toggleArmorFormula(LivingEntity target, DamageSource src, float amount, float armor, float toughness, CallbackInfoReturnable<Float> cir) {
        if (!ApotheosisConfig.USE_APOTH_ARMOR_FORMULA.get()) {
            // 保留 Armor Shred / Armor Pierce 计算（否则这两个属性会失效）
            if (src.getEntity() instanceof LivingEntity attacker) {
                float shred = (float) attacker.getAttributeValue(ALObjects.Attributes.ARMOR_SHRED.get());
                float bypassResist = Math.min(toughness * 0.02F, 0.6F);
                if (shred > 0.001F) {
                    shred *= 1 - bypassResist;
                    armor *= 1 - shred;
                }
                float pierce = (float) attacker.getAttributeValue(ALObjects.Attributes.ARMOR_PIERCE.get());
                if (pierce > 0.001F) {
                    pierce *= 1 - bypassResist;
                    armor -= pierce;
                }
            }
            // 原版护甲公式: DR = clamp(armor - dmg / (2 + tough/4), armor/5, 20) / 25
            if (armor <= 0) {
                cir.setReturnValue(amount);
                return;
            }
            float f = Math.min(20, Math.max(armor / 5, armor - amount / (2 + toughness / 4)));
            cir.setReturnValue(amount * (1 - f / 25));
        }
    }

    @Inject(method = "getDamageAfterProtection", at = @At("HEAD"), cancellable = true)
    private static void apotheosis_artifice_toggleProtFormula(LivingEntity target, DamageSource src, float amount, float protPoints, CallbackInfoReturnable<Float> cir) {
        if (!ApotheosisConfig.USE_APOTH_PROT_FORMULA.get()) {
            // 保留 Prot Shred / Prot Pierce 计算
            if (src.getEntity() instanceof LivingEntity attacker) {
                float shred = (float) attacker.getAttributeValue(ALObjects.Attributes.PROT_SHRED.get());
                if (shred > 0.001F) {
                    protPoints *= 1 - shred;
                }
                float pierce = (float) attacker.getAttributeValue(ALObjects.Attributes.PROT_PIERCE.get());
                if (pierce > 0.001F) {
                    protPoints -= pierce;
                }
            }
            // 原版保护公式: 每点减伤 4%，上限 80%
            if (protPoints <= 0) {
                cir.setReturnValue(amount);
                return;
            }
            cir.setReturnValue(amount * (1 - Math.min(protPoints * 0.04F, 0.8F)));
        }
    }
}
