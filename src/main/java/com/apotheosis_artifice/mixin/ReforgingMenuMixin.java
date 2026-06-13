package com.apotheosis_artifice.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.AffixTypes;
import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ISlotSelectMenu;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;

@Mixin(value = ReforgingMenu.class, remap = false)
public abstract class ReforgingMenuMixin implements ISlotSelectMenu {

    @Shadow
    private InternalItemHandler itemInv;
    @Shadow
    private InternalItemHandler choicesInv;
    @Shadow
    private RandomSource random;
    @Shadow
    private int seed;
    @Shadow
    private int[] costs;
    @Shadow
    private net.minecraft.world.entity.player.Player player;

    @Unique
    private int curiosforge_selectedSlotIdx = 0;

    /** 完整类别 ID 列表：原生 LootCategory 名称（如 "chestplate"）+ "curio:槽位名" */
    @Unique
    private List<String> curiosforge_availableSlots = List.of();

    public List<String> curiosforge_getAvailableSlots() {
        return this.curiosforge_availableSlots;
    }

    /**
     * 检测物品可用的所有重铸类别：
     * 1. 原生 LootCategory（如 chestplate、leggings）
     * 2. Curios 专属槽位（如 curio:charm、curio:back）
     */
    @Unique
    private void curiosforge_detectSlots(ItemStack input) {
        List<String> cats = new ArrayList<>();

        // 原生 LootCategory（绕过 curio_cat 干扰，取真实类别如 chestplate）
        LootCategory trueNative = curiosforge_getTrueNative(input);
        if (!trueNative.isNone() && !trueNative.getName().startsWith("curio")) {
            cats.add(trueNative.getName());
        }

        // Curios 专属槽位（仅添加有对应词缀的）
        var curiosSlots = CuriosApi.getSlots().keySet().stream()
            .filter(sid -> input.is(ItemTags.create(new ResourceLocation("curios:" + sid))))
            .sorted()
            .collect(Collectors.toList());
        curiosSlots.remove("curio");
        if (curiosSlots.isEmpty() && input.getCapability(CuriosCapability.ITEM).isPresent()) {
            curiosSlots = List.of("curio");
        }
        for (String slot : curiosSlots) {
            LootCategory cat = getOrCreateCurioCategory(slot);
            if (!cat.isNone() && hasAffixFor(input, cat)) {
                cats.add("curio:" + slot);
            }
        }

        this.curiosforge_availableSlots = List.copyOf(cats);
        if (this.curiosforge_selectedSlotIdx >= cats.size())
            this.curiosforge_selectedSlotIdx = 0;
    }

    /** 获取物品的真实原生 LootCategory（忽略 curio_cat 覆盖） */
    @Unique
    private static LootCategory curiosforge_getTrueNative(ItemStack stack) {
        ItemStack copy = stack.copy();
        var tag = copy.getTagElement(AffixHelper.AFFIX_DATA);
        if (tag != null && tag.contains("curio_cat")) {
            tag.remove("curio_cat");
            if (tag.isEmpty()) copy.removeTagKey(AffixHelper.AFFIX_DATA);
        }
        return LootCategory.forItem(copy);
    }

    /** 获取或创建 "curio:{slotName}" 对应的 LootCategory */
    @Unique
    private static LootCategory getOrCreateCurioCategory(String slotOrCatName) {
        String slotId = slotOrCatName.startsWith("curio:") ? slotOrCatName.substring(6) : slotOrCatName;
        String catName = "curio:" + slotId;
        LootCategory cat = LootCategory.byId(catName);
        if (cat == null || cat.isNone()) {
            cat = LootCategory.register(ApotheosisArtificeMod.CURIO, catName,
                s -> !s.isEmpty() && s.is(ItemTags.create(new ResourceLocation("curios:" + slotId))),
                new EquipmentSlot[]{EquipmentSlot.CHEST});
        }
        return cat;
    }

    public void curiosforge_selectSlot(int idx) {
        this.curiosforge_selectedSlotIdx = idx;
    }

    // ===== 输入槽验证：确保有词缀可用 =====

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/placebo/menu/PlaceboContainerMenu$UpdatingSlot;<init>(Lnet/minecraftforge/items/IItemHandler;IIILjava/util/function/Predicate;)V", remap = false), index = 4)
    private Predicate<ItemStack> enhanceSlotValidator(Predicate<ItemStack> original, IItemHandler handler) {
        if (handler == this.itemInv)
            return s -> original.test(s) && hasAffixFor(s);
        return original;
    }

