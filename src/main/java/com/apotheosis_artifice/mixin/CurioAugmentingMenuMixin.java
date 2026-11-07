package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.CatOverride;
import dev.shadowsoffire.apotheosis.adventure.affix.augmenting.AugmentingMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AugmentingMenu.class, remap = false)
public abstract class CurioAugmentingMenuMixin {
    @Shadow public abstract ItemStack getMainItem();

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true, remap = true)
    private void artifice$blockCurioUpgrade(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (CatOverride.isApotheosisBlocked(getMainItem())) cir.setReturnValue(false);
    }
}
