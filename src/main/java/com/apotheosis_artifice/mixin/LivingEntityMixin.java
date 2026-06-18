package com.apotheosis_artifice.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.affix.EffectImmunityAffix;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "canBeAffected", at = @At("RETURN"), cancellable = true)
    private void cf_checkCurioImmunity(MobEffectInstance effectInstance, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.level().isClientSide) return;

        var effect = effectInstance.getEffect();
        LazyOptional<ICuriosItemHandler> curiosInv = self.getCapability(CuriosCapability.INVENTORY);
        curiosInv.ifPresent(handler -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stackHandler = entry.getValue().getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    ItemStack stack = stackHandler.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    for (AffixInstance inst : AffixHelper.getAffixes(stack).values()) {
                        if (inst.affix().get() instanceof EffectImmunityAffix eia && eia.hasEffect(effect)) {
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                }
            }
        });
    }
}
