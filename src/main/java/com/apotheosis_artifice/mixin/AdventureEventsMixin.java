package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.AdventureEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;

@Mixin(value = AdventureEvents.class, remap = false)
public class AdventureEventsMixin {

    @Inject(method = "affixModifiers", at = @At("HEAD"), cancellable = true, remap = false)
    private void cf_skipCurioItems(ItemAttributeModifierEvent e, CallbackInfo ci) {
        ItemStack stack = e.getItemStack();
        if (!stack.hasTag()) return;
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData != null && afxData.contains("curio_artifice")) {
            String val = afxData.getString("curio_artifice");
            if (val.startsWith("curio")) {
                ci.cancel();
            }
        }
    }
}
