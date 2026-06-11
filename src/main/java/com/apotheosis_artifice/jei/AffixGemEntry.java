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
            boolean matches = gem.getBonuses().stream()
                .anyMatch(b -> b.getGemClass().types().contains(cat));
            if (!matches) continue;

            List<RarityEntry> supported = new ArrayList<>();
            for (DynamicHolder<LootRarity> holder : orderedRarities) {
                if (!holder.isBound()) continue;
                LootRarity rarity = holder.get();
                if (rarity.ordinal() < gem.getMinRarity().ordinal() || rarity.ordinal() > gem.getMaxRarity().ordinal()) continue;

                // 收集该分类对应的镶嵌属性描述（保留原始颜色）
                Component bonusComp = Component.literal("");
                try {
                    ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
                    GemItem.setGem(stack, gem);
                    AffixHelper.setRarity(stack, rarity);
                    for (var bonus : gem.getBonuses()) {
                        if (bonus.getGemClass().types().contains(cat) && bonus.supports(rarity)) {
                            bonusComp = bonus.getSocketBonusTooltip(stack, rarity);
                            break;
                        }
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
