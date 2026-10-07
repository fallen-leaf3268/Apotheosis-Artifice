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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.AffixTypes;
import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ApotheosisConfig;
import com.apotheosis_artifice.CatOverride;
import com.apotheosis_artifice.ISlotSelectMenu;
import com.apotheosis_artifice.enchant.ApotheosisArtificeReforgingTableBlock;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;

@Mixin(value = ReforgingMenu.class, remap = false)
public abstract class ReforgingMenuMixin implements ISlotSelectMenu {

    @Shadow
    private int[] costs;
    @Unique
    private int curiosforge_selectedSlotIdx = 0;

    @Unique
    private List<String> curiosforge_availableSlots = List.of();

    @Unique
    private boolean curiosforge_artificeGui;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void curiosforge_captureTable(int id, Inventory inventory, BlockPos pos, CallbackInfo ci) {
        this.curiosforge_artificeGui = inventory.player.level().getBlockState(pos).getBlock()
            instanceof ApotheosisArtificeReforgingTableBlock;
    }

    @Override
    public boolean curiosforge_usesArtificeGui() {
        return this.curiosforge_artificeGui;
    }

    public List<String> curiosforge_getAvailableSlots() {
        return this.curiosforge_availableSlots;
    }

    @Unique
    private void curiosforge_detectSlots(ItemStack input) {
        List<String> cats = new ArrayList<>();

        LootCategory trueNative = CatOverride.forNativeItem(input);
        if (curiosforge_isNativeReforgingCategory(trueNative)) {
            cats.add(trueNative.getName());
        }
        if (!ApotheosisConfig.isCuriosReforgingEnabled()) {
            this.curiosforge_availableSlots = List.copyOf(cats);
            if (this.curiosforge_selectedSlotIdx >= cats.size()) this.curiosforge_selectedSlotIdx = 0;
            return;
        }

        LootRarity rarity = null;
        var matSlot = ((ReforgingMenu)(Object)this).getSlot(1).getItem();
        if (!matSlot.isEmpty()) {
            var dh = RarityRegistry.getMaterialRarity(matSlot.getItem());
            if (dh.isBound()) rarity = dh.get();
        }

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
            boolean has = rarity == null ? hasAffixFor(input, cat) : curiosforge_hasAffixForRarity(input, cat, rarity);
            if (!cat.isNone() && has) {
                cats.add("curios:" + slot);
            }
        }

