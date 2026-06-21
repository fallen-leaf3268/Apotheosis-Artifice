package com.apotheosis_artifice.mixin;

import java.util.Map;
import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.ApotheosisEvents;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(at = @At("TAIL"), method = "doPostDamageEffects(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;)V")
    private static void curiosforge_doPostDamageEffects(LivingEntity user, Entity target, CallbackInfo ci) {
        if (user == null) return;
        forEachCurio(user, (stack, inst) -> {
            int oldInvul = target.invulnerableTime;
            target.invulnerableTime = 0;
            try {
                inst.doPostAttack(user, target);
            } finally {
                target.invulnerableTime = oldInvul;
            }
        }, (stack) -> {
            SocketHelper.getGems(stack).doPostAttack(user, target);
        });
    }

    @Inject(at = @At("TAIL"), method = "doPostHurtEffects(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;)V")
    private static void curiosforge_doPostHurtEffects(LivingEntity user, Entity attacker, CallbackInfo ci) {
        if (user == null) return;
        forEachCurio(user, (stack, inst) -> {
            inst.doPostHurt(user, attacker);
        }, (stack) -> {
            SocketHelper.getGems(stack).doPostHurt(user, attacker);
        });
    }

    private static void forEachCurio(LivingEntity user, BiConsumer<ItemStack, AffixInstance> affixFn, java.util.function.Consumer<ItemStack> gemFn) {
        LazyOptional<ICuriosItemHandler> curiosInv = user.getCapability(CuriosCapability.INVENTORY);
        curiosInv.ifPresent(handler -> {
            Map<String, ICurioStacksHandler> curios = handler.getCurios();
            for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                String slotId = entry.getKey();
                IDynamicStackHandler stackHandler = entry.getValue().getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    ItemStack stack = stackHandler.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    if (!ApotheosisEvents.curiosforge_matchesSlot(stack, slotId)) continue;
                    var affixes = AffixHelper.getAffixes(stack);
                    for (AffixInstance inst : affixes.values()) {
                        affixFn.accept(stack, inst);
                    }
                    gemFn.accept(stack);
                }
            }
        });
    }
}
