package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.client.AttributesGuiHooks;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Screen.class)
public class InventoryScreenKeyForwardMixin {

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void apotheosis_artifice_keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        EditBox box = AttributesGuiHooks.nameBox;
        if (box == null || !box.isFocused()) return;

        if (keyCode == 256) {
            box.setFocused(false);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }
        box.keyPressed(keyCode, scanCode, modifiers);
        cir.setReturnValue(true);
        cir.cancel();
    }
}
