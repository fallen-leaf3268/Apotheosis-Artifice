package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.compat.EasyMagicEnchantingStorage;
import com.apotheosis_artifice.compat.EasyMagicCompat;
import com.apotheosis_artifice.enchant.RavenEnchantTile;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantTile;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;

@Mixin(ApothEnchantTile.class)
public abstract class ApothEnchantTileEasyMagicMixin implements EasyMagicEnchantingStorage {

    @Shadow(remap = false) protected ItemStackHandler inv;
    @Unique private static final String ARTIFICE_EASY_MAGIC_INVENTORY = "artifice_easy_magic_inventory";
    @Unique private final SimpleContainer artifice$easyMagicInventory = new SimpleContainer(3) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == 1) return stack.is(Tags.Items.ENCHANTING_FUELS) || EasyMagicCompat.isEnchantingCatalyst(stack);
            if (slot == 2) return EasyMagicCompat.isRerollCatalyst(stack);
            return true;
        }

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

    @Unique private final IItemHandler artifice$fuelInventory = new RangedWrapper(new InvWrapper(this.artifice$easyMagicInventory), 1, 2);
    @Unique private LazyOptional<IItemHandler> artifice$fuelCapability = LazyOptional.of(() -> this.artifice$fuelInventory);

    @Override
    public Container getEasyMagicInventory() {
        return this.artifice$easyMagicInventory;
    }

    @Override
    public IItemHandler getEasyMagicFuelInventory() {
        return this.artifice$fuelInventory;
    }

    @Override
    public void migrateLegacyFuel(Player player) {
        if (player.level().isClientSide || !EasyMagicCompat.isLoaded()) return;
        ItemStack legacy = this.inv.extractItem(0, this.inv.getStackInSlot(0).getCount(), false);
        if (legacy.isEmpty()) return;
        ItemStack remaining = this.artifice$fuelInventory.insertItem(0, legacy, false);
        if (!remaining.isEmpty()) player.getInventory().placeItemBackInInventory(remaining);
        ((ApothEnchantTile) (Object) this).setChanged();
    }

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void artifice$exposeSharedFuel(Capability<T> capability, Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && EasyMagicCompat.isLoaded()) {
            cir.setReturnValue(this.artifice$fuelCapability.cast());
        }
    }

    @Inject(method = "invalidateCaps", at = @At("TAIL"), remap = false)
    private void artifice$invalidateFuel(CallbackInfo ci) {
        this.artifice$fuelCapability.invalidate();
    }

    @Inject(method = "reviveCaps", at = @At("TAIL"), remap = false)
    private void artifice$reviveFuel(CallbackInfo ci) {
        this.artifice$fuelCapability = LazyOptional.of(() -> this.artifice$fuelInventory);
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

@Mixin(ApothEnchantBlock.class)
abstract class ApothEnchantBlockEasyMagicMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void artifice$dropPersistentInventory(BlockState state, Level level, BlockPos pos,
        BlockState newState, boolean moving, CallbackInfo ci) {
        if (level.isClientSide || state.is(newState.getBlock())) return;
        var tile = level.getBlockEntity(pos);
        if (tile instanceof RavenEnchantTile || !(tile instanceof EasyMagicEnchantingStorage storage)) return;
        Container inventory = storage.getEasyMagicInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) Block.popResource(level, pos, stack);
        }
    }
}
