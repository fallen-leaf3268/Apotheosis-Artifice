package com.apotheosis_artifice.affix;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectImmunityAffix extends Affix {

    public static final Codec<EffectImmunityAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ForgeRegistries.MOB_EFFECTS.getCodec().listOf().fieldOf("effects").forGetter(a -> a.effects),
            LootCategory.SET_CODEC.optionalFieldOf("types", Set.of()).forGetter(a -> a.types))
        .apply(inst, EffectImmunityAffix::new));

    protected final List<MobEffect> effects;
    protected final Set<LootCategory> types;

    public EffectImmunityAffix(List<MobEffect> effects, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.effects = effects;
        this.types = types;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (types.isEmpty()) return true;
        if (types.contains(cat)) return true;
        String name = cat.getName();
        return types.stream().anyMatch(t -> name.startsWith(t.getName()));
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    public boolean hasEffect(MobEffect effect) {
        return effects.contains(effect);
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        String names = effects.stream()
            .map(e -> Component.translatable(e.getDescriptionId()).getString())
            .collect(Collectors.joining(", "));
        return Component.translatable("affix.apotheosis_artifice.effect_immunity", names);
    }
}
