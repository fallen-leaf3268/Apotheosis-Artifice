package com.apotheosis_artifice.mixin;

import java.util.UUID;

import com.apotheosis_artifice.CatOverride;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

@Mixin(targets = "top.theillusivec4.curios.mixin.CuriosImplMixinHooks", remap = false)
public class CurioAttributeAccessMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/type/capability/ICurio;getAttributeModifiers(Ltop/theillusivec4/curios/api/SlotContext;Ljava/util/UUID;)Lcom/google/common/collect/Multimap;"), require = 1)
    private static Multimap<Attribute, AttributeModifier> artifice$readCurioAttributes(ICurio curio, SlotContext context, UUID id) {
        ItemStack previous = CatOverride.setCurioAccess(curio.getStack());
        try {
            return curio.getAttributeModifiers(context, id);
        } finally {
            CatOverride.setCurioAccess(previous);
        }
    }

    @Redirect(method = "getAttributeModifiers", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"), require = 1)
    private static boolean artifice$dispatchCurioAttributes(IEventBus bus, Event event) {
        ItemStack previous = CatOverride.setCurioAccess(((CurioAttributeModifierEvent) event).getItemStack());
        try {
            return bus.post(event);
        } finally {
            CatOverride.setCurioAccess(previous);
        }
    }
}