        this.curiosforge_availableSlots = List.copyOf(cats);
        if (this.curiosforge_selectedSlotIdx >= cats.size())
            this.curiosforge_selectedSlotIdx = 0;
    }

    @Unique
    private static LootCategory getOrCreateCurioCategory(String slotOrCatName) {
        String slotId = slotOrCatName.startsWith("curios:") ? slotOrCatName.substring(7) : slotOrCatName;
        String catName = "curios:" + slotId;
        LootCategory cat = LootCategory.byId(catName);
        if (cat == null || cat.isNone()) {
            cat = LootCategory.register(ApotheosisArtificeMod.CURIO, catName,
                s -> !s.isEmpty() && s.is(ItemTags.create(new ResourceLocation("curios:" + slotId))),
                new EquipmentSlot[]{EquipmentSlot.CHEST});
        }
        return cat;
    }

    public void curiosforge_selectSlot(int idx) {
        this.curiosforge_selectedSlotIdx = Math.max(0, idx);
    }

    @Unique
    private static boolean curiosforge_isNativeReforgingCategory(LootCategory category) {
        return category != null && !category.isNone() && !category.getName().equals("curio")
            && !category.getName().startsWith("curios:");
    }

    @Unique
    private String curiosforge_selectedCategoryName() {
        if (this.curiosforge_availableSlots.isEmpty()) return null;
        int selected = this.curiosforge_selectedSlotIdx;
        if (selected < 0 || selected >= this.curiosforge_availableSlots.size()) selected = 0;
        return this.curiosforge_availableSlots.get(selected);
    }

    @Inject(method = "getRarity", at = @At("RETURN"), remap = false, cancellable = true)
    private void curiosforge_rejectDisabledCurioResults(CallbackInfoReturnable<LootRarity> cir) {
        if (ApotheosisConfig.isCuriosReforgingEnabled()) return;
        ItemStack input = ((ReforgingMenu)(Object)this).getSlot(0).getItem();
        String selected = curiosforge_selectedCategoryName();
        if (!curiosforge_isNativeReforgingCategory(CatOverride.forNativeItem(input))
            || selected != null && (selected.equals("curio") || selected.startsWith("curios:"))) {
            cir.setReturnValue(null);
        }
    }

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/adventure/affix/reforging/ReforgingMenu$1;<init>(Ldev/shadowsoffire/apotheosis/adventure/affix/reforging/ReforgingMenu;Ldev/shadowsoffire/placebo/cap/InternalItemHandler;IIILjava/util/function/Predicate;)V",
            remap = false),
        index = 5)
    private Predicate<ItemStack> enhanceSlotValidator(Predicate<ItemStack> original) {
        return s -> {
            if (!original.test(s)) return false;
            if (ApotheosisConfig.isCuriosReforgingEnabled()) return hasAffixFor(s);
            LootCategory nativeCategory = CatOverride.forNativeItem(s);
            return curiosforge_isNativeReforgingCategory(nativeCategory) && hasAffixFor(s, nativeCategory);
        };
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "slotsChanged", remap = true,
        at = @At(value = "INVOKE", remap = false,
            target = "Ldev/shadowsoffire/apotheosis/adventure/loot/LootController;createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack curiosforge_safeCreateLootItem(ItemStack stack, LootRarity rarity, RandomSource rand) {
        LootCategory previous = com.apotheosis_artifice.CatOverride.get();
        boolean previousReforging = CatOverride.isReforging();
        try {
            CatOverride.setReforging(true);
            String categoryName = null;
            String cat = curiosforge_selectedCategoryName();
            if (!ApotheosisConfig.isCuriosReforgingEnabled()
                && (cat == null || !curiosforge_isNativeReforgingCategory(CatOverride.forNativeItem(stack))
                    || cat.equals("curio") || cat.startsWith("curios:"))) return ItemStack.EMPTY;
            if (cat != null) {
                LootCategory category = cat.startsWith("curios:") ? getOrCreateCurioCategory(cat) : LootCategory.byId(cat);
                if (category == null || category.isNone()) return ItemStack.EMPTY;
                if (!ApotheosisConfig.isCuriosReforgingEnabled() && !curiosforge_isNativeReforgingCategory(category)) return ItemStack.EMPTY;
                com.apotheosis_artifice.CatOverride.set(category);
                if (this.curiosforge_availableSlots.size() > 1) {
                    categoryName = cat;
                }
                var affixData = stack.getTagElement("affix_data");
                if (categoryName != null || cat.equals("curio") || cat.startsWith("curios:")
                    || affixData != null && affixData.contains("curio_artifice")) {
                    categoryName = cat;
                    stack.getOrCreateTagElement("affix_data").putString("curio_artifice", cat);
                }
            }
            if (ApotheosisConfig.CLEAR_SOCKETS_ON_RARITY_CHANGE.get()) {
                var oldRarity = dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper.getRarity(stack);
                if (!oldRarity.isBound() || oldRarity.get() != rarity) {
                    dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper.setSockets(stack, 0);
                }
            }
            ItemStack result = LootController.createLootItem(stack, rarity, rand);
            if (categoryName != null && !result.isEmpty()) {
                result.getOrCreateTagElement("affix_data").putString("curio_artifice", categoryName);
            }
            return result;
        } catch (RuntimeException e) {
            ApotheosisArtificeMod.LOGGER.warn("Reforge preview roll failed for {} at rarity {}",
                stack.getItem(), rarity, e);
            return ItemStack.EMPTY;
        } finally {
            com.apotheosis_artifice.CatOverride.set(previous);
            CatOverride.setReforging(previousReforging);
        }
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"), remap = true)
    private void curiosforge_updateSlots(net.minecraft.world.Container container, CallbackInfo ci) {
        java.util.Arrays.fill(this.costs, 0);
        ItemStack input = ((ReforgingMenu)(Object)this).getSlot(0).getItem();
        if (input.isEmpty()) { this.curiosforge_availableSlots = List.of(); return; }
        curiosforge_detectSlots(input);
    }

    private static boolean hasAffixFor(ItemStack stack, LootCategory cat) {
        if (cat == null || cat.isNone()) return false;
        for (var a : AffixRegistry.INSTANCE.getValues()) {
            if (a instanceof AffixTypes af) {
                Set<LootCategory> types = af.curiosforge_getTypes();
                if (types.contains(cat) || AffixTypes.curiosforge_typeMatches(types, cat)) return true;
            } else if (curiosforge_canApplyAnyRarity(a, stack, cat)) {
                // 第三方自定义 Affix 类（如 apotheosis_spells 的法术词条）没有被 AffixTypes mixin
                // 覆盖到，用 canApplyTo 兜底，避免其物品被挡在重铸台输入槽外。
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean curiosforge_canApplyAnyRarity(dev.shadowsoffire.apotheosis.adventure.affix.Affix a, ItemStack stack, LootCategory cat) {
        for (var holder : RarityRegistry.INSTANCE.getOrderedRarities()) {
            if (!holder.isBound()) continue;
            try {
                if (a.canApplyTo(stack, cat, holder.get())) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Unique
    private static boolean curiosforge_hasAffixForRarity(ItemStack stack, LootCategory cat, LootRarity rarity) {
        for (var a : AffixRegistry.INSTANCE.getValues()) {
            if (a instanceof AffixTypes af) {
                Set<LootCategory> types = af.curiosforge_getTypes();
                if (!types.contains(cat) && !AffixTypes.curiosforge_typeMatches(types, cat)) continue;
                if (((dev.shadowsoffire.apotheosis.adventure.affix.Affix) a).canApplyTo(stack, cat, rarity)) return true;
            } else {
                try {
                    if (((dev.shadowsoffire.apotheosis.adventure.affix.Affix) a).canApplyTo(stack, cat, rarity)) return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private static boolean hasAffixFor(ItemStack stack) {
        return hasAffixFor(stack, LootCategory.forItem(stack));
    }
}
