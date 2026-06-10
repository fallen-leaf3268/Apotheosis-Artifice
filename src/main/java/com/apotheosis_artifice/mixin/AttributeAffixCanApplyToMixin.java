package com.apotheosis_artifice.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.AffixTypes;

import dev.shadowsoffire.apotheosis.adventure.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.world.item.ItemStack;

@Mixin(value = AttributeAffix.class, remap = false)
public abstract class AttributeAffixCanApplyToMixin implements AffixTypes {

    @Inject(method = "canApplyTo", at = @At("RETURN"), cancellable = true, remap = false)
    private void cf_prefixMatch(ItemStack stack, LootCategory cat, LootRarity rarity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            Set<LootCategory> types = this.curiosforge_getTypes();
            if (!types.isEmpty() && AffixTypes.curiosforge_typeMatches(types, cat)
                && ((AttributeAffixAccessor) (Object) this).curiosforge_getModifiers().containsKey(rarity)) {
                cir.setReturnValue(true);
            }
        }
    }
}
