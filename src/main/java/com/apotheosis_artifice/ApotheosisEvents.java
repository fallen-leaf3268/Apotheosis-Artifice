package com.apotheosis_artifice;

import java.util.Map;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class ApotheosisEvents {

    @SubscribeEvent
    public void curioAttributes(CurioAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        LootCategory cat = LootCategory.forItem(stack);

        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        boolean hasCurioCat = afxData != null && afxData.contains("curio_cat");

        if (hasCurioCat) {
            String curioCat = afxData.getString("curio_cat");
            String expectedSlot = curioCat.startsWith("curio:") ? curioCat.substring(6) : null;
            if (expectedSlot != null && !expectedSlot.equals(event.getSlotContext().identifier())) {
                return;
            }
        } else if (!cat.isNone() && !cat.getName().startsWith("curio")) {
            return;
        }

        if (AffixHelper.hasAffixes(stack)) {
            var affixes = AffixHelper.getAffixes(stack);
            affixes.forEach((afx, inst) -> {
                inst.addModifiers(EquipmentSlot.CHEST, event::addModifier);
            });
        }

        if (!cat.isNone()) {
            SocketHelper.getGems(stack).addModifiers(cat, EquipmentSlot.CHEST, event::addModifier);
        }
    }

    /** 检查某物品是否允许在当前 Curios 槽位生效 */
    public static boolean curiosforge_matchesSlot(ItemStack stack, String slotId) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        boolean hasCurioCat = afxData != null && afxData.contains("curio_cat");

        if (hasCurioCat) {
            String expectedSlot = afxData.getString("curio_cat");
            if (expectedSlot.startsWith("curio:") && !expectedSlot.substring(6).equals(slotId)) {
                return false;
            }
        } else if (afxData != null) {
            LootCategory nativeCat = LootCategory.forItem(stack);
            if (!nativeCat.isNone() && !nativeCat.getName().startsWith("curio")) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        LazyOptional<ICuriosItemHandler> curiosInv = entity.getCapability(CuriosCapability.INVENTORY);
        curiosInv.ifPresent(handler -> {
            float dmg = event.getAmount();
            Map<String, ICurioStacksHandler> curios = handler.getCurios();
            for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                String slotId = entry.getKey();
                IDynamicStackHandler stackHandler = entry.getValue().getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    ItemStack stack = stackHandler.getStackInSlot(i);
                    if (stack.isEmpty()) continue;

                    if (!curiosforge_matchesSlot(stack, slotId)) continue;

                    // 词缀伤害减免
                    var affixes = AffixHelper.getAffixes(stack);
                    for (AffixInstance affixInst : affixes.values()) {
                        dmg = affixInst.onHurt(event.getSource(), entity, dmg);
                    }

                    // 宝石伤害减免
                    dmg = SocketHelper.getGems(stack).onHurt(event.getSource(), entity, dmg);
                }
            }
            event.setAmount(dmg);
        });
    }
}
