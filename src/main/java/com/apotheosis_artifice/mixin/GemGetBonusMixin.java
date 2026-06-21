package com.apotheosis_artifice.mixin;

import java.util.Map;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;

@Mixin(value = Gem.class, remap = false)
public class GemGetBonusMixin {

    @Shadow
    protected Map<LootCategory, GemBonus> bonusMap;

    @Inject(method = "getBonus", at = @At("RETURN"), cancellable = true)
    private void cf_fallbackCurio(LootCategory cat, LootRarity rarity, CallbackInfoReturnable<Optional<GemBonus>> cir) {
        if (cir.getReturnValue().isPresent()) return;

        String catName = cat.getName();
        if (!catName.startsWith("curios:")) return;

        LootCategory genericCurio = LootCategory.byId("curio");
        if (genericCurio == null || genericCurio.isNone()) return;

        Optional<GemBonus> fallback = Optional.ofNullable(this.bonusMap.get(genericCurio)).filter(b -> b.supports(rarity));
        if (fallback.isPresent()) {
            cir.setReturnValue(fallback);
        }
    }
}
