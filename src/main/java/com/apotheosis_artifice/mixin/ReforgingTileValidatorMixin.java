package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableBlock;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableTile;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.item.ItemStack;

/**
 * 允许任何稀有度材料放入重铸台，不要求存在 ReforgingRecipe。
 * 适配 KubeJS/其他模组添加的自定义稀有度（如 9 级稀有度）。
 * 如果缺少配方，消耗默认为 0，但材料可以正常放入和重铸。
 */
@Mixin(value = ReforgingTableTile.class, remap = false)
public class ReforgingTileValidatorMixin {

    @Inject(method = "isValidRarityMat", at = @At("HEAD"), cancellable = true)
    private void apotheosis_artifice_allowAnyRarity(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        DynamicHolder<LootRarity> rarity = RarityRegistry.getMaterialRarity(stack.getItem());
        if (!rarity.isBound()) return;

        LootRarity maxRarity = ((ReforgingTableBlock) ((ReforgingTableTile) (Object) this).getBlockState().getBlock()).getMaxRarity();
        if (maxRarity.isAtLeast(rarity.get())) {
            cir.setReturnValue(true);
        }
    }
}
