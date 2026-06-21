package com.apotheosis_artifice.affix;

import java.util.List;
import java.util.Map;
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
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class DamageResistanceAffix extends Affix implements AffixTypes {

    public static final Codec<DamageResistanceAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ResourceLocation.CODEC.listOf().fieldOf("damage_types").forGetter(a -> a.damageTypes),
            GemBonus.VALUES_CODEC.fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.optionalFieldOf("types", Set.of()).forGetter(a -> a.types))
        .apply(inst, DamageResistanceAffix::new));

    protected final List<ResourceLocation> damageTypes;
    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> types;

    public DamageResistanceAffix(List<ResourceLocation> damageTypes, Map<LootRarity, StepFunction> values, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.damageTypes = damageTypes;
        this.values = values;
        this.types = types;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (!rarityInValues(rarity)) return false;
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

    private boolean rarityInValues(LootRarity rarity) {
        for (LootRarity key : values.keySet()) {
            if (key.ordinal() == rarity.ordinal()) return true;
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

    @Override
    public float onHurt(ItemStack stack, LootRarity rarity, float level, DamageSource src, LivingEntity ent, float amount) {
        ResourceLocation id = ent.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getKey(src.type());
        if (id != null && damageTypes.contains(id)) {
            float reduction = this.values.get(rarity).get(level);
            return amount * (1 - reduction);
        }
        return super.onHurt(stack, rarity, level, src, ent, amount);
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        String names = damageTypes.stream()
            .map(id -> {
                String key = "damage_type." + id.getNamespace() + "." + id.getPath();
                Component t = Component.translatable(key);
                String raw = t.getString();
                return raw.equals(key) ? id.getPath() : raw;
            })
            .collect(Collectors.joining(", "));
        float reduction = this.values.get(rarity).get(level);
        int pct = Math.round(reduction * 100);
        if (pct >= 100) {
            return Component.translatable("affix.apotheosis_artifice.damage_immunity", names);
        }
        return Component.translatable("affix.apotheosis_artifice.damage_resistance", names, pct);
    }
}
