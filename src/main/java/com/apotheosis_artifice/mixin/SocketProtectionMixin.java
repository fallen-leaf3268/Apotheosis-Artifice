package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

@Mixin(value = LootController.class, remap = false)
public class SocketProtectionMixin {

    @Inject(method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"))
    private static void cf_clearSocketsOnRarityChange(ItemStack stack, LootCategory cat, LootRarity rarity, RandomSource rand, CallbackInfoReturnable<ItemStack> cir) {
        if (!ApotheosisConfig.CLEAR_SOCKETS_ON_RARITY_CHANGE.get()) return;

        var oldRarity = AffixHelper.getRarity(stack);
        if (!oldRarity.isBound()) return;

        String oldKey = RarityRegistry.INSTANCE.getKey(oldRarity.get()).toString();
        String newKey = RarityRegistry.INSTANCE.getKey(rarity).toString();
        if (!oldKey.equals(newKey)) {
            SocketHelper.setSockets(stack, 0);
        }
    }
}
