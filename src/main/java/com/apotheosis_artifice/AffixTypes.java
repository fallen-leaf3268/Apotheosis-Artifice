package com.apotheosis_artifice;

import java.util.Set;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.world.item.ItemStack;

public interface AffixTypes {
    default Set<LootCategory> curiosforge_getTypes() {
        return Set.of();
    }

    static boolean curiosforge_typeMatches(Set<LootCategory> types, LootCategory cat) {
        String catName = cat.getName();
        return types.stream().anyMatch(t -> {
            String tName = t.getName();
            return catName.equals(tName)
                || catName.startsWith(tName + ":")
                || (tName.equals("curio") && catName.startsWith("curios:"));
        });
    }

    static boolean tagMatches(Set<LootCategory> types, ItemStack stack) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData != null && afxData.contains("curio_artifice")) {
            String val = afxData.getString("curio_artifice");
            return types.stream().anyMatch(t -> {
                String tName = t.getName();
                return val.equals(tName)
                    || val.startsWith(tName + ":")
                    || (tName.equals("curio") && val.startsWith("curios:"));
            });
        }
        return false;
    }
}
