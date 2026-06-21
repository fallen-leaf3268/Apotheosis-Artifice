package com.apotheosis_artifice.affix;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.apotheosis_artifice.AffixTypes;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectImmunityAffix extends Affix implements AffixTypes {

    public static final Codec<EffectImmunityAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ForgeRegistries.MOB_EFFECTS.getCodec().listOf().fieldOf("effects").forGetter(a -> a.effects),
            PlaceboCodecs.setOf(LootRarity.CODEC).fieldOf("rarities").forGetter(a -> a.rarities),
            LootCategory.SET_CODEC.optionalFieldOf("types", Set.of()).forGetter(a -> a.types))
        .apply(inst, EffectImmunityAffix::new));

    protected final List<MobEffect> effects;
    protected final Set<LootRarity> rarities;
    protected final Set<LootCategory> types;

    public EffectImmunityAffix(List<MobEffect> effects, Set<LootRarity> rarities, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.effects = effects;
        this.rarities = rarities;
        this.types = types;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (!rarityInSet(rarity)) return false;
        if (types.isEmpty()) return true;
        if (types.contains(cat)) return true;
        if (tagMatches(stack, types)) return true;
        String name = cat.getName();
        return types.stream().anyMatch(t -> name.startsWith(t.getName()));
    }

    private static boolean tagMatches(ItemStack stack, Set<LootCategory> types) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData != null && afxData.contains("curio_artifice")) {
            String val = afxData.getString("curio_artifice");
            return types.stream().anyMatch(t -> val.startsWith(t.getName()));
        }
        return false;
    }

    private boolean rarityInSet(LootRarity rarity) {
        for (LootRarity r : rarities) {
            if (r.ordinal() == rarity.ordinal()) return true;
        }
        return false;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public Set<LootCategory> curiosforge_getTypes() {
        return types;
    }

    public boolean hasEffect(MobEffect effect) {
        return effects.contains(effect);
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        String names = effects.stream()
            .map(e -> Component.translatable(e.getDescriptionId()).getString())
            .collect(Collectors.joining(", "));
        return Component.translatable("affix.apotheosis_artifice.effect_immunity",
            Component.literal(names).withStyle(ChatFormatting.GREEN));
    }
}
