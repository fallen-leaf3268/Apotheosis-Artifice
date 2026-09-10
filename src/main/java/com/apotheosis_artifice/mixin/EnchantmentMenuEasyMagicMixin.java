package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.compat.EasyMagicCompat;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import com.apotheosis_artifice.enchant.MechanicalRavenEnchantMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuEasyMagicMixin extends AbstractContainerMenu {

    protected EnchantmentMenuEasyMagicMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void artifice$keepEasyMagicInventory(Player player, CallbackInfo ci) {
        if (!((Object) this instanceof ApothEnchantmentMenu)
            || (!EasyMagicCompat.isLoaded() && !((Object) this instanceof MechanicalRavenEnchantMenu))) return;
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
                if (player.isAlive() && !serverPlayer.hasDisconnected()) player.getInventory().placeItemBackInInventory(carried);
                else player.drop(carried, false);
                menu.setCarried(ItemStack.EMPTY);
            }
        }
        ci.cancel();
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void artifice$moveEasyMagicStacks(Player player, int index, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<ItemStack> cir) {
        if (!EasyMagicCompat.isLoaded() || !EasyMagicCompat.dedicatedRerollButton()
            || !((Object) this instanceof ApothEnchantmentMenu menu)
            || (Object) this instanceof MechanicalRavenEnchantMenu) return;
        if (index < 0 || index >= menu.slots.size()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        Slot slot = menu.slots.get(index);
        if (!slot.hasItem()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack raw = slot.getItem();
        ItemStack result = raw.copy();
        int playerStart = 2;
        int playerEnd = 38;
        int dedicated = 38;
        if (index == 0 || index == 1 || index == dedicated) {
            if (!this.moveItemStackTo(raw, playerStart, playerEnd, false)) result = ItemStack.EMPTY;
        } else if (index >= playerStart && index < playerEnd) {
            if (EasyMagicCompat.isRerollCatalyst(raw)
                && this.moveItemStackTo(raw, dedicated, dedicated + 1, false)) {
            } else if ((raw.is(net.minecraftforge.common.Tags.Items.ENCHANTING_FUELS) || EasyMagicCompat.isEnchantingCatalyst(raw))
                && this.moveItemStackTo(raw, 1, 2, false)) {
            } else if (!this.moveItemStackTo(raw, 0, 1, false)) result = ItemStack.EMPTY;
        }
        if (raw.isEmpty()) slot.set(ItemStack.EMPTY);
        slot.setChanged();
        cir.setReturnValue(result);
    }
}
