package com.apotheosis_artifice.compat;

import java.lang.reflect.Method;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class EnigmaticLegacyCompat {

    private static final String MODID = "enigmaticlegacy";
    private static final ResourceLocation ENCHANTER_PEARL = new ResourceLocation(MODID, "enchanter_pearl");
    private static boolean initialized;
    private static boolean available;
    private static Method isPresent;

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

    public static TableStats enableTreasure(TableStats stats, Player player) {
        if (stats == null || stats.treasure() || !isEnchanterPearlActive(player)) return stats;
        return new TableStats(
            stats.eterna(), stats.quanta(), stats.arcana(),
            stats.rectification(), stats.clues(), stats.blacklist(), true);
    }

    private static synchronized void initialize() {
        if (initialized || !ModList.get().isLoaded(MODID)) return;
        initialized = true;
        try {
            Item pearl = ForgeRegistries.ITEMS.getValue(ENCHANTER_PEARL);
            if (pearl == null) return;
            isPresent = pearl.getClass().getMethod("isPresent", Player.class);
            available = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            ApotheosisArtificeMod.LOGGER.warn("Failed to initialize Enigmatic Legacy compatibility", e);
        }
    }
}
