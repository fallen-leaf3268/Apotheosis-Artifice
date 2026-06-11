package com.apotheosis_artifice.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record AffixDetailEntry(LootCategory category, Affix affix, List<RarityEntry> rarities) {

    public record RarityEntry(LootRarity rarity, Component rangeTooltip) {}

    public static List<AffixDetailEntry> createAll(LootCategory cat) {
        var registry = AffixRegistry.INSTANCE;
        var orderedRarities = RarityRegistry.INSTANCE.getOrderedRarities();

        List<AffixDetailEntry> entries = new ArrayList<>();

        for (Affix affix : registry.getValues()) {
            List<RarityEntry> supported = new ArrayList<>();
            for (DynamicHolder<LootRarity> holder : orderedRarities) {
                if (!holder.isBound()) continue;
                LootRarity rarity = holder.get();
                try {
                    if (affix.canApplyTo(ItemStack.EMPTY, cat, rarity)) {
                        supported.add(new RarityEntry(rarity, buildDescription(affix, rarity)));
                    }
                } catch (Exception e) { /* skip */ }
            }
            if (!supported.isEmpty()) {
                entries.add(new AffixDetailEntry(cat, affix, supported));
            }
        }

        entries.sort(Comparator.comparing(a -> {
            try { return a.affix().getName(true).getString(); }
            catch (Exception e) { return ""; }
        }));

        return entries;
    }

    public static Component buildDescription(Affix affix, LootRarity rarity) {
        ItemStack stack = ItemStack.EMPTY;
        try {
            Component desc = affix.getAugmentingText(stack, rarity, 0.5f);
            if (desc != null && !desc.getString().isBlank())
                return Component.literal("§7" + desc.getString());
        } catch (Exception ignored) {}
        return Component.literal("");
    }
}
