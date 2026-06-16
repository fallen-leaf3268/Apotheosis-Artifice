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
            .filter(c -> {
                for (Affix a : registry.getValues()) {
                    for (DynamicHolder<LootRarity> holder : orderedRarities) {
                        if (!holder.isBound()) continue;
                        try {
                            if (a.canApplyTo(ItemStack.EMPTY, c, holder.get())) return true;
                            // 通用 curio 页面也显示所有 curio:xxx 子槽位的词缀
                            if ("curio".equals(c.getName())) {
                                for (LootCategory sub : LootCategory.VALUES) {
                                    if (!sub.isNone() && sub.getName().startsWith("curio:")
                                        && a.canApplyTo(ItemStack.EMPTY, sub, holder.get())) return true;
                                }
                            }
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
        // 为所有已注册的 Curios 槽位创建 LootCategory（确保法术书等能独立显示）
        for (String slotId : top.theillusivec4.curios.api.CuriosApi.getSlotHelper().getSlotTypeIds()) {
            String catName = "curio:" + slotId;
            if (LootCategory.byId(catName) == null || LootCategory.byId(catName).isNone()) {
                LootCategory.register(null, catName,
                    s -> !s.isEmpty() && s.is(net.minecraft.tags.ItemTags.create(new ResourceLocation("curios:" + slotId))),
                    new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.CHEST});
            }
        }

        // 遍历所有物品，统计每个物品的饰品槽位数量
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) continue;

            // 手动找原生非 curio 分类（绕过注册顺序影响）
            LootCategory nativeCat = LootCategory.NONE;
            for (LootCategory c : LootCategory.VALUES) {
                if (c.isNone() || c.getName().startsWith("curio")) continue;
                if (c.isValid(stack)) { nativeCat = c; break; }
            }

            // 收集该物品匹配的所有 curio 槽位
            List<String> matchedCurioSlots = new ArrayList<>();
            for (String slotId : top.theillusivec4.curios.api.CuriosApi.getSlotHelper().getSlotTypeIds()) {
                String catName = "curio:" + slotId;
                LootCategory lc = LootCategory.byId(catName);
                if (lc == null || lc.isNone()) continue;
                var tag = net.minecraft.tags.ItemTags.create(new ResourceLocation("curios:" + slotId));
                if (stack.is(tag)) {
                    matchedCurioSlots.add(catName);
                }
            }

            if (!matchedCurioSlots.isEmpty()) {
                if (matchedCurioSlots.size() == 1) {
                    CATEGORY_ITEMS.computeIfAbsent(matchedCurioSlots.get(0), k -> new ArrayList<>()).add(stack);
                } else {
                    CATEGORY_ITEMS.computeIfAbsent("curio", k -> new ArrayList<>()).add(stack);
                }
                // 双类别物品（如胸甲+背饰）：也加入原生非 curio 类别
                if (nativeCat != null && !nativeCat.isNone() && !nativeCat.getName().startsWith("curio")) {
                    CATEGORY_ITEMS.computeIfAbsent(nativeCat.getName(), k -> new ArrayList<>()).add(stack);
                }
            } else if (nativeCat != null && !nativeCat.isNone()) {
                CATEGORY_ITEMS.computeIfAbsent(nativeCat.getName(), k -> new ArrayList<>()).add(stack);
            }
        }
        // 扫描结果日志 + 空分类兜底
        for (LootCategory cat : LootCategory.VALUES) {
            if (cat.isNone()) continue;
            String name = cat.getName();
            List<ItemStack> list = CATEGORY_ITEMS.get(name);
            if (list == null || list.isEmpty()) {
                LOGGER.warn("[{}] empty, using BARRIER", name);
                CATEGORY_ITEMS.put(name, List.of(new ItemStack(Items.BARRIER)));
            } else {
                LOGGER.info("[{}] {} items", name, list.size());
            }
        }
    }

    public static List<ItemStack> getCategoryItems(LootCategory cat) {
        return CATEGORY_ITEMS.getOrDefault(cat.getName(), List.of(new ItemStack(Items.BARRIER)));
    }
}
