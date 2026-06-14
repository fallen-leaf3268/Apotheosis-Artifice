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
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class DamageResistanceAffix extends Affix {

    public static final Codec<DamageResistanceAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ResourceLocation.CODEC.listOf().fieldOf("damage_types").forGetter(a -> a.damageTypes),
            Codec.FLOAT.optionalFieldOf("reduction", 0.5F).forGetter(a -> a.reduction),
            LootCategory.SET_CODEC.optionalFieldOf("types", Set.of()).forGetter(a -> a.types))
        .apply(inst, DamageResistanceAffix::new));

    protected final List<ResourceLocation> damageTypes;
    protected final float reduction;
    protected final Set<LootCategory> types;

    public DamageResistanceAffix(List<ResourceLocation> damageTypes, float reduction, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.damageTypes = damageTypes;
        this.reduction = reduction;
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

    @Override
    public float onHurt(ItemStack stack, LootRarity rarity, float level, DamageSource src, LivingEntity ent, float amount) {
        ResourceLocation id = ent.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getKey(src.type());
        if (id != null && damageTypes.contains(id)) {
            return amount * (1 - reduction);
        }
        return super.onHurt(stack, rarity, level, src, ent, amount);
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        String names = damageTypes.stream()
            .map(id -> {
                String key = "death.attack." + id.getPath();
                Component t = Component.translatable(key);
                String raw = t.getString();
                return raw.equals(key) ? id.getPath() : raw;
            })
            .collect(Collectors.joining(", "));
        int pct = Math.round(reduction * 100);
        return Component.translatable("affix.apotheosis_artifice.damage_resistance", pct, names);
    }
}
