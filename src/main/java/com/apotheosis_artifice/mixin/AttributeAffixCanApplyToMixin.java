package com.apotheosis_artifice.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.AffixTypes;

import dev.shadowsoffire.apotheosis.adventure.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@Mixin(value = AttributeAffix.class, remap = false)
public abstract class AttributeAffixCanApplyToMixin implements AffixTypes {

    @Shadow
    protected Set<LootCategory> types;

    @Override
    public Set<LootCategory> curiosforge_getTypes() {
        return this.types;
    }

    @Inject(method = "canApplyTo", at = @At("RETURN"), cancellable = true, remap = false)
    private void cf_prefixMatch(ItemStack stack, LootCategory cat, LootRarity rarity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            Set<LootCategory> types = this.curiosforge_getTypes();
            if (types.isEmpty()) return;
            boolean catMatch = AffixTypes.curiosforge_typeMatches(types, cat) || AffixTypes.tagMatches(types, stack);
            if (catMatch) {
                ResourceLocation rid = RarityRegistry.INSTANCE.getKey(rarity);
                if (rid != null) {
                    for (var key : ((AttributeAffixAccessor) (Object) this).getValues().keySet()) {
                        if (rid.equals(RarityRegistry.INSTANCE.getKey(key))) {
                            cir.setReturnValue(true);
                            break;
                        }
                    }
                }
            }
        }
    }
}
