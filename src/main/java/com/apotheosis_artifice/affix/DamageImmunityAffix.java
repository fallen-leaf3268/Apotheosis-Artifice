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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class DamageImmunityAffix extends Affix {

    public static final Codec<DamageImmunityAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ResourceLocation.CODEC.listOf().fieldOf("damage_types").forGetter(a -> a.damageTypes),
            LootCategory.SET_CODEC.optionalFieldOf("types", Set.of()).forGetter(a -> a.types))
        .apply(inst, DamageImmunityAffix::new));

    protected final List<ResourceLocation> damageTypes;
    protected final Set<LootCategory> types;

    public DamageImmunityAffix(List<ResourceLocation> damageTypes, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.damageTypes = damageTypes;
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
            return 0;
        }
        return super.onHurt(stack, rarity, level, src, ent, amount);
    }

    @Override
    public net.minecraft.network.chat.MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        String names = damageTypes.stream()
            .map(id -> {
                String key = "death.attack." + id.getPath();
                Component t = Component.translatable(key);
                String raw = t.getString();
                return raw.equals(key) ? id.getPath() : raw;
            })
            .collect(Collectors.joining(", "));
        return Component.translatable("affix.apotheosis_artifice.damage_immunity", names);
    }
}
