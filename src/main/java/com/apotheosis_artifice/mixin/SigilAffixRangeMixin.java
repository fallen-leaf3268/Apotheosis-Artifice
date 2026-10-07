package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.CleavingAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.DamageReductionAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.EnlightenedAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.ExecutingAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.FestiveAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.PsychicAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.RadialAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.SpectralShotAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.ThunderstruckAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.world.item.ItemStack;

@Mixin(value = AttributeAffix.class, remap = false)
public abstract class SigilAffixRangeMixin {
    @Redirect(method = "getAugmentingText", at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/placebo/util/StepFunction;get(F)F", ordinal = 2), require = 1, allow = 1)
    private float artifice$checkRangeMaximum(StepFunction function, float original, ItemStack stack, LootRarity rarity, float level) {
        return function.get(Apotheosis.enableAdventure && Float.isFinite(level) ? Math.max(original, level) : original);
    }

    @Redirect(method = "getAugmentingText", at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/placebo/util/StepFunction;get(F)F", ordinal = 4), require = 1, allow = 1)
    private float artifice$showRangeMaximum(StepFunction function, float original, ItemStack stack, LootRarity rarity, float level) {
        return function.get(Apotheosis.enableAdventure && Float.isFinite(level) ? Math.max(original, level) : original);
    }

    @Mixin(value = {DamageReductionAffix.class, ExecutingAffix.class, FestiveAffix.class, PsychicAffix.class,
        SpectralShotAffix.class, ThunderstruckAffix.class}, remap = false)
    public abstract static class FloatRanges {
        @ModifyConstant(method = "getAugmentingText", constant = @Constant(floatValue = 1F, ordinal = 0), require = 1, allow = 1)
        private float artifice$rangeMaximum(float original, ItemStack stack, LootRarity rarity, float level) {
            return Apotheosis.enableAdventure && Float.isFinite(level) ? Math.max(original, level) : original;
        }
    }

    @Mixin(value = EnlightenedAffix.class, remap = false)
    public abstract static class IntegerRanges {
        @ModifyConstant(method = "getAugmentingText", constant = @Constant(floatValue = 1F, ordinal = 0), require = 1, allow = 1)
        private float artifice$rangeMaximum(float original, ItemStack stack, LootRarity rarity, float level) {
            return Apotheosis.enableAdventure && Float.isFinite(level) ? Math.max(original, level) : original;
        }
    }

    @Mixin(value = RadialAffix.class, remap = false)
    public abstract static class RadialRanges {
        @ModifyConstant(method = "getAugmentingText", constant = @Constant(floatValue = 1F, ordinal = 0), require = 1, allow = 1)
        private float artifice$rangeMaximum(float original, ItemStack stack, LootRarity rarity, float level) {
            return Apotheosis.enableAdventure && Float.isFinite(level) ? Math.max(original, level) : original;
        }
    }

    @Mixin(value = CleavingAffix.class, remap = false)
    public abstract static class CleavingRanges {
        @ModifyConstant(method = "getAugmentingText", constant = @Constant(floatValue = 1F, ordinal = 0), require = 1, allow = 1)
        private float artifice$chanceMaximum(float original, ItemStack stack, LootRarity rarity, float level) {
            return Apotheosis.enableAdventure && Float.isFinite(level) ? Math.max(original, level) : original;
        }

        @ModifyConstant(method = "getAugmentingText", constant = @Constant(floatValue = 1F, ordinal = 1), require = 1, allow = 1)
        private float artifice$targetsMaximum(float original, ItemStack stack, LootRarity rarity, float level) {
            return Apotheosis.enableAdventure && Float.isFinite(level) ? Math.max(original, level) : original;
        }
    }
}
