package com.apotheosis_artifice.compat;

import java.lang.reflect.Method;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class EnigmaticLegacyCompat {

    private static final String MODID = "enigmaticlegacy";
    private static final ResourceLocation ENCHANTER_PEARL = new ResourceLocation(MODID, "enchanter_pearl");
    private static boolean initialized;
    private static boolean available;
    private static Method isPresent;
    private static Method mergeEnchantments;
    private static Method maybeApplyEternalBinding;

    private EnigmaticLegacyCompat() {}

    public static boolean isEnchanterPearlActive(Player player) {
        if (player == null || !ModList.get().isLoaded(MODID)) return false;
        initialize();
        if (!available) return false;
        Item pearl = ForgeRegistries.ITEMS.getValue(ENCHANTER_PEARL);
        if (pearl == null) return false;
        try {
            return Boolean.TRUE.equals(isPresent.invoke(pearl, player));
        } catch (ReflectiveOperationException e) {
            ApotheosisArtificeMod.LOGGER.warn("Failed to query Enchanter's Pearl state", e);
            return false;
        }
    }

    public static ItemStack mergePearlEnchantments(ItemStack primary, ItemStack bonus) {
        initialize();
        if (!available) return primary;
        try {
            ItemStack merged = (ItemStack) mergeEnchantments.invoke(null, primary, bonus, false, false);
            return (ItemStack) maybeApplyEternalBinding.invoke(null, merged);
        } catch (ReflectiveOperationException e) {
            ApotheosisArtificeMod.LOGGER.warn("Failed to apply Enchanter's Pearl enchantments", e);
            return primary;
        }
    }

    private static synchronized void initialize() {
        if (initialized || !ModList.get().isLoaded(MODID)) return;
        initialized = true;
        try {
            Item pearl = ForgeRegistries.ITEMS.getValue(ENCHANTER_PEARL);
            if (pearl == null) return;
            Method resolvedIsPresent = pearl.getClass().getMethod("isPresent", Player.class);
            Class<?> handler = Class.forName("com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler");
            Method resolvedMergeEnchantments = handler.getMethod(
                "mergeEnchantments", ItemStack.class, ItemStack.class, boolean.class, boolean.class);
            Method resolvedMaybeApplyEternalBinding = handler.getMethod("maybeApplyEternalBinding", ItemStack.class);
            isPresent = resolvedIsPresent;
            mergeEnchantments = resolvedMergeEnchantments;
            maybeApplyEternalBinding = resolvedMaybeApplyEternalBinding;
            available = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            ApotheosisArtificeMod.LOGGER.warn("Failed to initialize Enigmatic Legacy compatibility", e);
        }
    }
}
