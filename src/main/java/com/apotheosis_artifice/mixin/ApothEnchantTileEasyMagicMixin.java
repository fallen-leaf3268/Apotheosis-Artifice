package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.compat.EasyMagicEnchantingStorage;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantTile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

@Mixin(ApothEnchantTile.class)
public abstract class ApothEnchantTileEasyMagicMixin implements EasyMagicEnchantingStorage {

    @Unique private static final String ARTIFICE_EASY_MAGIC_INVENTORY = "artifice_easy_magic_inventory";
    @Unique private final SimpleContainer artifice$easyMagicInventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ApothEnchantTile tile = (ApothEnchantTile) (Object) ApothEnchantTileEasyMagicMixin.this;
            tile.setChanged();
            if (tile.getLevel() == null || tile.getLevel().isClientSide) return;
            tile.getLevel().players().forEach(player -> {
                if (player.containerMenu instanceof ApothEnchantmentMenu menu && menu.enchantSlots == this) {
                    menu.slotsChanged(this);
                }
            });
        }
    };

    @Override
    public Container getEasyMagicInventory() {
        return this.artifice$easyMagicInventory;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void artifice$saveEasyMagicInventory(CompoundTag tag, CallbackInfo ci) {
        CompoundTag inventory = new CompoundTag();
        ItemStack input = this.artifice$easyMagicInventory.getItem(0);
        ItemStack fuel = this.artifice$easyMagicInventory.getItem(1);
        ItemStack catalyst = this.artifice$easyMagicInventory.getItem(2);
        if (!input.isEmpty()) inventory.put("input", input.save(new CompoundTag()));
        if (!fuel.isEmpty()) inventory.put("fuel", fuel.save(new CompoundTag()));
        if (!catalyst.isEmpty()) inventory.put("catalyst", catalyst.save(new CompoundTag()));
        tag.put(ARTIFICE_EASY_MAGIC_INVENTORY, inventory);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void artifice$loadEasyMagicInventory(CompoundTag tag, CallbackInfo ci) {
        this.artifice$easyMagicInventory.setItem(0, ItemStack.EMPTY);
        this.artifice$easyMagicInventory.setItem(1, ItemStack.EMPTY);
        this.artifice$easyMagicInventory.setItem(2, ItemStack.EMPTY);
        if (!tag.contains(ARTIFICE_EASY_MAGIC_INVENTORY)) return;
        CompoundTag inventory = tag.getCompound(ARTIFICE_EASY_MAGIC_INVENTORY);
        if (inventory.contains("input")) this.artifice$easyMagicInventory.setItem(0, ItemStack.of(inventory.getCompound("input")));
        if (inventory.contains("fuel")) this.artifice$easyMagicInventory.setItem(1, ItemStack.of(inventory.getCompound("fuel")));
        if (inventory.contains("catalyst")) this.artifice$easyMagicInventory.setItem(2, ItemStack.of(inventory.getCompound("catalyst")));
    }
}
