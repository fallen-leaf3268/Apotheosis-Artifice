package com.apotheosis_artifice.proxy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import java.util.List;

public interface IProxy {

    /** 强制设置客户端附魔槽 slot0 */
    default void handleForceSlot0(ItemStack stack) {}

    /** 同步附魔线索到客户端界面 */
    default void handleSyncClues(int slot, List<EnchantmentInstance> clues) {}
}
