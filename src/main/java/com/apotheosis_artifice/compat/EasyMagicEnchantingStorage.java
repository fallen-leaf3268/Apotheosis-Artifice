package com.apotheosis_artifice.compat;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.IItemHandler;

public interface EasyMagicEnchantingStorage {
    Container getEasyMagicInventory();
    IItemHandler getEasyMagicFuelInventory();
    void migrateLegacyFuel(Player player);
}
