package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.apotheosis_artifice.adventure.SigilUpgradeEvents;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.augmenting.AugmentingScreen;
import net.minecraft.network.chat.Component;

@Mixin(value = AugmentingScreen.class, remap = false)
public abstract class SigilAugmentingScreenMixin {

    @Redirect(method = {"renderBg", "m_7286_"}, at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/apotheosis/adventure/affix/AffixInstance;getAugmentingText()Lnet/minecraft/network/chat/Component;"))
    private Component artifice$showPower(AffixInstance instance) {
        return SigilUpgradeEvents.decorateAffix(instance, instance.getAugmentingText());
    }

}
