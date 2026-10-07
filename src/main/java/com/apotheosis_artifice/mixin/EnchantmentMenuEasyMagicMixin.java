package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.compat.EasyMagicCompat;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import com.apotheosis_artifice.enchant.MechanicalRavenEnchantMenu;
import net.minecraft.world.Container;
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

    @Redirect(method = "lambda$removed$2(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/EnchantmentMenu;clearContainer(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/Container;)V"), require = 1)
    private void artifice$clearTransientInventory(EnchantmentMenu menu, Player player, Container inventory) {
        if ((Object) this instanceof ApothEnchantmentMenu
            && (EasyMagicCompat.isLoaded() || (Object) this instanceof MechanicalRavenEnchantMenu)) return;
        this.clearContainer(player, inventory);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, require = 1)
    private void artifice$moveEasyMagicStacks(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!EasyMagicCompat.isLoaded() || !((Object) this instanceof ApothEnchantmentMenu menu)
            || (Object) this instanceof MechanicalRavenEnchantMenu
            || EasyMagicCompat.getDedicatedRerollSlot(menu) == null) return;
        if (index < 0 || index >= menu.slots.size()) return;
        Slot slot = menu.slots.get(index);
        boolean dedicated = slot.container == menu.enchantSlots && slot.getContainerSlot() == 2;
        if (!dedicated && slot.container != player.getInventory()) return;
        if (!slot.hasItem()) {
            if (dedicated) cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack raw = slot.getItem();
        ItemStack result = raw.copy();
        if (dedicated) {
            if (!this.artifice$moveToPlayerInventory(raw, player)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else {
            boolean moved = false;
            if (EasyMagicCompat.isRerollCatalyst(raw)) {
                int target = this.artifice$findStorageSlot(menu.enchantSlots, 2);
                moved = target >= 0 && this.moveItemStackTo(raw, target, target + 1, false);
            }
            if (!moved && !raw.is(net.minecraftforge.common.Tags.Items.ENCHANTING_FUELS)
                && EasyMagicCompat.isEnchantingCatalyst(raw)) {
                int target = this.artifice$findStorageSlot(menu.enchantSlots, 1);
                moved = target >= 0 && this.moveItemStackTo(raw, target, target + 1, false);
            }
            if (!moved) return;
        }
        if (raw.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (raw.getCount() == result.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        slot.onTake(player, raw);
        cir.setReturnValue(result);
    }

    @Unique
    private int artifice$findStorageSlot(Container inventory, int containerSlot) {
        for (int i = 0; i < this.slots.size(); i++) {
            Slot slot = this.slots.get(i);
            if (slot.container == inventory && slot.getContainerSlot() == containerSlot) return i;
        }
        return -1;
    }

    @Unique
    private boolean artifice$moveToPlayerInventory(ItemStack stack, Player player) {
        boolean moved = false;
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < this.slots.size() && !stack.isEmpty(); i++) {
                Slot target = this.slots.get(i);
                if (target.container == player.getInventory() && target.hasItem() == (pass == 0)) {
                    moved |= this.moveItemStackTo(stack, i, i + 1, false);
                }
            }
        }
        return moved;
    }
}
