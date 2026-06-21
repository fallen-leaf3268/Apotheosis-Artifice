package com.apotheosis_artifice.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import net.minecraft.world.item.ItemStack;

@Mixin(value = Gem.class, remap = false)
public class GemMixin {

    @Inject(method = "isValidIn", at = @At("RETURN"), cancellable = true)
    private void cf_isValidIn(ItemStack socketed, ItemStack gem, dev.shadowsoffire.apotheosis.adventure.loot.LootRarity rarity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        LootCategory cat = LootCategory.forItem(socketed);
        if (cat == null || (!cat.getName().equals("curio") && !cat.getName().startsWith("curios:"))) return;

        Map<LootCategory, GemBonus> map = ((GemAccessor) (Object) this).getBonusMap();
        LootCategory genericCurio = LootCategory.byId("curio");
        if (genericCurio == null || genericCurio.isNone()) return;

        GemBonus bonus = map.get(genericCurio);
        if (bonus != null && bonus.supports(rarity)) {
            cir.setReturnValue(true);
        }
    }
}
