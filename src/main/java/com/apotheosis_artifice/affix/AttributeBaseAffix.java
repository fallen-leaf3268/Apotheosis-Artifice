package com.apotheosis_artifice.affix;

import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.apotheosis_artifice.AffixTypes;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class AttributeBaseAffix extends Affix implements AffixTypes {

    public static final Codec<AttributeBaseAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ForgeRegistries.ATTRIBUTES.getCodec().fieldOf("attribute").forGetter(a -> a.attribute),
            GemBonus.VALUES_CODEC.fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.optionalFieldOf("types", Set.of()).forGetter(a -> a.types))
        .apply(inst, AttributeBaseAffix::new));

    protected final Attribute attribute;
    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> types;

    public AttributeBaseAffix(Attribute attribute, Map<LootRarity, StepFunction> values, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.attribute = attribute;
        this.values = values;
        this.types = types;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public float getBonus(LootRarity rarity, float level) {
        StepFunction sf = values.get(rarity);
        if (sf == null) return 0;
        return sf.get(level);
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (!values.containsKey(rarity)) return false;
        if (types.isEmpty()) return true;
        if (AffixTypes.curiosforge_typeMatches(types, cat)) return true;
        return AffixTypes.tagMatches(types, stack);
    }

    @Override
    public Set<LootCategory> curiosforge_getTypes() {
        return types;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        float v = getBonus(rarity, level);
        return Component.translatable("affix.apotheosis_artifice.attribute_base", (int) v, Component.translatable(attribute.getDescriptionId()));
    }
}