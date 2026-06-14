package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.augmenting.AugmentingMenu;
import net.minecraft.world.item.ItemStack;

@Mixin(value = AugmentingMenu.class, remap = false)
public class AugmentingMenuMixin {

    @SuppressWarnings("rawtypes")
    @Inject(method = "computeAlternatives", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cf_logStack(ItemStack stack, AffixInstance selected, java.util.List affixes, CallbackInfoReturnable<java.util.List> cir) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        String curioTag = afxData != null ? afxData.getString("curio_artifice") : "null";
        ApotheosisArtificeMod.LOGGER.info("[Augment] computeAlternatives: item={} curio_artifice={} selectedAffix={}",
            stack.getHoverName().getString(), curioTag, selected.affix().getId());
    }
}
