package com.apotheosis_artifice.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record AffixGemEntry(LootCategory category, Gem gem, List<RarityEntry> rarities) {

    public record RarityEntry(LootRarity rarity, Component bonusTooltip) {}

    public static List<AffixGemEntry> createAll(LootCategory cat) {
        var orderedRarities = RarityRegistry.INSTANCE.getOrderedRarities();
        List<AffixGemEntry> entries = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();

        // 如果是通用 curio，也扫描所有 curios:xxx 子槽位
        boolean isGenericCurio = "curio".equals(cat.getName());
        List<LootCategory> scanCats = new ArrayList<>();
        scanCats.add(cat);
        if (isGenericCurio) {
            for (LootCategory sub : LootCategory.VALUES) {
                if (!sub.isNone() && sub.getName().startsWith("curios:") && !"curio".equals(sub.getName())) {
                    scanCats.add(sub);
                }
            }
        }

        for (LootCategory scanCat : scanCats) {
            for (Gem gem : GemRegistry.INSTANCE.getValues()) {
                if (seen.contains(gem.getId())) continue;

                List<RarityEntry> supported = new ArrayList<>();
                for (DynamicHolder<LootRarity> holder : orderedRarities) {
                    if (!holder.isBound()) continue;
                    LootRarity rarity = holder.get();
                    if (rarity.ordinal() < gem.getMinRarity().ordinal() || rarity.ordinal() > gem.getMaxRarity().ordinal())
                        continue;

                    Component bonusComp = Component.literal("");
                    try {
                        var bonusOpt = gem.getBonus(scanCat, rarity);
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
                    entries.add(new AffixGemEntry(scanCat, gem, supported));
                    seen.add(gem.getId());
                }
            }
        }

        entries.sort(Comparator.comparing(a -> a.gem().getId().toString()));
        return entries;
    }
}
