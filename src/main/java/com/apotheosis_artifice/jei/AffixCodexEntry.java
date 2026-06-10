package com.apotheosis_artifice.jei;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.world.item.ItemStack;

public record AffixCodexEntry(LootCategory category, LootRarity rarity, List<Affix> affixes) {

    @Nullable
    public static AffixCodexEntry create(LootCategory cat, LootRarity rar) {
        var registry = dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry.INSTANCE;
        var affixes = registry.getValues().stream()
            .filter(a -> {
                try { return a.canApplyTo(ItemStack.EMPTY, cat, rar); }
                catch (Exception e) { return false; }
            })
            .sorted(java.util.Comparator.comparing(a -> {
                try { return a.getName(true).getString(); }
                catch (Exception e) { return ""; }
            }))
            .toList();
        if (affixes.isEmpty()) return null;
        return new AffixCodexEntry(cat, rar, affixes);
    }
}
