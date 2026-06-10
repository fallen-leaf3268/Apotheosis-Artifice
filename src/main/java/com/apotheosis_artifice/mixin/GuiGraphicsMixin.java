package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Redirect(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
                       ordinal = 0))
    private int scaleCountText(GuiGraphics instance, Font font, String text, int x, int y, int color, boolean shadow) {
        int textW = font.width(text);
        if (textW <= 14) {
            return instance.drawString(font, text, x, y, color, shadow);
        }
        float scale = 14f / textW;
        float ox = x + textW * (1 - scale);
        float oy = y + (1 - scale) * 4;
        instance.pose().pushPose();
        instance.pose().translate(ox, oy, 0);
        instance.pose().scale(scale, scale, 1.0F);
        int result = instance.drawString(font, text, 0, 0, color, shadow);
        instance.pose().popPose();
        return result;
    }
}
