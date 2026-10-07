package com.apotheosis_artifice.compat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.enchant.MechanicalRavenEnchantMenu;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class EasyMagicCompat {

    private static final TagKey<Item> REROLL_CATALYSTS = TagKey.create(Registries.ITEM, new ResourceLocation("easymagic", "reroll_catalysts"));
    private static final TagKey<Item> ENCHANTING_CATALYSTS = TagKey.create(Registries.ITEM, new ResourceLocation("easymagic", "enchanting_catalysts"));
    private static volatile boolean available = ModList.get().isLoaded("easymagic");
    private static volatile boolean configAccessResolved;
    private static boolean warned;
    private static Class<?> serverConfigClass;
    private static Field configHolderField;
    private static Method configGetter;
    private static final Map<String, Optional<Field>> configFields = new ConcurrentHashMap<>();

    private EasyMagicCompat() {}

    public static boolean isLoaded() {
        if (available) resolveConfigAccess();
        return available;
    }

    public static boolean rerollEnchantments() {
        return getBoolean("rerollEnchantments", true);
    }

    public static boolean lenientBookshelves() {
        if (!isLoaded()) return false;
        boolean enabled = getBoolean("lenientBookshelves", true);
        return available && enabled;
    }

    public static int rerollCatalystCost() {
        return getInt("rerollCatalystCost", 1);
    }

    public static int rerollCatalystCost(Player player) {
        return rerollCatalystCost(player, dedicatedRerollButton());
    }

    public static int rerollCatalystCost(ApothEnchantmentMenu menu, Player player) {
        return rerollCatalystCost(player, getDedicatedRerollSlot(menu) != null);
    }

    private static int rerollCatalystCost(Player player, boolean dedicated) {
        int configuredCost = rerollCatalystCost();
        if (!dedicated && EnigmaticLegacyCompat.isEnchanterPearlActive(player)) return 0;
        return configuredCost;
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
        Slot dedicated = getDedicatedRerollSlot(menu);
        return (dedicated == null ? menu.getSlot(1) : dedicated).getItem().getCount();
    }

    public static Slot getDedicatedRerollSlot(ApothEnchantmentMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container == menu.enchantSlots && slot.getContainerSlot() == 2) return slot;
        }
        return null;
    }

    public static boolean tryReroll(ApothEnchantmentMenu menu, Player player) {
        if (!canUseReroll(menu)) return false;
        Slot dedicated = getDedicatedRerollSlot(menu);
        Slot catalystSlot = dedicated == null ? menu.getSlot(1) : dedicated;
        int catalystCost = rerollCatalystCost(player, dedicated != null);
        int experienceCost = rerollExperienceCost();
        if (!player.getAbilities().instabuild
            && (getTotalExperience(player) < experienceCost || catalystSlot.getItem().getCount() < catalystCost)) return false;
        if (player.level().isClientSide) return true;

        ItemStack input = menu.enchantSlots.getItem(0);
        player.onEnchantmentPerformed(input, 0);
        int newSeed = player.getEnchantmentSeed();
        menu.enchantmentSeed.set(newSeed);
        if (menu instanceof MechanicalRavenEnchantMenu mechanical) {
            mechanical.persistEnchantmentSeed(newSeed);
        }
        if (!player.getAbilities().instabuild) {
            if (catalystCost > 0) {
                ItemStack catalyst = catalystSlot.getItem();
                catalyst.shrink(catalystCost);
                if (catalyst.isEmpty()) catalystSlot.set(ItemStack.EMPTY);
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
        if (!isLoaded()) return null;
        Field field = configFields.computeIfAbsent(fieldName, name -> {
            try {
                return Optional.of(serverConfigClass.getField(name));
            } catch (NoSuchFieldException exception) {
                warnConfigRead(exception);
                return Optional.empty();
            }
        }).orElse(null);
        if (field == null) return null;
        try {
            Object holder = configHolderField.get(null);
            if (holder == null) return null;
            Object config = configGetter.invoke(holder, serverConfigClass);
            return config == null ? null : field.get(config);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Error error) throw error;
            warnConfigRead(exception);
            return null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            warnConfigRead(exception);
            return null;
        }
    }

    private static synchronized void resolveConfigAccess() {
        if (configAccessResolved || !available) return;
        try {
            ClassLoader loader = EasyMagicCompat.class.getClassLoader();
            Class<?> easyMagic = Class.forName("fuzs.easymagic.EasyMagic", false, loader);
            Class<?> configType = Class.forName("fuzs.easymagic.config.ServerConfig", false, loader);
            Field holder = easyMagic.getField("CONFIG");
            Method getter = holder.getType().getMethod("get", Class.class);
            serverConfigClass = configType;
            configHolderField = holder;
            configGetter = getter;
        } catch (ReflectiveOperationException | LinkageError exception) {
            available = false;
            warnConfigRead(exception);
        } finally {
            configAccessResolved = true;
        }
    }

    private static synchronized void warnConfigRead(Throwable exception) {
        if (!warned) {
            warned = true;
            ApotheosisArtificeMod.LOGGER.warn("Easy Magic configuration could not be read; unavailable values use defaults", exception);
        }
    }
}
