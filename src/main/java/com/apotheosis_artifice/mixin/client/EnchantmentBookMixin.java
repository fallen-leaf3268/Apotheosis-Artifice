package com.apotheosis_artifice.mixin.client;

import com.apotheosis_artifice.enchant.MechanicalRavenEnchantScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentScreen.class)
public class EnchantmentBookMixin {

    @Inject(method = "renderBook", at = @At("HEAD"), cancellable = true)
    private void apotheosis_artifice_cancelBook(GuiGraphics gfx, int x, int y, float pt, CallbackInfo ci) {
        if (((Object) this) instanceof MechanicalRavenEnchantScreen) {
            ci.cancel();
        }
    }
}
