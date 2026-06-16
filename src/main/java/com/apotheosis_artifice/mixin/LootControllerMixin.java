package com.apotheosis_artifice.mixin;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@Mixin(value = LootController.class, remap = false)
public class LootControllerMixin {

    @Inject(method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Ljava/lang/RuntimeException;<init>(Ljava/lang/String;)V"),
        cancellable = true)
    private static void curiosforge_preventCrashOnNoAffixes(ItemStack stack, dev.shadowsoffire.apotheosis.adventure.loot.LootCategory cat, LootRarity rarity, net.minecraft.util.RandomSource rand, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(ItemStack.EMPTY);
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
}
