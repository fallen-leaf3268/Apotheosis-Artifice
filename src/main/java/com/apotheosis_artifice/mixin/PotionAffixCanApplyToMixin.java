package com.apotheosis_artifice.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.AffixTypes;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.PotionAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.world.item.ItemStack;

@Mixin(value = PotionAffix.class, remap = false)
public abstract class PotionAffixCanApplyToMixin implements AffixTypes {

    @Inject(method = "canApplyTo", at = @At("RETURN"), cancellable = true, remap = false)
    private void cf_prefixMatch(ItemStack stack, LootCategory cat, LootRarity rarity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            Set<LootCategory> types = this.curiosforge_getTypes();
            if (types.isEmpty()) return;
            boolean catMatch = AffixTypes.curiosforge_typeMatches(types, cat) || tagMatches(types, stack);
            if (catMatch && ((PotionAffixAccessor) (Object) this).curiosforge_getValues().containsKey(rarity)) {
                cir.setReturnValue(true);
            }
        }
    }

    private static boolean tagMatches(Set<LootCategory> types, ItemStack stack) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData != null && afxData.contains("curio_artifice")) {
            String val = afxData.getString("curio_artifice");
            return types.stream().anyMatch(t -> val.startsWith(t.getName()));
        }
        return false;
    }
}
