package com.apotheosis_artifice.compat;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;

public interface EasyMagicEnchantingStorage {
    Container getEasyMagicInventory();
    IItemHandler getEasyMagicFuelInventory();
    boolean isEasyMagicOutputPending();
    void setEasyMagicCompletedInput(ItemStack stack);
    void migrateLegacyFuel(Player player);

    abstract class Inventory extends SimpleContainer {
        public Inventory(int size) { super(size); }
        public abstract boolean notifiesMenu(ApothEnchantmentMenu menu, Player player);
    }
}
