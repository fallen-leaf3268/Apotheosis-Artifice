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
import net.minecraft.world.item.ItemStack;

public class CurioSlotBonusAffix extends Affix implements AffixTypes {

    public static final Codec<CurioSlotBonusAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Codec.STRING.fieldOf("slot").forGetter(a -> a.slot),
            GemBonus.VALUES_CODEC.fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.optionalFieldOf("types", Set.of()).forGetter(a -> a.types),
            Codec.STRING.optionalFieldOf("uuid", "").forGetter(a -> a.uuid))
        .apply(inst, CurioSlotBonusAffix::new));

    protected final String slot;
    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> types;
    protected final String uuid;

    public CurioSlotBonusAffix(String slot, Map<LootRarity, StepFunction> values, Set<LootCategory> types) {
        this(slot, values, types, "");
    }

    public CurioSlotBonusAffix(String slot, Map<LootRarity, StepFunction> values, Set<LootCategory> types, String uuid) {
        super(AffixType.ABILITY);
        this.slot = slot;
        this.values = values;
        this.types = types;
        this.uuid = uuid == null ? "" : uuid;
    }

    public String getFixedUuid() {
        return uuid;
    }

    public String getSlot() {
        return slot;
    }

    public String getSlotId() {
        return slot.startsWith("curios:") ? slot.substring("curios:".length()) : slot;
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
        int v = (int) getBonus(rarity, level);
        Component slotName = Component.translatable("curios.identifier." + getSlotId());
        MutableComponent base = Component.translatable("affix.apotheosis_artifice.curio_slot_bonus", slotName, v);
        if (uuid != null && !uuid.isEmpty()) {
            base.append(" ").append(Component.translatable("affix.apotheosis_artifice.curio_slot_bonus.unique"));
        }
        return base;
    }

    @Override
    public Component getName(boolean prefix) {
        return Component.translatable("affix." + this.getId());
    }
}