package com.apotheosis_artifice;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;

public class CatOverride {
    private static LootCategory override;

    public static void set(LootCategory cat) {
        override = cat;
    }

    public static LootCategory get() {
        return override;
    }
}
