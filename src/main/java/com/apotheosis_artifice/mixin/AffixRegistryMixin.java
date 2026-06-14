package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.affix.DamageImmunityAffix;
import com.apotheosis_artifice.affix.DamageResistanceAffix;
import com.apotheosis_artifice.affix.EffectImmunityAffix;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;

import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

@Mixin(value = AffixRegistry.class, remap = false)
public class AffixRegistryMixin {

    @Inject(method = "registerBuiltinCodecs", at = @At("TAIL"))
    private void apotheosis_artifice_registerCodecs(CallbackInfo ci) {
        try {
            var m = AffixRegistry.class.getSuperclass().getDeclaredMethod("registerCodec", ResourceLocation.class, Codec.class);
            m.setAccessible(true);
            m.invoke(this, new ResourceLocation("apotheosis_artifice", "damage_immunity"), DamageImmunityAffix.CODEC);
            m.invoke(this, new ResourceLocation("apotheosis_artifice", "damage_resistance"), DamageResistanceAffix.CODEC);
            m.invoke(this, new ResourceLocation("apotheosis_artifice", "effect_immunity"), EffectImmunityAffix.CODEC);
        } catch (Exception e) {
            ApotheosisArtificeMod.LOGGER.error("Failed to register custom affix codecs", e);
        }
    }
}
