package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.affix.DamageResistanceAffix;
import com.apotheosis_artifice.affix.EffectImmunityAffix;
import com.apotheosis_artifice.affix.RadianceAffix;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;

import top.theillusivec4.curios.common.data.CuriosSlotManager;
import net.minecraft.world.entity.EquipmentSlot;

@Mixin(value = AffixRegistry.class, remap = false)
public class AffixRegistryMixin {

    @SuppressWarnings("unchecked")
    @Inject(method = "registerBuiltinCodecs", at = @At("TAIL"))
    private void apotheosis_artifice_registerCodecs(CallbackInfo ci) {
        // registerCodec 是 DynamicRegistry 上的 public final 方法，直接调用即可（无需反射）。
        DynamicRegistry<Affix> self = (DynamicRegistry<Affix>) (Object) this;
        self.registerCodec(new ResourceLocation("apotheosis_artifice", "damage_resistance"), DamageResistanceAffix.CODEC);
        self.registerCodec(new ResourceLocation("apotheosis_artifice", "effect_immunity"), EffectImmunityAffix.CODEC);
        self.registerCodec(new ResourceLocation("apotheosis_artifice", "radiance"), RadianceAffix.CODEC);
    }

    @Inject(method = "beginReload", at = @At("TAIL"))
    private void apotheosis_artifice_registerSlots(CallbackInfo ci) {
        if (!com.apotheosis_artifice.ApotheosisConfig.ENABLE_CURIOS_LOOT_RARITY.get()) {
            ApotheosisArtificeMod.LOGGER.info("Curios loot rarity is disabled, skipping curios LootCategory registration");
            return;
        }
        try {
            for (String slotId : CuriosSlotManager.SERVER.getSlots().keySet()) {
                String catName = "curios:" + slotId;
                if (LootCategory.byId(catName) == null || LootCategory.byId(catName).isNone()) {
                    LootCategory.register(LootCategory.HELMET, catName,
                        s -> !s.isEmpty() && s.is(ItemTags.create(new ResourceLocation("curios:" + slotId))),
                        new EquipmentSlot[]{EquipmentSlot.CHEST});
                }
            }
            ApotheosisArtificeMod.LOGGER.info("Registered all curios LootCategories before affix reload");
        } catch (Exception e) {
            ApotheosisArtificeMod.LOGGER.warn("Could not register curios slot categories before reload", e);
        }
    }
}
