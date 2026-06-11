package com.apotheosis_artifice.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Level 1: 显示所有可重铸的 LootCategory。<br>
 * 每个分类自动扫描所有注册物品，JEI 自动循环切换展示。
 */
public record AffixCodexEntry(List<LootCategory> categories) {

    private static final Logger LOGGER = LogManager.getLogger("ApotheosisArtifice/JEI");

    @Nullable
    public static AffixCodexEntry create() {
        var orderedRarities = RarityRegistry.INSTANCE.getOrderedRarities();
        var registry = AffixRegistry.INSTANCE;

        var cats = LootCategory.VALUES.stream()
            .filter(c -> !c.isNone())
            .filter(c -> !"curio".equals(c.getName())) // 通用 curio 不单独显示，词缀自动归入各子槽位
            .filter(c -> {
                for (Affix a : registry.getValues()) {
                    for (DynamicHolder<LootRarity> holder : orderedRarities) {
                        if (!holder.isBound()) continue;
                        try {
                            if (a.canApplyTo(ItemStack.EMPTY, c, holder.get())) return true;
                        } catch (Exception e) { /* skip */ }
                    }
                }
                return false;
            })
            .sorted(Comparator.comparing(LootCategory::getName))
            .toList();

        return cats.isEmpty() ? null : new AffixCodexEntry(cats);
    }

    /** 全物品自动扫描结果 */
    private static final Map<String, List<ItemStack>> CATEGORY_ITEMS = new HashMap<>();

    static {
        // 第一遍：LootCategory.forItem 自动归类（通用 curio 会拦截多槽位物品）
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) continue;
            LootCategory cat = LootCategory.forItem(stack);
            if (cat != null && !cat.isNone()) {
                CATEGORY_ITEMS.computeIfAbsent(cat.getName(), k -> new ArrayList<>()).add(stack);
            }
        }
        // 第二遍：按 curios 标签直接匹配槽位（解决多槽位被通用 curio 拦截的问题）
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) continue;
            for (LootCategory cat : LootCategory.VALUES) {
                if (cat.isNone()) continue;
                String name = cat.getName();
                if (!name.startsWith("curio:") || CATEGORY_ITEMS.getOrDefault(name, List.of()).contains(stack)) continue;
                String slotId = name.substring(6);
                var tag = net.minecraft.tags.ItemTags.create(new ResourceLocation("curios:" + slotId));
                if (stack.is(tag)) {
                    CATEGORY_ITEMS.computeIfAbsent(name, k -> new ArrayList<>()).add(stack);
                }
            }
        }
        // 扫描结果日志 + 空分类兜底
        for (LootCategory cat : LootCategory.VALUES) {
            if (cat.isNone()) continue;
            String name = cat.getName();
            List<ItemStack> list = CATEGORY_ITEMS.get(name);
            if (list == null || list.isEmpty()) {
                if (name.startsWith("curio:") && CATEGORY_ITEMS.containsKey("curio")) {
                    CATEGORY_ITEMS.put(name, CATEGORY_ITEMS.get("curio"));
                    LOGGER.warn("[{}] empty, borrowed {} from [curio]", name, CATEGORY_ITEMS.get("curio").size());
                } else {
                    LOGGER.warn("[{}] empty, using BARRIER", name);
                    CATEGORY_ITEMS.put(name, List.of(new ItemStack(Items.BARRIER)));
                }
            } else {
                LOGGER.info("[{}] {} items", name, list.size());
            }
        }
    }

    public static List<ItemStack> getCategoryItems(LootCategory cat) {
        return CATEGORY_ITEMS.getOrDefault(cat.getName(), List.of(new ItemStack(Items.BARRIER)));
    }
}
