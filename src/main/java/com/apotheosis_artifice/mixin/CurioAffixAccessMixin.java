package com.apotheosis_artifice.mixin;

import java.util.Map;

import com.apotheosis_artifice.CatOverride;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AffixHelper.class, remap = false)
public class CurioAffixAccessMixin {
    @Inject(method = {"getAffixes(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Map;", "getAffixesImpl"}, at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioAffixes(ItemStack stack,
        CallbackInfoReturnable<Map<DynamicHolder<? extends Affix>, AffixInstance>> cir) {
        if (CatOverride.isApotheosisBlocked(stack)) cir.setReturnValue(Map.of());
    }

    @Inject(method = "hasAffixes", at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioAffixPresence(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CatOverride.isApotheosisBlocked(stack)) cir.setReturnValue(false);
    }

    @Inject(method = "getRarity(Lnet/minecraft/world/item/ItemStack;)Ldev/shadowsoffire/placebo/reload/DynamicHolder;",
        at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioRarity(ItemStack stack, CallbackInfoReturnable<DynamicHolder<LootRarity>> cir) {
        if (CatOverride.isApotheosisBlocked(stack)) cir.setReturnValue(RarityRegistry.INSTANCE.emptyHolder());
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (CatOverride.isApotheosisBlocked(stack)) cir.setReturnValue(null);
    }

    @Inject(method = "setAffixes", at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioAffixWrites(ItemStack stack,
        Map<DynamicHolder<? extends Affix>, AffixInstance> affixes, CallbackInfo ci) {
        if (CatOverride.isApotheosisBlocked(stack)) ci.cancel();
    }

    @Inject(method = "setRarity", at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioRarityWrites(ItemStack stack, LootRarity rarity, CallbackInfo ci) {
        if (CatOverride.isApotheosisBlocked(stack)) ci.cancel();
    }

    @Inject(method = "setName", at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioNameWrites(ItemStack stack, Component name, CallbackInfo ci) {
        if (CatOverride.isApotheosisBlocked(stack)) ci.cancel();
    }

    @Inject(method = "copyFrom", at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioSourceTransfer(ItemStack stack, Entity entity, CallbackInfo ci) {
        if (CatOverride.isApotheosisBlocked(stack)) ci.cancel();
    }
}
