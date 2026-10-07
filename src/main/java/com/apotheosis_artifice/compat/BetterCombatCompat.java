package com.apotheosis_artifice.compat;

import java.lang.reflect.InvocationTargetException;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import net.minecraft.world.item.ItemStack;

public final class BetterCombatCompat {

    private static final String MODID = "bettercombat";
    private static volatile Boolean LOADED = null;
    private static volatile Class<?> REGISTRY_CLASS = null;
    private static volatile java.lang.reflect.Method GET_ATTRIBUTES = null;
    private static volatile java.lang.reflect.Method IS_TWO_HANDED = null;
    private static boolean reflectionAttempted;
    private static boolean invocationWarned;

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
                if (REGISTRY_CLASS == null || GET_ATTRIBUTES == null || IS_TWO_HANDED == null) return false;
            }
            Object attrs = GET_ATTRIBUTES.invoke(null, stack);
            if (attrs == null) return false;
            Object result = IS_TWO_HANDED.invoke(attrs);
            return result instanceof Boolean && (Boolean) result;
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Error error) throw error;
            warnInvocation(exception);
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            warnInvocation(exception);
            return false;
        }
    }

    private static synchronized void tryInitReflection() {
        if (reflectionAttempted) return;
        try {
            Class<?> registry = Class.forName("net.bettercombat.logic.WeaponRegistry", false, BetterCombatCompat.class.getClassLoader());
            var getAttributes = registry.getMethod("getAttributes", ItemStack.class);
            Class<?> attrsClass = Class.forName("net.bettercombat.api.WeaponAttributes", false, BetterCombatCompat.class.getClassLoader());
            var isTwoHanded = attrsClass.getMethod("isTwoHanded");
            REGISTRY_CLASS = registry;
            GET_ATTRIBUTES = getAttributes;
            IS_TWO_HANDED = isTwoHanded;
        } catch (ReflectiveOperationException | LinkageError exception) {
            REGISTRY_CLASS = null;
            GET_ATTRIBUTES = null;
            IS_TWO_HANDED = null;
            ApotheosisArtificeMod.LOGGER.warn("Better Combat weapon compatibility is unavailable because its API could not be resolved", exception);
        } finally {
            reflectionAttempted = true;
        }
    }

    private static synchronized void warnInvocation(Throwable exception) {
        if (!invocationWarned) {
            invocationWarned = true;
            ApotheosisArtificeMod.LOGGER.warn("Better Combat weapon attributes could not be read; later queries will retry", exception);
        }
    }
}
