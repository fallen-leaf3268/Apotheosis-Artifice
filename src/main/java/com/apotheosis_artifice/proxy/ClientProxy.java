package com.apotheosis_artifice.proxy;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.List;

public class ClientProxy implements IProxy {

    @Override
    public void handleForceSlot0(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu em) {
            em.enchantSlots.setItem(0, stack.copy());
            em.enchantSlots.setChanged();
        }
    }

    @Override
    public void handleSyncClues(int slot, List<EnchantmentInstance> clues) {
        if (Minecraft.getInstance().screen instanceof ApothEnchantScreen es) {
            es.acceptClues(slot, clues, true);
        }
    }
}
