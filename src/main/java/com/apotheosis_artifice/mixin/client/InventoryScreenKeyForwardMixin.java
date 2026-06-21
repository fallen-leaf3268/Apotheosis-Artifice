package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.client.AttributesGuiHooks;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

/**
 * 只处理 keyPressed（特殊按键：退格、方向键、ESC 等）。
 * charTyped 由 Forge 事件 ScreenEvent.CharacterTyped 处理（在 AttributesGuiHooks 中注册）。
 */
@Mixin(Screen.class)
public class InventoryScreenKeyForwardMixin {

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void apotheosis_artifice_keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        EditBox box = AttributesGuiHooks.nameBox;
        if (box == null || !box.isFocused()) return;

        // ESC: 取消焦点
        if (keyCode == 256) {
            box.setFocused(false);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }
        // 搜索框聚焦时，所有按键都不应传给 Screen（防止 E 键关闭背包等快捷键触发）
        box.keyPressed(keyCode, scanCode, modifiers);
        cir.setReturnValue(true);
        cir.cancel();
    }
}