    // ===== slotsChanged HEAD: 检测可用的所有类别 =====

    @Inject(method = "slotsChanged", at = @At("HEAD"), remap = true)
    private void curiosforge_updateSlots(net.minecraft.world.Container container, CallbackInfo ci) {
        ItemStack input = ((ReforgingMenu)(Object)this).getSlot(0).getItem();
        if (input.isEmpty()) { this.curiosforge_availableSlots = List.of(); return; }
        curiosforge_detectSlots(input);
    }

    // ===== slotsChanged TAIL: 按选中类别重造 =====

    @Inject(method = "slotsChanged", at = @At("TAIL"), remap = true)
    private void curiosforge_regenerate(net.minecraft.world.Container container, CallbackInfo ci) {
        ReforgingMenu menu = (ReforgingMenu)(Object)this;
        ItemStack input = menu.getSlot(0).getItem();
        if (input.isEmpty()) return;

        if (this.curiosforge_availableSlots.size() > 1) {
            // ===== 多栏位：TAIL 按选中类别重造 =====
            ItemStack mat = menu.getSlot(1).getItem();
            if (mat.isEmpty()) return;

            LootRarity rarity = RarityRegistry.getMaterialRarity(mat.getItem()).get();
            if (rarity == null) return;

            String catName = this.curiosforge_availableSlots.get(this.curiosforge_selectedSlotIdx);

            LootCategory cat = getOrCreateCurioCategory(catName);
            if (cat == null || cat.isNone()) return;

            boolean isCurio = catName.startsWith("curio:");
            if (isCurio) com.apotheosis_artifice.CatOverride.set(cat);
            try {
                for (int s = 0; s < 3; s++) {
                    RandomSource r = this.random;
                    r.setSeed(this.seed ^ ForgeRegistries.ITEMS.getKey(input.getItem()).hashCode() + s);
                    ItemStack result = LootController.createLootItem(input.copy(), cat, rarity, r);
                    if (isCurio) {
                        result.getOrCreateTagElement("affix_data").putString("curio_cat", cat.getName());
                    }
                    this.choicesInv.setStackInSlot(s, result);
                }
            } finally {
                if (isCurio) com.apotheosis_artifice.CatOverride.set(null);
            }
            menu.broadcastChanges();
        }
        // 任何情况下 cost 为 0 时，取所有已加载配方的最高 cost
        if (this.costs[0] == 0 && this.costs[1] == 0 && this.costs[2] == 0 && this.player != null && this.player.level() != null) {
            var allRecipes = this.player.level().getRecipeManager().getAllRecipesFor(dev.shadowsoffire.apotheosis.Apoth.RecipeTypes.REFORGING);
            int maxSigil = 0, maxMat = 0, maxLevel = 0;
            for (var r : allRecipes) {
                if (r.sigilCost() > maxSigil) maxSigil = r.sigilCost();
                if (r.matCost() > maxMat) maxMat = r.matCost();
                if (r.levelCost() > maxLevel) maxLevel = r.levelCost();
            }
            if (maxSigil > 0) this.costs[0] = maxSigil;
            if (maxMat > 0) this.costs[1] = maxMat;
            if (maxLevel > 0) this.costs[2] = maxLevel;
        }
        // 单栏位：不需写入 curio_cat，由 ApotheosisEvents 通过原生 LootCategory 判断绑定
    }

    /** 检查某物品在指定类别下是否有至少一个可用的词缀 */
    private static boolean hasAffixFor(ItemStack stack, LootCategory cat) {
        if (cat == null || cat.isNone()) return false;
        return AffixRegistry.INSTANCE.getValues().stream()
            .anyMatch(a -> {
                Set<LootCategory> types = ((AffixTypes) a).curiosforge_getTypes();
                return types.contains(cat) || AffixTypes.curiosforge_typeMatches(types, cat);
            });
    }

    /** 检查某物品是否有至少一个可用的词缀（使用物品默认的 LootCategory） */
    private static boolean hasAffixFor(ItemStack stack) {
        return hasAffixFor(stack, LootCategory.forItem(stack));
    }
}
