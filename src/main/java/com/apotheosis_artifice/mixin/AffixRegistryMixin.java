package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.affix.DamageResistanceAffix;
import com.apotheosis_artifice.affix.EffectImmunityAffix;
import com.apotheosis_artifice.affix.RadianceAffix;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;

import com.mojang.serialization.Codec;

import top.theillusivec4.curios.common.data.CuriosSlotManager;
import net.minecraft.world.entity.EquipmentSlot;

@Mixin(value = AffixRegistry.class, remap = false)
public class AffixRegistryMixin {

    @Inject(method = "registerBuiltinCodecs", at = @At("TAIL"))
    private void apotheosis_artifice_registerCodecs(CallbackInfo ci) {
        try {
            var m = AffixRegistry.class.getSuperclass().getDeclaredMethod("registerCodec", ResourceLocation.class, Codec.class);
            m.setAccessible(true);
            m.invoke(this, new ResourceLocation("apotheosis_artifice", "damage_resistance"), DamageResistanceAffix.CODEC);
            m.invoke(this, new ResourceLocation("apotheosis_artifice", "effect_immunity"), EffectImmunityAffix.CODEC);
            m.invoke(this, new ResourceLocation("apotheosis_artifice", "radiance"), RadianceAffix.CODEC);
        } catch (Exception e) {
            ApotheosisArtificeMod.LOGGER.error("Failed to register custom affix codecs", e);
        }
    }

    @Inject(method = "beginReload", at = @At("TAIL"))
    private void apotheosis_artifice_registerSlots(CallbackInfo ci) {
        try {
            for (String slotId : CuriosSlotManager.SERVER.getSlots().keySet()) {
                String catName = "curio:" + slotId;
                if (LootCategory.byId(catName) == null || LootCategory.byId(catName).isNone()) {
                    LootCategory.register(LootCategory.HELMET, catName,
                        s -> !s.isEmpty() && s.is(ItemTags.create(new ResourceLocation("curios:" + slotId))),
                        new EquipmentSlot[]{EquipmentSlot.CHEST});
                }
            }
            ApotheosisArtificeMod.LOGGER.info("Registered all curio LootCategories before affix reload");
        } catch (Exception e) {
            ApotheosisArtificeMod.LOGGER.warn("Could not register curio slot categories before reload", e);
        }
    }
}
