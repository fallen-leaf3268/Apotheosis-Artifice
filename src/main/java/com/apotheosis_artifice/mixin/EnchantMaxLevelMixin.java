package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.ench.EnchantmentInfo;
import net.minecraft.world.item.enchantment.Enchantment;

@Mixin(EnchantmentInfo.class)
public abstract class EnchantMaxLevelMixin {

    @Shadow(remap = false)
    @Final
    protected Enchantment ench;

    @Inject(method = "getMaxLevel", at = @At("RETURN"), cancellable = true, remap = false)
    private void artifice_boostMaxLevel(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ApotheosisConfig.limitTaggedEnchantmentLevel(this.ench, cir.getReturnValueI()));
    }

    @Redirect(method = "load", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/apotheosis/ench/EnchModule;getDefaultMax(Lnet/minecraft/world/item/enchantment/Enchantment;)I"),
        remap = false, require = 1, allow = 1)
    private static int artifice$nativeDefaultMax(Enchantment ench) {
        return ApotheosisConfig.getNativeDefaultMax(ench);
    }
}
