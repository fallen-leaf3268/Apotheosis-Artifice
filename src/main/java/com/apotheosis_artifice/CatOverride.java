package com.apotheosis_artifice;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.world.item.ItemStack;
import com.apotheosis_artifice.compat.CurioSlotAccess;
import top.theillusivec4.curios.api.CuriosCapability;

public class CatOverride {
    private static final ThreadLocal<LootCategory> OVERRIDE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> NATIVE_LOOKUP = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> REFORGING = new ThreadLocal<>();
    private static final ThreadLocal<ItemStack> CURIO_ACCESS = new ThreadLocal<>();

    public static void set(LootCategory cat) {
        if (cat == null) OVERRIDE.remove();
        else OVERRIDE.set(cat);
    }

    public static LootCategory get() {
        return OVERRIDE.get();
    }

    public static void clear() {
        OVERRIDE.remove();
    }

    public static boolean isCurioItem(ItemStack stack) {
        return !stack.isEmpty() && (stack.getCapability(CuriosCapability.ITEM).isPresent()
            || !CurioSlotAccess.getItemStackSlots(stack).isEmpty());
    }

    public static boolean isApotheosisBlocked(ItemStack stack) {
        if (!com.apotheosis_artifice.curios.CuriosApotheosisModule.isDisabled()) return false;
        if (CURIO_ACCESS.get() == stack) return true;
        if (!isCurioItem(stack)) return false;
        LootCategory nativeCategory = forNativeItem(stack);
        return nativeCategory.isNone() || isCurioCategory(nativeCategory);
    }

    public static boolean isCurioCategory(LootCategory category) {
        return category.getName().equals("curio") || category.getName().startsWith("curios:");
    }

    public static ItemStack setCurioAccess(ItemStack stack) {
        ItemStack previous = CURIO_ACCESS.get();
        if (stack == null) CURIO_ACCESS.remove();
        else CURIO_ACCESS.set(stack);
        return previous;
    }

    public static LootCategory forNativeItem(ItemStack stack) {
        if (stack.isEmpty()) return LootCategory.NONE;
        LootCategory previous = OVERRIDE.get();
        Boolean previousLookup = NATIVE_LOOKUP.get();
        OVERRIDE.remove();
        NATIVE_LOOKUP.set(true);
        try {
            return LootCategory.forItem(stack);
        } finally {
            set(previous);
            if (previousLookup == null) NATIVE_LOOKUP.remove();
            else NATIVE_LOOKUP.set(previousLookup);
        }
    }

    public static boolean isNativeLookup() {
        return Boolean.TRUE.equals(NATIVE_LOOKUP.get());
    }

    public static boolean isReforging() {
        return Boolean.TRUE.equals(REFORGING.get());
    }

    public static void setReforging(boolean reforging) {
        if (reforging) REFORGING.set(true);
        else REFORGING.remove();
    }
}
