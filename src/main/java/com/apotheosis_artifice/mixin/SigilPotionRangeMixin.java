package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.PotionAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.world.item.ItemStack;

@Mixin(value = PotionAffix.class, remap = false)
public abstract class SigilPotionRangeMixin {
    @ModifyConstant(method = "getAugmentingText", constant = @Constant(floatValue = 1F, ordinal = 0), require = 1, allow = 1)
    private float artifice$rangeMaximum(float original, ItemStack stack, LootRarity rarity, float level) {
        return Apotheosis.enableAdventure && Float.isFinite(level) ? Math.max(original, level) : original;
    }
}
