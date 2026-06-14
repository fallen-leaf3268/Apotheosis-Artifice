package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.CatOverride;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.world.item.ItemStack;

@Mixin(value = LootCategory.class, remap = false)
public class LootCategoryMixin {

    @Inject(method = "forItem", at = @At("HEAD"), cancellable = true)
    private static void curiosforge_checkOverride(ItemStack item, CallbackInfoReturnable<LootCategory> cir) {
        LootCategory override = CatOverride.get();
        if (override != null) {
            ApotheosisArtificeMod.LOGGER.info("[LootCat] CatOverride={} for {}", override.getName(), item.getHoverName().getString());
            cir.setReturnValue(override);
            return;
        }

        var afxData = item.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData != null && afxData.contains("curio_artifice")) {
            String val = afxData.getString("curio_artifice");
            ApotheosisArtificeMod.LOGGER.info("[LootCat] curio_artifice={} for {}", val, item.getHoverName().getString());
            if (val.startsWith("curio")) {
                ApotheosisArtificeMod.LOGGER.info("[LootCat] -> return NONE (curio tag, skip vanilla)");
                cir.setReturnValue(LootCategory.NONE);
                return;
            }
            LootCategory cat = LootCategory.byId(val);
            if (cat != null && !cat.isNone()) {
                ApotheosisArtificeMod.LOGGER.info("[LootCat] -> return {}", cat.getName());
                cir.setReturnValue(cat);
            }
        }
    }
}
