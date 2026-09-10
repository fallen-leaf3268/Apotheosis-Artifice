package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.adventure.socket.SocketingRecipe;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Mixin(value = SocketingRecipe.class, priority = 500, remap = false)
public class SocketingRecipeMixin {

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true, remap = true)
    private void apotheosis_artifice_validateInputs(Container inv, RegistryAccess regs, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack base = inv.getItem(1);
        ItemStack gemStack = inv.getItem(2);
        if (base.isEmpty() || gemStack.isEmpty()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        int socket = SocketHelper.getFirstEmptySocket(base);
        if (socket < 0 || socket >= SocketHelper.getGems(base).size()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
