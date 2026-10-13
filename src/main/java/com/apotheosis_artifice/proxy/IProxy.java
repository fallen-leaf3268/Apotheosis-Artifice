package com.apotheosis_artifice.proxy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import java.util.List;

public interface IProxy {
    default void handleGemCaseMaterials(int menuId, List<ItemStack> materials) {}

    default void handleForceSlot0(ItemStack stack) {}

    default void handleSyncClues(int slot, List<EnchantmentInstance> clues) {}
}
