package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.CatOverride;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.event.CuriosEventHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

@Mixin(value = CuriosEventHandler.class, remap = false)
public class CurioCallbackAccessMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/type/capability/ICurio;curioTick(Ltop/theillusivec4/curios/api/SlotContext;)V"), require = 1)
    private static void artifice$tickCurio(ICurio curio, SlotContext context) {
        ItemStack previous = CatOverride.setCurioAccess(curio.getStack());
        try {
            curio.curioTick(context);
        } finally {
            CatOverride.setCurioAccess(previous);
        }
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/type/capability/ICurio;onEquip(Ltop/theillusivec4/curios/api/SlotContext;Lnet/minecraft/world/item/ItemStack;)V"), require = 1)
    private static void artifice$equipCurio(ICurio curio, SlotContext context, ItemStack previousStack) {
        ItemStack previous = CatOverride.setCurioAccess(curio.getStack());
        try {
            curio.onEquip(context, previousStack);
        } finally {
            CatOverride.setCurioAccess(previous);
        }
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/type/capability/ICurio;onUnequip(Ltop/theillusivec4/curios/api/SlotContext;Lnet/minecraft/world/item/ItemStack;)V"), require = 1)
    private static void artifice$unequipCurio(ICurio curio, SlotContext context, ItemStack replacement) {
        ItemStack previous = CatOverride.setCurioAccess(curio.getStack());
        try {
            curio.onUnequip(context, replacement);
        } finally {
            CatOverride.setCurioAccess(previous);
        }
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/type/capability/ICurio;onEquipFromUse(Ltop/theillusivec4/curios/api/SlotContext;)V"), require = 1)
    private static void artifice$equipCurioFromUse(ICurio curio, SlotContext context) {
        ItemStack previous = CatOverride.setCurioAccess(curio.getStack());
        try {
            curio.onEquipFromUse(context);
        } finally {
            CatOverride.setCurioAccess(previous);
        }
    }

    @Mixin(value = CurioStacksHandler.class, remap = false)
    public static class Overflow {
        @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/type/capability/ICurio;onUnequip(Ltop/theillusivec4/curios/api/SlotContext;Lnet/minecraft/world/item/ItemStack;)V"), require = 1)
        private static void artifice$removeOverflowCurio(ICurio curio, SlotContext context, ItemStack replacement) {
            ItemStack previous = CatOverride.setCurioAccess(curio.getStack());
            try {
                curio.onUnequip(context, replacement);
            } finally {
                CatOverride.setCurioAccess(previous);
            }
        }
    }
}
