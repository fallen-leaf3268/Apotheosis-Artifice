package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.shadowsoffire.attributeslib.client.AttributesGui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Mixin(value = AttributesGui.class, remap = false)
public class AttributesGuiLocalizeMixin {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", ordinal = 1), remap = true)
    private int localizeHideUnchanged(GuiGraphics gfx, Font font, Component text, int x, int y, int color, boolean shadow) {
        return gfx.drawString(font, Component.translatable("attributeslib.gui.hide_unchanged"), x, y, color, shadow);
    }

    @Redirect(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"), remap = true)
    private MutableComponent localizeModifierFormula(String text) {
        if ("Modifier Formula".equals(text)) {
            return Component.translatable("attributeslib.gui.modifier_formula");
        }
        return Component.literal(text);
    }

    @Mixin(value = dev.shadowsoffire.attributeslib.client.AttributesGui.HideUnchangedButton.class, remap = false)
    public static class HideUnchangedButtonMixin {
        @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"), remap = true)
        private static MutableComponent localizeHideUnchangedAttr(String text) {
            if ("Hide Unchanged Attributes".equals(text)) {
                return Component.translatable("attributeslib.gui.hide_unchanged_attr");
            }
            return Component.literal(text);
        }
    }
}
