package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.CatOverride;
import dev.shadowsoffire.apotheosis.adventure.socket.AddSocketsRecipe;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketingRecipe;
import dev.shadowsoffire.apotheosis.adventure.socket.WithdrawalRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {AddSocketsRecipe.class, SocketingRecipe.class, WithdrawalRecipe.class}, remap = false)
public abstract class CurioSmithingRecipeMixin {
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true, remap = true)
    private void artifice$blockCurioInput(Container inv, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (CatOverride.isApotheosisBlocked(inv.getItem(1))) cir.setReturnValue(false);
    }

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true, remap = true)
    private void artifice$blockCurioResult(Container inv, RegistryAccess access, CallbackInfoReturnable<ItemStack> cir) {
        if (CatOverride.isApotheosisBlocked(inv.getItem(1))) cir.setReturnValue(ItemStack.EMPTY);
    }
}
