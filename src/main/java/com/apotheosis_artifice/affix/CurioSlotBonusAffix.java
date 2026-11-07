package com.apotheosis_artifice.affix;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.apotheosis_artifice.AffixTypes;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class CurioSlotBonusAffix extends Affix implements AffixTypes {

    private static final Codec<Optional<UUID>> FIXED_UUID_CODEC = Codec.STRING.comapFlatMap(value -> {
        if (value.isEmpty()) return DataResult.success(Optional.empty());
        try {
            return DataResult.success(Optional.of(UUID.fromString(value)));
        } catch (IllegalArgumentException error) {
            return DataResult.error(() -> "Invalid fixed curio slot UUID: " + value);
        }
    }, value -> value.map(UUID::toString).orElse(""));

    public static final Codec<CurioSlotBonusAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Codec.STRING.fieldOf("slot").forGetter(a -> a.slot),
            GemBonus.VALUES_CODEC.fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.optionalFieldOf("types", Set.of()).forGetter(a -> a.types),
            PlaceboCodecs.nullableField(FIXED_UUID_CODEC, "uuid", Optional.empty()).forGetter(a -> Optional.ofNullable(a.fixedUuid)))
        .apply(inst, CurioSlotBonusAffix::new));

    protected final String slot;
    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> types;
    protected final String uuid;
    private final UUID fixedUuid;

    public CurioSlotBonusAffix(String slot, Map<LootRarity, StepFunction> values, Set<LootCategory> types) {
        this(slot, values, types, "");
    }

    public CurioSlotBonusAffix(String slot, Map<LootRarity, StepFunction> values, Set<LootCategory> types, String uuid) {
        this(slot, values, types, uuid == null || uuid.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(uuid)));
    }

    private CurioSlotBonusAffix(String slot, Map<LootRarity, StepFunction> values, Set<LootCategory> types, Optional<UUID> uuid) {
        super(AffixType.ABILITY);
        this.slot = slot;
        this.values = values;
        this.types = types;
        this.uuid = uuid.map(UUID::toString).orElse("");
        this.fixedUuid = uuid.orElse(null);
    }

    public String getFixedUuid() {
        return uuid;
    }

    public UUID getFixedUuidValue() {
        return fixedUuid;
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
        if (fixedUuid != null) {
            base.append(" ").append(Component.translatable("affix.apotheosis_artifice.curio_slot_bonus.unique"));
        }
        return base;
    }

    @Override
    public Component getName(boolean prefix) {
        return Component.translatable("affix." + this.getId());
    }
}
