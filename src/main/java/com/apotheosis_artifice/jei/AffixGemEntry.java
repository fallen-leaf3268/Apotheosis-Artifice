package com.apotheosis_artifice.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record AffixGemEntry(LootCategory category, Gem gem, List<RarityEntry> rarities) {

    public record RarityEntry(LootRarity rarity, Component bonusTooltip) {}

    public static List<AffixGemEntry> createAll(LootCategory cat) {
        var orderedRarities = RarityRegistry.INSTANCE.getOrderedRarities();
        List<AffixGemEntry> entries = new ArrayList<>();

        for (Gem gem : GemRegistry.INSTANCE.getValues()) {
            List<RarityEntry> supported = new ArrayList<>();
            for (DynamicHolder<LootRarity> holder : orderedRarities) {
                if (!holder.isBound()) continue;
                LootRarity rarity = holder.get();
                if (rarity.ordinal() < gem.getMinRarity().ordinal() || rarity.ordinal() > gem.getMaxRarity().ordinal()) continue;

                // 用 gem.getBonus(cat, rarity) 判定可镶嵌性：它已包含 GemGetBonusMixin
                // 注入的“通用 curio → 具体 curio:xxx 槽位”回退，因此 JEI 显示与运行时
                // 实际可镶嵌行为完全一致（修复：万能神化护符等具体 curio 槽位看不到宝石）。
                Component bonusComp = Component.literal("");
                try {
                    var bonusOpt = gem.getBonus(cat, rarity);
                    if (bonusOpt.isPresent()) {
                        ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
                        GemItem.setGem(stack, gem);
                        AffixHelper.setRarity(stack, rarity);
                        Component tt = bonusOpt.get().getSocketBonusTooltip(stack, rarity);
                        if (tt != null) bonusComp = tt;
                    }
                } catch (Exception ignored) {}
                if (!bonusComp.getString().isBlank()) {
                    supported.add(new RarityEntry(rarity, bonusComp));
                }
            }
            if (!supported.isEmpty()) {
                entries.add(new AffixGemEntry(cat, gem, supported));
            }
        }

        entries.sort(Comparator.comparing(a -> a.gem().getId().toString()));
        return entries;
    }
}
