package com.apotheosis_artifice.jei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.apotheosis_artifice.compat.AffixCompatibilityDiagnostics;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.item.ItemStack;

public record AffixDetailEntry(LootCategory category, Affix affix, List<RarityEntry> rarities, List<ItemStack> items) {

    public record RarityEntry(LootRarity rarity, Component rangeTooltip) {}

    public Component name() {
        try {
            return affix.getName(true);
        } catch (Exception exception) {
            AffixCompatibilityDiagnostics.warnOnce(affix.getId(), category.getName(), "JEI name", exception);
            return Component.literal(String.valueOf(affix.getId()));
        }
    }

    public static List<AffixDetailEntry> createAll(LootCategory cat) {
        List<LootRarity> rarities = new ArrayList<>();
        for (DynamicHolder<LootRarity> holder : RarityRegistry.INSTANCE.getOrderedRarities()) {
            if (holder.isBound()) rarities.add(holder.get());
        }
        LootCategory genericCurio = cat.getName().startsWith("curios:") ? LootCategory.byId("curio") : null;
        return createAll(cat, AffixCodexEntry.getCategoryItems(cat), AffixRegistry.INSTANCE.getValues(), rarities, genericCurio);
    }

    static List<AffixDetailEntry> createAll(LootCategory cat, List<ItemStack> candidates, Collection<Affix> affixes,
            List<LootRarity> rarities, LootCategory genericCurio) {
        List<AffixDetailEntry> entries = new ArrayList<>();
        for (Affix affix : affixes) {
            Map<List<RarityEntry>, List<ItemStack>> groups = new LinkedHashMap<>();
            Map<LootRarity, Component> catalogDescriptions = new HashMap<>();
            for (ItemStack stack : candidates) {
                if (stack.isEmpty()) continue;
                List<RarityEntry> supported = new ArrayList<>();
                for (LootRarity rarity : rarities) {
                    try {
                        if (!affix.canApplyTo(stack, cat, rarity)) continue;
                        if (genericCurio != null && !genericCurio.isNone() && affix.canApplyTo(stack, genericCurio, rarity)) continue;
                    } catch (Exception exception) {
                        AffixCompatibilityDiagnostics.warnOnce(affix.getId(), cat.getName(), "JEI applicability", exception);
                        continue;
                    }
                    Component catalog = catalogDescriptions.computeIfAbsent(rarity,
                        key -> description(affix, ItemStack.EMPTY, key, cat, "JEI catalog description"));
                    Component actual = description(affix, stack, rarity, cat, "JEI description");
                    supported.add(new RarityEntry(rarity, mergeDescriptions(actual, catalog)));
                }
                if (!supported.isEmpty()) {
                    groups.computeIfAbsent(List.copyOf(supported), key -> new ArrayList<>()).add(stack);
                }
            }
            groups.forEach((supported, stacks) -> entries.add(new AffixDetailEntry(cat, affix, supported, List.copyOf(stacks))));
        }

        entries.sort(Comparator.comparing(entry -> entry.name().getString()));

        return entries;
    }

    private static Component description(Affix affix, ItemStack stack, LootRarity rarity, LootCategory category, String stage) {
        try {
            Component desc = affix.getAugmentingText(stack, rarity, 0.5f);
            if (desc != null && !desc.getString().isBlank()) {
                Component example = desc.copy();
                if (AffixPreviewText.hasBounds(example)) {
                    try {
                        Component lower = affix.getAugmentingText(stack, rarity, 0F);
                        Component upper = affix.getAugmentingText(stack, rarity, 1F);
                        if (lower != null && upper != null) return AffixPreviewText.midpoint(example, lower, upper);
                    } catch (Exception exception) {
                        AffixCompatibilityDiagnostics.warnOnce(affix.getId(), category.getName(), stage + " endpoints", exception);
                    }
                }
                return example;
            }
        } catch (Exception exception) {
            AffixCompatibilityDiagnostics.warnOnce(affix.getId(), category.getName(), stage, exception);
        }
        return Component.literal("");
    }

    private static Component mergeDescriptions(Component actual, Component catalog) {
        var result = Component.empty().withStyle(net.minecraft.ChatFormatting.YELLOW);
        String actualText = actual.getString();
        String catalogText = catalog.getString();
        if (actualText.isBlank()) return result.append(catalog.copy());
        if (catalogText.isBlank() || ("\n" + actualText + "\n").contains("\n" + catalogText + "\n"))
            return result.append(actual.copy());
        if (catalogText.startsWith(actualText + "\n")) return result.append(catalog.copy());
        var actualLines = descriptionLines(actual);
        var catalogLines = descriptionLines(catalog);
        result.append(actual.copy());
        if (!actualLines.get(0).getString().strip().equals(catalogLines.get(0).getString().strip())) return result;
        var seen = new HashSet<String>();
        actualLines.forEach(line -> seen.add(line.getString().strip()));
        for (Component line : catalogLines) {
            if (seen.add(line.getString().strip())) result.append("\n").append(line.copy());
        }
        return result;
    }

    private static List<Component> descriptionLines(Component text) {
        List<MutableComponent> lines = new ArrayList<>();
        lines.add(Component.empty());
        appendDescriptionLines(text, Style.EMPTY, lines);
        return lines.stream().filter(line -> !line.getString().isBlank()).map(line -> (Component) line).toList();
    }

    private static void appendDescriptionLines(Component text, Style parent, List<MutableComponent> lines) {
        Style style = text.getStyle().applyTo(parent);
        if (text.getContents() instanceof LiteralContents literal) {
            String[] parts = literal.text().split("\n", -1);
            for (int index = 0; index < parts.length; index++) {
                if (index > 0) lines.add(Component.empty());
                if (!parts[index].isEmpty()) lines.get(lines.size() - 1).append(Component.literal(parts[index]).setStyle(style));
            }
        } else {
            lines.get(lines.size() - 1).append(text.plainCopy().setStyle(style));
        }
        for (Component child : text.getSiblings()) appendDescriptionLines(child, style, lines);
    }
}
