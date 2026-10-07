package com.apotheosis_artifice.mixin;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@Mixin(value = LootController.class, remap = false)
public class LootControllerMixin {

    @Inject(method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private static void artifice$gateCurioLoot(ItemStack stack, LootCategory cat, LootRarity rarity,
        net.minecraft.util.RandomSource rand, CallbackInfoReturnable<ItemStack> cir) {
        if (!artifice$isCurioCategory(cat) || artifice$canGenerateCurioLoot()) return;
        LootCategory nativeCategory = com.apotheosis_artifice.CatOverride.forNativeItem(stack);
        if (nativeCategory.isNone() || artifice$isCurioCategory(nativeCategory)) {
            cir.setReturnValue(stack);
            return;
        }
        LootCategory previous = com.apotheosis_artifice.CatOverride.get();
        try {
            com.apotheosis_artifice.CatOverride.set(nativeCategory);
            var affixData = stack.getTagElement("affix_data");
            if (affixData != null && affixData.contains("curio_artifice")) {
                affixData.putString("curio_artifice", nativeCategory.getName());
            }
            cir.setReturnValue(LootController.createLootItem(stack, nativeCategory, rarity, rand));
        } finally {
            com.apotheosis_artifice.CatOverride.set(previous);
        }
    }

    @Unique
    private static boolean artifice$isCurioCategory(LootCategory category) {
        String name = category.getName();
        return name.equals("curio") || name.startsWith("curios:");
    }

    @Unique
    private static boolean artifice$canGenerateCurioLoot() {
        return com.apotheosis_artifice.ApotheosisConfig.isCuriosReforgingEnabled()
            && (com.apotheosis_artifice.CatOverride.isReforging()
                || com.apotheosis_artifice.ApotheosisConfig.isCuriosLootRarityEnabled());
    }

    @Inject(method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private static void artifice$clearMaliceOnReforge(ItemStack stack, dev.shadowsoffire.apotheosis.adventure.loot.LootCategory cat,
        LootRarity rarity, net.minecraft.util.RandomSource rand, CallbackInfoReturnable<ItemStack> cir) {
        if (artifice$isCurioCategory(cat) && !artifice$canGenerateCurioLoot()) return;
        com.apotheosis_artifice.adventure.SigilAffixHelper.clearMaliceState(cir.getReturnValue());
    }

    @Inject(method = "getAvailableAffixes", at = @At("RETURN"), cancellable = true)
    private static void cf_dedupById(ItemStack stack, LootRarity rarity, Set<DynamicHolder<? extends Affix>> currentAffixes,
        AffixType type, CallbackInfoReturnable<List<DynamicHolder<? extends Affix>>> cir) {
        List<DynamicHolder<? extends Affix>> list = cir.getReturnValue();
        if (list.isEmpty()) return;
        Set<ResourceLocation> currentIds = currentAffixes.stream().map(DynamicHolder::getId).collect(Collectors.toSet());
        List<DynamicHolder<? extends Affix>> filtered = list.stream()
            .filter(h -> !currentIds.contains(h.getId()))
            .collect(Collectors.toList());
        if (filtered.size() != list.size()) {
            cir.setReturnValue(filtered);
        }
    }

    // 替换 Collections.shuffle:在打乱之后立刻把 ABILITY 类型 affix 排到最前,
    // 让物品名前缀位置(nameList.get(0))稳定是 ABILITY 类型,显示主名(无"X 之"后缀)。
    @Redirect(method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Ljava/util/Collections;shuffle(Ljava/util/List;Ljava/util/Random;)V"))
    private static void cf_stableSort(java.util.List<?> list, java.util.Random random) {
        CollectionsShim.shuffleAbilityFirst((java.util.List<AffixInstance>) list, random);
    }

    static class CollectionsShim {
        static void shuffleAbilityFirst(List<AffixInstance> list, java.util.Random random) {
            java.util.Collections.shuffle(list, random);
            list.sort(Comparator.comparingInt((AffixInstance i) -> i.affix().get().getType() == AffixType.ABILITY ? 0 : 1));
        }
    }
}
