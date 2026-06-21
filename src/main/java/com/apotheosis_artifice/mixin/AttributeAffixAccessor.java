package com.apotheosis_artifice.mixin;

import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.shadowsoffire.apotheosis.adventure.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

@Mixin(value = AttributeAffix.class, remap = false)
public interface AttributeAffixAccessor {

    @Accessor("types")
    Set<LootCategory> curiosforge_getTypes();

    @Accessor("attribute")
    Attribute getAttribute();

    @Accessor("operation")
    Operation getOperation();

    @Accessor("values")
    Map<LootRarity, StepFunction> getValues();
}
