package com.apotheosis_artifice.mixin;

import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.apotheosis_artifice.AffixTypes;

import dev.shadowsoffire.apotheosis.adventure.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;

@Mixin(value = AttributeAffix.class, remap = false)
public abstract class AttributeAffixAccessor implements AffixTypes {

    @Accessor("types")
    @Override
    public abstract Set<LootCategory> curiosforge_getTypes();

    @Accessor("modifiers")
    public abstract Map<LootRarity, StepFunction> curiosforge_getModifiers();
}
