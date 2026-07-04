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
        ensureInit();
        var orderedRarities = RarityRegistry.INSTANCE.getOrderedRarities();
        var registry = AffixRegistry.INSTANCE;

        var cats = LootCategory.VALUES.stream()
            .filter(c -> !c.isNone())
            .filter(c -> !c.getName().startsWith("curios:"))
            .filter(c -> {
                // 没有实际物品的分类不显示
                List<ItemStack> catItems = getCategoryItems(c);
                boolean hasRealItems = catItems.stream().anyMatch(s -> !s.is(Items.BARRIER));
                if (!hasRealItems) return false;
                for (Affix a : registry.getValues()) {
                    for (DynamicHolder<LootRarity> holder : orderedRarities) {
                        if (!holder.isBound()) continue;
                        try {
                            if (a.canApplyTo(ItemStack.EMPTY, c, holder.get())) return true;
                            // 通用 curio 页面也显示所有 curios:xxx 子槽位的词缀
                            if ("curio".equals(c.getName())) {
                                for (LootCategory sub : LootCategory.VALUES) {
                                    if (!sub.isNone() && sub.getName().startsWith("curios:")
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

    private static volatile boolean initialized = false;

    /**
     * JEI 的物品变体列表（含 NBT 变体，例如 Iron's Spellbooks 每个法术一支的卷轴）。
     * 由 JEI 插件在 registerRecipes 时绑定；扫描优先用它，这样带 NBT 才有意义的物品
     * （法术卷轴等）会以全部变体进入类别列表，JEI 槽位自动轮播展示。
     */
    private static volatile mezz.jei.api.runtime.IIngredientManager ingredientManager;

    public static synchronized void bindIngredientManager(mezz.jei.api.runtime.IIngredientManager manager) {
        if (manager != null && ingredientManager != manager) {
            ingredientManager = manager;
            // JEI 重载（换资源包/重连）会带来新的 manager，重扫一遍
            initialized = false;
            CATEGORY_ITEMS.clear();
        }
    }

    /**
     * 惰性、幂等的一次性初始化。原先在 static 块里执行：类加载期就调用
     * {@code CuriosApi.getSlotHelper()}（可能为 null）并修改全局 LootCategory 注册表，
     * 一旦 NPE 就用 ExceptionInInitializerError 污染整个类、后续 JEI 全挂。
     * 改为由 registerRecipes / getCategoryItems 触发，并对 null 做守卫、未就绪则下次再试。
     */
    private static synchronized void ensureInit() {
        if (initialized) return;
        var slotHelper = top.theillusivec4.curios.api.CuriosApi.getSlotHelper();
        if (slotHelper == null) return;
        // 为所有已注册的 Curios 槽位创建 LootCategory（确保法术书等能独立显示）
        for (String slotId : slotHelper.getSlotTypeIds()) {
            String catName = "curios:" + slotId;
            if (LootCategory.byId(catName) == null || LootCategory.byId(catName).isNone()) {
                LootCategory.register(null, catName,
                    s -> !s.isEmpty() && s.is(net.minecraft.tags.ItemTags.create(new ResourceLocation("curios:" + slotId))),
                    new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.CHEST});
            }
        }

        // 物品来源：优先 JEI 物品变体列表（含 NBT 变体），JEI 未就绪时退回注册表裸物品
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

        // 遍历所有物品（含变体），统计每个物品的饰品槽位数量
        for (ItemStack stack : universe) {
            if (stack.isEmpty()) continue;

            // 手动找原生非 curio 分类（绕过注册顺序影响）
            LootCategory nativeCat = LootCategory.NONE;
            for (LootCategory c : LootCategory.VALUES) {
                if (c.isNone() || c.getName().equals("curio") || c.getName().startsWith("curios:")) continue;
                if (c.isValid(stack)) { nativeCat = c; break; }
            }

            // 收集该物品匹配的所有 curio 槽位
            List<String> matchedCurioSlots = new ArrayList<>();
            for (String slotId : slotHelper.getSlotTypeIds()) {
                String catName = "curios:" + slotId;
                LootCategory lc = LootCategory.byId(catName);
                if (lc == null || lc.isNone()) continue;
                var tag = net.minecraft.tags.ItemTags.create(new ResourceLocation("curios:" + slotId));
                if (stack.is(tag)) {
                    matchedCurioSlots.add(catName);
                }
            }

            if (!matchedCurioSlots.isEmpty()) {
                    // 多槽位物品：同时加入每个具体槽位 + 通用 curio
                    for (String slotCat : matchedCurioSlots) {
                        CATEGORY_ITEMS.computeIfAbsent(slotCat, k -> new ArrayList<>()).add(stack);
                    }
                    CATEGORY_ITEMS.computeIfAbsent("curio", k -> new ArrayList<>()).add(stack);
                // 双类别物品（如胸甲+背饰）：也加入原生非 curio 类别
                if (nativeCat != null && !nativeCat.isNone() && !nativeCat.getName().equals("curio") && !nativeCat.getName().startsWith("curios:")) {
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
        initialized = true;
    }

    public static List<ItemStack> getCategoryItems(LootCategory cat) {
        ensureInit();
        return CATEGORY_ITEMS.getOrDefault(cat.getName(), List.of(new ItemStack(Items.BARRIER)));
    }
}
