package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.CatOverride;

import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketedGems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SocketHelper.class, remap = false)
public class CurioSocketAccessMixin {
    @Inject(method = "getSockets", at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioSockets(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (CatOverride.isApotheosisBlocked(stack)) cir.setReturnValue(0);
    }

    @Inject(method = "getGems(Lnet/minecraft/world/item/ItemStack;)Ldev/shadowsoffire/apotheosis/adventure/socket/SocketedGems;",
        at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioGems(ItemStack stack, CallbackInfoReturnable<SocketedGems> cir) {
        if (CatOverride.isApotheosisBlocked(stack)) cir.setReturnValue(SocketedGems.EMPTY);
    }

    @Inject(method = "setSockets", at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioSocketWrites(ItemStack stack, int sockets, CallbackInfo ci) {
        if (CatOverride.isApotheosisBlocked(stack)) ci.cancel();
    }

    @Inject(method = "setGems", at = @At("HEAD"), cancellable = true)
    private static void artifice$blockCurioGemWrites(ItemStack stack, SocketedGems gems, CallbackInfo ci) {
        if (CatOverride.isApotheosisBlocked(stack)) ci.cancel();
    }
}
