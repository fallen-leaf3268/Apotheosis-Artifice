package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.shadowsoffire.attributeslib.client.AttributesGui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 将 Apothic Attributes 属性 GUI 中的硬编码英文字符串替换为 translatable key。
 * AttributesGui.java 中有三处: "Hide Unchanged", "Modifier Formula", "Hide Unchanged Attributes"
 */
@Mixin(value = AttributesGui.class, remap = false)
public class AttributesGuiLocalizeMixin {

    /**
     * render() 中 gfx.drawString(font, Component.literal("Hide Unchanged"), ...)
     * drawString 在 render 中调用两次: 第一次是标题(translatable，不需要改)，第二次是 Hide Unchanged。
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", ordinal = 1), remap = true)
    private int localizeHideUnchanged(GuiGraphics gfx, Font font, Component text, int x, int y, int color, boolean shadow) {
        return gfx.drawString(font, Component.translatable("attributeslib.gui.hide_unchanged"), x, y, color, shadow);
    }

    /**
     * renderTooltip() 中 this.addComp(Component.literal("Modifier Formula"), ...)
     */
    @Redirect(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 0), remap = false)
    private MutableComponent localizeModifierFormula(String text) {
        if ("Modifier Formula".equals(text)) {
            return Component.translatable("attributeslib.gui.modifier_formula");
        }
        return Component.literal(text);
    }

    /**
     * 内类 HideUnchangedButton.<init>() 中的 Component.literal("Hide Unchanged Attributes")
     * 需要单独 target 内类。
     */
    @Mixin(targets = "dev.shadowsoffire.attributeslib.client.AttributesGui$HideUnchangedButton")
    public static class HideUnchangedButtonMixin {
        @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"), remap = false)
        private MutableComponent localizeHideUnchangedAttr(String text) {
            if ("Hide Unchanged Attributes".equals(text)) {
                return Component.translatable("attributeslib.gui.hide_unchanged_attr");
            }
            return Component.literal(text);
        }
    }
}
