package com.apotheosis_artifice.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisNetwork;
import com.apotheosis_artifice.enchant.GemBinderItem;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

    private static final Set<String> TOGGLE_ITEMS = Set.of(
        "l2hostility:detector_glasses",
        "l2hostility:mining_claw");

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void apotheosis_artifice_toggleItem(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 1) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        var slot = screen.getSlotUnderMouse();
        if (slot == null || !slot.hasItem()) return;

        var stack = slot.getItem();
        var item = stack.getItem();

        if (item instanceof GemBinderItem) {
            ApotheosisNetwork.sendToggleBinder(slot.index);
            cir.setReturnValue(true);
            return;
        }

        var id = BuiltInRegistries.ITEM.getKey(item);
        if (id != null && TOGGLE_ITEMS.contains(id.toString())) {
            ApotheosisNetwork.sendToggleBinder(slot.index);
            cir.setReturnValue(true);
        }
    }
}
