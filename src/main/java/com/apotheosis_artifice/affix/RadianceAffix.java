package com.apotheosis_artifice.affix;

import java.util.Map;
import java.util.Set;

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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * “光辉” —— 项链专属前缀（ABILITY 类型）。佩戴后玩家周围会持续发出可移动的光源，
 * 光照强度由稀有度决定（见 {@link #getLight}）。实际放置/移动光源的逻辑在
 * {@code ApotheosisEvents#radianceTick}，本类只负责数据、可套用判定与 tooltip。
 */
public class RadianceAffix extends Affix implements AffixTypes {

    public static final Codec<RadianceAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            GemBonus.VALUES_CODEC.fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.optionalFieldOf("types", Set.of()).forGetter(a -> a.types))
        .apply(inst, RadianceAffix::new));

    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> types;

    public RadianceAffix(Map<LootRarity, StepFunction> values, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.values = values;
        this.types = types;
    }

    /** 该稀有度 + 词缀等级下的光照强度，限制在 0..15；无效返回 0。 */
    public int getLight(LootRarity rarity, float level) {
        StepFunction sf = null;
        for (var e : values.entrySet()) {
            if (e.getKey().ordinal() == rarity.ordinal()) {
                sf = e.getValue();
                break;
            }
        }
        if (sf == null) return 0;
        return Mth.clamp(Math.round(sf.get(level)), 0, 15);
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (!rarityInValues(rarity)) return false;
        if (types.isEmpty()) return true;
        // 用 AffixTypes 提供的边界匹配（按 ':' 区分槽位），避免 curio:necklace 误匹配 curio:necklace_x。
        if (AffixTypes.curiosforge_typeMatches(types, cat)) return true;
        return tagMatches(stack);
    }

    private boolean tagMatches(ItemStack stack) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData != null && afxData.contains("curio_artifice")) {
            String val = afxData.getString("curio_artifice");
            return types.stream().anyMatch(t ->
                val.equals(t.getName()) || val.startsWith(t.getName() + ":"));
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
    public Set<LootCategory> curiosforge_getTypes() {
        return types;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        return Component.translatable("affix.apotheosis_artifice.radiance", getLight(rarity, level));
    }
}
