package com.apotheosis_artifice.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.client.AttributesGuiHooks;

import dev.shadowsoffire.attributeslib.client.AttributesGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

@Mixin(value = AttributesGui.class, remap = false)
public class AttributesGuiSearchMixin {

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    protected boolean open;
    @Shadow
    protected List<AttributeInstance> data;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void apotheosis_artifice_init(InventoryScreen parent, CallbackInfo ci) {
        AttributesGuiHooks.init((AttributesGui) (Object) this, leftPos, topPos);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = true)
    private void apotheosis_artifice_render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!open) {
            if (AttributesGuiHooks.nameBox != null) AttributesGuiHooks.nameBox.setFocused(false);
            return;
        }
        if (AttributesGuiHooks.nameBox != null) {
            AttributesGuiHooks.nameBox.setPosition(leftPos + 40, topPos + 4);
        }
        AttributesGuiHooks.render(gfx, mouseX, mouseY, partialTicks, open);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void apotheosis_artifice_mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!open) return;
        AttributesGuiHooks.mouseClicked(mouseX, mouseY, button);
        if (AttributesGuiHooks.nameBox != null && AttributesGuiHooks.nameBox.isFocused()) cir.setReturnValue(true);
    }

    @Inject(method = "refreshData", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V", shift = At.Shift.AFTER))
    private void apotheosis_artifice_refreshData(CallbackInfo ci) {
        data = AttributesGuiHooks.refreshData(data);
    }
}
