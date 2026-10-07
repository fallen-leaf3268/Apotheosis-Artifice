package com.apotheosis_artifice.enchant;

public final class EnchantingCostRules {

    private EnchantingCostRules() {}

    public static int lapisCost(int base, int discount) {
        return Math.max(0, Math.max(0, base) - Math.max(0, Math.min(3, discount)));
    }

    public static int experienceCost(int base, int percent) {
        int remaining = 100 - Math.max(0, Math.min(100, percent));
        return (int) ((Math.max(0, base) * (long) remaining + 99) / 100);
    }
}
