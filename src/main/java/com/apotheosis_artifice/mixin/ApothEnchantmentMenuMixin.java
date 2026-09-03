package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.compat.EasyMagicCompat;
import com.apotheosis_artifice.compat.EasyMagicEnchantingStorage;
import com.apotheosis_artifice.compat.EnigmaticLegacyCompat;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.StatsMessage;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantTile;
import dev.shadowsoffire.placebo.network.PacketDistro;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;

@Mixin(ApothEnchantmentMenu.class)
public abstract class ApothEnchantmentMenuMixin extends EnchantmentMenu {

    @Shadow(remap = false) protected ApothEnchantmentMenu.TableStats stats;
    @Unique private Player artifice$menuPlayer;
    @Unique private Inventory artifice$playerInventory;
    @Unique private Container artifice$pendingEasyMagicInventory;

    protected ApothEnchantmentMenuMixin(int id, Inventory inventory) {
        super(id, inventory);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void artifice$bindClientEasyMagicInventory(int id, Inventory inventory, CallbackInfo ci) {
        this.artifice$rememberPlayer(inventory);
        if (EasyMagicCompat.isLoaded()) this.artifice$bindEasyMagicInventory(new SimpleContainer(3) {
            @Override
            public void setChanged() {
                super.setChanged();
                ApothEnchantmentMenuMixin.this.slotsChanged(this);
            }
        });
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantTile;)V", at = @At("TAIL"), remap = false)
    private void artifice$bindServerEasyMagicInventory(int id, Inventory inventory, ContainerLevelAccess access, ApothEnchantTile tile, CallbackInfo ci) {
        this.artifice$rememberPlayer(inventory);
        if (!(tile instanceof EasyMagicEnchantingStorage storage)) return;
        if (EasyMagicCompat.isLoaded()) {
            this.artifice$bindEasyMagicInventory(storage.getEasyMagicInventory());
        } else {
            this.artifice$pendingEasyMagicInventory = storage.getEasyMagicInventory();
        }
    }

    private void artifice$rememberPlayer(Inventory inventory) {
        this.artifice$menuPlayer = inventory.player;
        this.artifice$playerInventory = inventory;
    }

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void artifice$migrateEasyMagicInventory(CallbackInfo ci) {
        Container source = this.artifice$pendingEasyMagicInventory;
        if (source == null) return;
        this.artifice$pendingEasyMagicInventory = null;
        this.artifice$moveOrReturnEasyMagicStack(source, 0, 0);
        this.artifice$moveOrReturnEasyMagicStack(source, 1, 1);
        this.artifice$returnEasyMagicStack(source, 2);
    }

    @Unique
    private void artifice$moveOrReturnEasyMagicStack(Container source, int sourceSlot, int targetSlot) {
        ItemStack stack = source.getItem(sourceSlot).copy();
        if (stack.isEmpty()) return;
        Slot target = this.slots.get(targetSlot);
        if (!target.hasItem() && target.mayPlace(stack)) {
            target.set(stack);
        } else {
            this.artifice$playerInventory.placeItemBackInInventory(stack);
        }
        source.setItem(sourceSlot, ItemStack.EMPTY);
    }

    @Unique
    private void artifice$returnEasyMagicStack(Container source, int sourceSlot) {
        ItemStack stack = source.getItem(sourceSlot).copy();
        if (stack.isEmpty()) return;
        this.artifice$playerInventory.placeItemBackInInventory(stack);
        source.setItem(sourceSlot, ItemStack.EMPTY);
    }

    private void artifice$bindEasyMagicInventory(Container inventory) {
        this.enchantSlots = inventory;
        Slot input = new Slot(inventory, 0, EasyMagicCompat.dedicatedRerollButton() ? 5 : 15, 47) {
            @Override public int getMaxStackSize() { return 1; }
        };
        input.index = 0;
        this.slots.set(0, input);
        Slot oldFuel = this.slots.get(1);
        Slot fuel = new Slot(inventory, 1, EasyMagicCompat.dedicatedRerollButton() ? 23 : 35, 47) {
            @Override public boolean mayPlace(ItemStack stack) {
                return oldFuel.mayPlace(stack) || EasyMagicCompat.isEnchantingCatalyst(stack);
            }
        };
        fuel.index = 1;
        this.slots.set(1, fuel);
        if (EasyMagicCompat.dedicatedRerollButton()) {
            this.addSlot(new Slot(inventory, 2, 41, 47) {
                @Override public boolean mayPlace(ItemStack stack) { return EasyMagicCompat.isRerollCatalyst(stack); }
            });
        }
        if (((Object) this).getClass() == ApothEnchantmentMenu.class) {
            this.slotsChanged(this.enchantSlots);
        }
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void artifice$handleEasyMagicReroll(Player player, int data, CallbackInfoReturnable<Boolean> cir) {
        if (data == 5) {
            ApothEnchantmentMenu menu = (ApothEnchantmentMenu) (Object) this;
            if (!player.level().isClientSide) menu.slotsChanged(menu.enchantSlots);
            cir.setReturnValue(true);
            return;
        }
        if (data == 4) cir.setReturnValue(EasyMagicCompat.tryReroll((ApothEnchantmentMenu) (Object) this, player));
    }

    @ModifyVariable(method = "clickMenuButton", at = @At("STORE"), ordinal = 1)
    private ItemStack artifice$provideVirtualPearlFuel(ItemStack fuel, Player player, int id) {
        if (id < 0 || id >= 3 || !EnigmaticLegacyCompat.isEnchanterPearlActive(player)) return fuel;
        if (fuel.isEmpty()) return new ItemStack(Items.LAPIS_LAZULI, 64);
        ItemStack virtualFuel = fuel.copy();
        virtualFuel.setCount(64);
        return virtualFuel;
    }

    @Inject(method = "canReadStatsFrom", at = @At("HEAD"), cancellable = true, remap = false)
    private static void artifice$allowLenientBookshelfPath(Level level, BlockPos tablePos,
        BlockPos shelfOffset, CallbackInfoReturnable<Boolean> cir) {
        if (!EasyMagicCompat.lenientBookshelves()) return;
        BlockPos between = tablePos.offset(
            shelfOffset.getX() / 2, shelfOffset.getY(), shelfOffset.getZ() / 2);
        if (level.getBlockState(between).getCollisionShape(level, between) != Shapes.block()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "gatherStats", at = @At("TAIL"), remap = false)
    private void artifice$enableEnchanterPearlTreasure(CallbackInfo ci) {
        if (this.artifice$menuPlayer == null || this.artifice$menuPlayer.level().isClientSide) return;
        ApothEnchantmentMenu.TableStats updated = EnigmaticLegacyCompat.enableTreasure(this.stats, this.artifice$menuPlayer);
        if (updated == this.stats) return;
        this.stats = updated;
        PacketDistro.sendTo(Apotheosis.CHANNEL, new StatsMessage(this.stats), this.artifice$menuPlayer);
    }
}
