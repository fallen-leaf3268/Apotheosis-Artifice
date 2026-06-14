package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

@Mixin(value = LootController.class, remap = false)
public class LootControllerMixin {

    @Inject(method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Ljava/lang/RuntimeException;<init>(Ljava/lang/String;)V"),
        cancellable = true)
    private static void curiosforge_preventCrashOnNoAffixes(ItemStack stack, LootCategory cat, LootRarity rarity, RandomSource rand, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(stack);
    }

    @Redirect(method = "getAvailableAffixes",
        at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;forItem(Lnet/minecraft/world/item/ItemStack;)Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;"),
        remap = false)
    private static LootCategory curiosforge_replaceCat(ItemStack stack) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData != null && afxData.contains("curio_artifice")) {
            String val = afxData.getString("curio_artifice");
            LootCategory cat = LootCategory.byId(val);
            if (cat != null && !cat.isNone()) {
                ApotheosisArtificeMod.LOGGER.info("[LootCtrl] forItem redirect: curio_artifice={} -> {}", val, cat.getName());
                return cat;
            }
        }
        LootCategory cat = LootCategory.forItem(stack);
        if (cat != null && !cat.isNone() && cat.getName().startsWith("curio")) {
            for (LootCategory c : LootCategory.VALUES) {
                if (!c.isNone() && !c.getName().startsWith("curio") && c.isValid(stack)) {
                    ApotheosisArtificeMod.LOGGER.info("[LootCtrl] forItem fallback: {} -> prefer native {}", cat.getName(), c.getName());
                    return c;
                }
            }
        }
        return cat;
    }
}
