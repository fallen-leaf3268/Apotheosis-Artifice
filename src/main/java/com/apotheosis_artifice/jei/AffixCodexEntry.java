package com.apotheosis_artifice.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisConfig;
import com.apotheosis_artifice.compat.BetterCombatCompat;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public record AffixCodexEntry(List<LootCategory> categories, Map<LootCategory, List<ItemStack>> items) {

    @Nullable
    public AffixCodexEntry withoutCurios() {
        var nativeCategories = categories.stream()
            .filter(c -> !c.getName().equals("curio") && !c.getName().startsWith("curios:"))
            .toList();
        return nativeCategories.isEmpty() ? null : new AffixCodexEntry(nativeCategories, items);
    }

    @Nullable
    public static AffixCodexEntry create(List<AffixDetailEntry> entries) {
        Map<LootCategory, LinkedHashSet<ItemStack>> supported = new HashMap<>();
        for (AffixDetailEntry entry : entries) {
            LootCategory cat = entry.category();
            if (cat.getName().startsWith("curios:")) cat = LootCategory.byId("curio");
            if (cat == null || cat.isNone()) continue;
            supported.computeIfAbsent(cat, key -> new LinkedHashSet<>()).addAll(entry.items());
        }
        Map<LootCategory, List<ItemStack>> items = new HashMap<>();
        supported.forEach((cat, stacks) -> {
            if (!stacks.isEmpty()) items.put(cat, List.copyOf(stacks));
        });
        var cats = items.keySet().stream().sorted(Comparator.comparing(LootCategory::getName)).toList();
        return cats.isEmpty() ? null : new AffixCodexEntry(cats, Map.copyOf(items));
    }

    private static final Map<String, List<ItemStack>> CATEGORY_ITEMS = new HashMap<>();

    private static volatile boolean initialized = false;

    private static volatile mezz.jei.api.runtime.IIngredientManager ingredientManager;

    public static synchronized void bindIngredientManager(mezz.jei.api.runtime.IIngredientManager manager) {
        ingredientManager = manager;
        invalidate();
    }

    public static synchronized void invalidate() {
        initialized = false;
        CATEGORY_ITEMS.clear();
    }

    public static synchronized void unbindIngredientManager() {
        ingredientManager = null;
        invalidate();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        var slotHelper = top.theillusivec4.curios.api.CuriosApi.getSlotHelper();
        if (slotHelper == null) return;
        var slotIds = List.copyOf(slotHelper.getSlotTypeIds());
        for (String slotId : slotIds) {
            String catName = "curios:" + slotId;
            if (LootCategory.byId(catName) == null || LootCategory.byId(catName).isNone()) {
                LootCategory.register(null, catName,
                    s -> !s.isEmpty() && s.is(net.minecraft.tags.ItemTags.create(new ResourceLocation("curios:" + slotId))),
                    new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.CHEST});
            }
        }

        java.util.Collection<ItemStack> universe;
        var manager = ingredientManager;
        if (manager != null) {
            universe = manager.getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK);
        } else {
            List<ItemStack> all = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS) {
                ItemStack s = new ItemStack(item);
                if (!s.isEmpty()) all.add(s);
            }
            universe = all;
        }

        var nativeCategories = LootCategory.VALUES.stream()
            .filter(cat -> !cat.isNone() && !cat.getName().equals("curio") && !cat.getName().startsWith("curios:"))
            .toList();
        LootCategory genericCurio = LootCategory.byId("curio");
        for (ItemStack ingredient : universe) {
            if (ingredient.isEmpty()) continue;
            ItemStack stack = ingredient.copy();

            LootCategory nativeCat = LootCategory.NONE;
            for (LootCategory c : nativeCategories) {
                if (c.isValid(stack)) { nativeCat = c; break; }
            }

            List<String> matchedCurioSlots = new ArrayList<>();
            for (String slotId : slotIds) {
                String catName = "curios:" + slotId;
                LootCategory lc = LootCategory.byId(catName);
                if (lc == null || lc.isNone()) continue;
                var tag = net.minecraft.tags.ItemTags.create(new ResourceLocation("curios:" + slotId));
                if (stack.is(tag)) {
                    matchedCurioSlots.add(catName);
                }
            }

            if (!matchedCurioSlots.isEmpty() || (genericCurio != null && !genericCurio.isNone() && genericCurio.isValid(stack))) {
                    for (String slotCat : matchedCurioSlots) {
                        CATEGORY_ITEMS.computeIfAbsent(slotCat, k -> new ArrayList<>()).add(stack);
                    }
                    CATEGORY_ITEMS.computeIfAbsent("curio", k -> new ArrayList<>()).add(stack);
                if (!nativeCat.isNone()) {
                    CATEGORY_ITEMS.computeIfAbsent(nativeCat.getName(), k -> new ArrayList<>()).add(stack);
                }
            } else if (!nativeCat.isNone()) {
                if (ApotheosisConfig.USE_BETTERCOMBAT_HEAVY_OVERRIDE.get()
                    && (nativeCat == LootCategory.SWORD || nativeCat == LootCategory.TRIDENT)
                    && BetterCombatCompat.isTwoHanded(stack)) {
                    CATEGORY_ITEMS.computeIfAbsent("heavy_weapon", k -> new ArrayList<>()).add(stack);
                } else {
                    CATEGORY_ITEMS.computeIfAbsent(nativeCat.getName(), k -> new ArrayList<>()).add(stack);
                }
            }
        }
        CATEGORY_ITEMS.replaceAll((name, stacks) -> List.copyOf(stacks));
        initialized = true;
    }

    public static synchronized List<ItemStack> getCategoryItems(LootCategory cat) {
        ensureInit();
        return CATEGORY_ITEMS.getOrDefault(cat.getName(), List.of());
    }

    public static synchronized List<LootCategory> getCategories() {
        ensureInit();
        return List.copyOf(LootCategory.VALUES);
    }
}
