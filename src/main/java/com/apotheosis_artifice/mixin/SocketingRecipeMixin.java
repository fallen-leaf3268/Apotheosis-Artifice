package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.adventure.socket.SocketingRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Mixin(value = SocketingRecipe.class, priority = 100, remap = false)
public class SocketingRecipeMixin {

    @Inject(method = {"assemble", "m_5874_"}, at = @At("HEAD"), cancellable = true, remap = true)
    private void cf_guard(Container inv, net.minecraft.core.RegistryAccess regs, CallbackInfoReturnable<ItemStack> cir) {
        if (inv.getItem(1).isEmpty()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
