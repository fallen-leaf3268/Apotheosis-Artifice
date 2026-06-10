package com.apotheosis_artifice.mixin;

import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.apotheosis_artifice.AffixTypes;

import dev.shadowsoffire.apotheosis.adventure.affix.effect.DamageReductionAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;

@Mixin(value = DamageReductionAffix.class, remap = false)
public abstract class DamageReductionAffixAccessor implements AffixTypes {

    @Accessor("types")
    @Override
    public abstract Set<LootCategory> curiosforge_getTypes();

    @Accessor("values")
    public abstract Map<LootRarity, ?> curiosforge_getValues();
}
