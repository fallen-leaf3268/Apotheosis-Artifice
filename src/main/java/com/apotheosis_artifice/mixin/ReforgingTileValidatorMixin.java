package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingRecipe;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableTile;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import net.minecraft.resources.ResourceLocation;

@Mixin(value = ReforgingTableTile.class, remap = false)
public class ReforgingTileValidatorMixin {

    @Inject(method = "getRecipeFor", at = @At("RETURN"), cancellable = true)
    private void apotheosis_artifice_fallbackRecipe(LootRarity rarity, CallbackInfoReturnable<ReforgingRecipe> cir) {
        if (cir.getReturnValue() != null || !Apotheosis.enableAdventure || rarity == null) return;
        ReforgingTableTile tile = (ReforgingTableTile) (Object) this;
        LootRarity maxRarity = tile.getMaxRarity();
        ResourceLocation rarityId = RarityRegistry.INSTANCE.getKey(rarity);
        if (rarityId == null || maxRarity == null || !maxRarity.isAtLeast(rarity)) return;
        int sigilCost = 0;
        int materialCost = 0;
        int levelCost = 0;
        for (ReforgingRecipe recipe : tile.getLevel().getRecipeManager().getAllRecipesFor(Apoth.RecipeTypes.REFORGING)) {
            sigilCost = Math.max(sigilCost, recipe.sigilCost());
            materialCost = Math.max(materialCost, recipe.matCost());
            levelCost = Math.max(levelCost, recipe.levelCost());
        }
        cir.setReturnValue(new ReforgingRecipe(
            new ResourceLocation("apotheosis_artifice", "fallback_reforging/" + rarityId.getNamespace() + "/" + rarityId.getPath()),
            RarityRegistry.INSTANCE.holder(rarityId), materialCost, sigilCost, levelCost));
    }
}
