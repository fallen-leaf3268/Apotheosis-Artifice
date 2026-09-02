package com.apotheosis_artifice.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class EasyMagicCompat {

    private static final TagKey<Item> REROLL_CATALYSTS = TagKey.create(Registries.ITEM, new ResourceLocation("easymagic", "reroll_catalysts"));
    private static final TagKey<Item> ENCHANTING_CATALYSTS = TagKey.create(Registries.ITEM, new ResourceLocation("easymagic", "enchanting_catalysts"));
    private static boolean available = ModList.get().isLoaded("easymagic");
    private static boolean warned;

    private EasyMagicCompat() {}

    public static boolean isLoaded() {
        return available;
    }

    public static boolean rerollEnchantments() {
        return getBoolean("rerollEnchantments", true);
    }

    public static int rerollCatalystCost() {
        return getInt("rerollCatalystCost", 1);
    }

    public static int rerollExperienceCost() {
        return getInt("rerollExperiencePointsCost", 5);
    }

    public static boolean dedicatedRerollButton() {
        return getBoolean("dedicatedRerollCatalyst", false);
    }

    public static boolean isRerollCatalyst(ItemStack stack) {
        return !stack.isEmpty() && stack.is(REROLL_CATALYSTS);
    }

    public static boolean isEnchantingCatalyst(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ENCHANTING_CATALYSTS);
    }

    public static boolean canUseReroll(ApothEnchantmentMenu menu) {
        if (!isLoaded() || !rerollEnchantments()) return false;
        ItemStack input = menu.enchantSlots.getItem(0);
        if (input.isEmpty() || !input.isEnchantable()) return false;
        for (int cost : menu.costs) {
            if (cost > 0) return true;
        }
        return false;
    }

    public static int getRerollCatalystCount(ApothEnchantmentMenu menu) {
        if (dedicatedRerollButton()) return menu.enchantSlots.getItem(2).getCount();
        return menu.getSlot(1).getItem().getCount();
    }

    public static boolean tryReroll(ApothEnchantmentMenu menu, Player player) {
        if (!canUseReroll(menu)) return false;
        int catalystCost = rerollCatalystCost();
        int experienceCost = rerollExperienceCost();
        if (!player.getAbilities().instabuild
            && (getTotalExperience(player) < experienceCost || getRerollCatalystCount(menu) < catalystCost)) return false;
        if (player.level().isClientSide) return true;

        ItemStack input = menu.enchantSlots.getItem(0);
        player.onEnchantmentPerformed(input, 0);
        menu.enchantmentSeed.set(player.getEnchantmentSeed());
        if (!player.getAbilities().instabuild) {
            if (catalystCost > 0) {
                if (dedicatedRerollButton()) {
                    ItemStack catalyst = menu.enchantSlots.getItem(2);
                    catalyst.shrink(catalystCost);
                    if (catalyst.isEmpty()) menu.enchantSlots.setItem(2, ItemStack.EMPTY);
                } else {
                    ItemStack catalyst = menu.getSlot(1).getItem();
                    catalyst.shrink(catalystCost);
                    if (catalyst.isEmpty()) menu.getSlot(1).set(ItemStack.EMPTY);
                }
            }
            if (experienceCost > 0) player.giveExperiencePoints(-experienceCost);
        }
        menu.enchantSlots.setChanged();
        menu.slotsChanged(menu.enchantSlots);
        return true;
    }

    public static int getTotalExperience(Player player) {
        int level = player.experienceLevel;
        int fromLevels;
        if (level < 17) fromLevels = level * level + 6 * level;
        else if (level < 32) fromLevels = (int) (2.5F * level * level - 40.5F * level + 360);
        else fromLevels = (int) (4.5F * level * level - 162.5F * level + 2220);
        return fromLevels + (int) (player.getXpNeededForNextLevel() * player.experienceProgress);
    }

    private static boolean getBoolean(String field, boolean fallback) {
        Object value = getConfigField(field);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static int getInt(String field, int fallback) {
        Object value = getConfigField(field);
        return value instanceof Number number ? Math.max(0, number.intValue()) : fallback;
    }

    private static Object getConfigField(String fieldName) {
        if (!available) return null;
        try {
            Class<?> easyMagic = Class.forName("fuzs.easymagic.EasyMagic");
            Class<?> serverConfig = Class.forName("fuzs.easymagic.config.ServerConfig");
            Field configField = easyMagic.getField("CONFIG");
            Object configHolder = configField.get(null);
            Method get = configHolder.getClass().getMethod("get", Class.class);
            Object config = get.invoke(configHolder, serverConfig);
            return serverConfig.getField(fieldName).get(config);
        } catch (ReflectiveOperationException | LinkageError ex) {
            available = false;
            if (!warned) {
                warned = true;
                ApotheosisArtificeMod.LOGGER.warn("Easy Magic compatibility was disabled because its config could not be read", ex);
            }
            return null;
        }
    }
}
