package com.apotheosis_artifice.compat;

import net.minecraft.world.item.ItemStack;

public final class BetterCombatCompat {

    private static final String MODID = "bettercombat";
    private static Boolean LOADED = null;
    private static Class<?> REGISTRY_CLASS = null;
    private static java.lang.reflect.Method GET_ATTRIBUTES = null;
    private static java.lang.reflect.Method IS_TWO_HANDED = null;

    private BetterCombatCompat() {}

    public static boolean isLoaded() {
        if (LOADED == null) {
            LOADED = net.minecraftforge.fml.ModList.get().isLoaded(MODID);
            if (LOADED) tryInitReflection();
        }
        return LOADED;
    }

    public static boolean isTwoHanded(ItemStack stack) {
        if (!isLoaded() || stack.isEmpty()) return false;
        try {
            if (REGISTRY_CLASS == null || GET_ATTRIBUTES == null || IS_TWO_HANDED == null) {
                tryInitReflection();
                if (REGISTRY_CLASS == null) return false;
            }
            Object attrs = GET_ATTRIBUTES.invoke(null, stack);
            if (attrs == null) return false;
            Object result = IS_TWO_HANDED.invoke(attrs);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void tryInitReflection() {
        try {
            REGISTRY_CLASS = Class.forName("net.bettercombat.logic.WeaponRegistry");
            GET_ATTRIBUTES = REGISTRY_CLASS.getMethod("getAttributes", ItemStack.class);
            Class<?> attrsClass = Class.forName("net.bettercombat.api.WeaponAttributes");
            IS_TWO_HANDED = attrsClass.getMethod("isTwoHanded");
        } catch (Throwable t) {
            REGISTRY_CLASS = null;
            GET_ATTRIBUTES = null;
            IS_TWO_HANDED = null;
        }
    }
}
