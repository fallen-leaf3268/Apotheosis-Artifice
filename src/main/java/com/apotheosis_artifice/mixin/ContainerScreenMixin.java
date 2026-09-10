package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisNetwork;
import com.apotheosis_artifice.enchant.GemBinderItem;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void apotheosis_artifice_toggleItem(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 1) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        // 手上拿着物品时不拦截右键，避免影响放置/分堆等原版交互。
        if (!screen.getMenu().getCarried().isEmpty()) return;
        var slot = screen.getSlotUnderMouse();
        if (slot == null || !slot.hasItem()) return;

        var stack = slot.getItem();
        var item = stack.getItem();

        if (item instanceof GemBinderItem) {
            ApotheosisNetwork.sendToggleBinder(slot.index);
            cir.setReturnValue(true);
            return;
        }

    }
}
