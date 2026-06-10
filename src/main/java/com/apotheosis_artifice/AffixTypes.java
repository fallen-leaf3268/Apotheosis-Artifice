package com.apotheosis_artifice;

import java.util.Set;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;

public interface AffixTypes {
    default Set<LootCategory> curiosforge_getTypes() {
        return Set.of();
    }

    static boolean curiosforge_typeMatches(Set<LootCategory> types, LootCategory cat) {
        String catName = cat.getName();
        return types.stream().anyMatch(t -> catName.startsWith(t.getName()));
    }
}
