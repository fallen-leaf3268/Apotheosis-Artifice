package com.apotheosis_artifice.mixin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.attributeslib.api.ALCombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 根据 ApotheosisConfig 的开关，控制是否应用 Apothic Attributes 的护甲/保护公式修改。
 * 关闭时：穿透/护甲削减仍然生效，仅伤害减免计算降回原版公式。
 */
@Mixin(value = ALCombatRules.class, remap = false)
public class ALCombatRulesMixin {

    @Unique
    private static final MethodHandle apotheosis_artifice_armorFormula = apotheosis_artifice_resolveArmorFormula();

    @Group(name = "apotheosis_artifice_armorFormula", min = 1, max = 1)
    @Redirect(method = "getDamageAfterArmor", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/attributeslib/api/ALCombatRules;getArmorDamageReduction(FF)F"), require = 0, expect = 0, allow = 1)
    private static float apotheosis_artifice_toggleArmorFormula(float damage, float armor,
        LivingEntity target, DamageSource source, float amount, float originalArmor, float toughness) throws Throwable {
        return apotheosis_artifice_applyArmorFormula(damage, armor, toughness);
    }

    @Group(name = "apotheosis_artifice_armorFormula", min = 1, max = 1)
    @Redirect(method = "getDamageAfterArmor", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/attributeslib/api/ALCombatRules;getArmorDamageReduction(FFF)F"), require = 0, expect = 0, allow = 1)
    private static float apotheosis_artifice_toggleArmorFormulaModern(float damage, float armor, float toughness) throws Throwable {
        return apotheosis_artifice_applyArmorFormula(damage, armor, toughness);
    }

    @Unique
    private static float apotheosis_artifice_applyArmorFormula(float damage, float armor, float toughness) throws Throwable {
        if (ApotheosisConfig.USE_APOTH_ARMOR_FORMULA.get()) {
            return (float) apotheosis_artifice_armorFormula.invokeExact(damage, armor, toughness);
        }
        float reduction = Math.min(20, Math.max(armor / 5, armor - damage / (2 + toughness / 4)));
        return 1 - reduction / 25;
    }

    @Unique
    private static MethodHandle apotheosis_artifice_resolveArmorFormula() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            try {
                return lookup.findStatic(ALCombatRules.class, "getArmorDamageReduction",
                    MethodType.methodType(float.class, float.class, float.class, float.class));
            } catch (NoSuchMethodException legacy) {
                return MethodHandles.dropArguments(lookup.findStatic(ALCombatRules.class, "getArmorDamageReduction",
                    MethodType.methodType(float.class, float.class, float.class)), 2, float.class);
            }
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    @Redirect(method = "getDamageAfterProtection", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/attributeslib/api/ALCombatRules;getProtDamageReduction(F)F"), require = 1)
    private static float apotheosis_artifice_toggleProtFormula(float protPoints) {
        if (ApotheosisConfig.USE_APOTH_PROT_FORMULA.get()) return ALCombatRules.getProtDamageReduction(protPoints);
        return 1 - Math.min(protPoints * 0.04F, 0.8F);
    }
}
