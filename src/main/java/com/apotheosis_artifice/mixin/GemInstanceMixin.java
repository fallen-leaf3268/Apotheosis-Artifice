package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance;

@Mixin(value = GemInstance.class, remap = false)
public class GemInstanceMixin {

    @Inject(method = "isValid", at = @At("RETURN"), cancellable = true)
    private void cf_fallbackCurio(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        GemInstance self = (GemInstance) (Object) this;
        if (!self.isValidUnsocketed()) return;

        LootCategory curioCat = self.cat();
        String catName = curioCat.getName();
        if (!catName.startsWith("curios:")) return;

        LootCategory genericCurio = LootCategory.byId("curio");
        if (genericCurio == null || genericCurio.isNone()) return;

        if (self.gem().get().getBonus(genericCurio, self.rarity().get()).isPresent()) {
            cir.setReturnValue(true);
        }
    }
}
