package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingRecipe;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.menu.BlockEntityMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(targets = "dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu$ReforgingResultSlot")
public class ReforgingSlotPickupMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true, remap = true)
    private void apotheosis_artifice_allowPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        try {
            var slot = this;
            java.lang.reflect.Field outerField = slot.getClass().getDeclaredField("this$0");
            outerField.setAccessible(true);
            Object menuObj = outerField.get(slot);
            if (!(menuObj instanceof dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu menu)) return;

            ItemStack input = menu.getSlot(0).getItem();
            if (input.isEmpty()) return;

            LootRarity rarity = menu.getRarity();
            if (rarity == null) return;

            int slotIdx = ((net.minecraftforge.items.SlotItemHandler)(Object)this).getSlotIndex();
            var handler = ((net.minecraftforge.items.SlotItemHandler)(Object)this).getItemHandler();
            if (handler.extractItem(slotIdx, 1, true).isEmpty()) { cir.setReturnValue(false); return; }

            if ((menu.getSigilCount() < menu.getSigilCost(slotIdx)
                || menu.getMatCount() < menu.getMatCost(slotIdx)
                || player.experienceLevel < menu.getLevelCost(slotIdx))
                && !player.isCreative()) {
                cir.setReturnValue(false); return;
            }

            cir.setReturnValue(true);
        } catch (Exception ignored) {}
    }
}
