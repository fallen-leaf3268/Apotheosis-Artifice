package com.apotheosis_artifice.affix;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.apotheosis_artifice.AttributeHelper;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.event.SlotModifiersUpdatedEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class HeartBaseListener {

    // 追踪每个玩家每个 attribute 的当前加成,确保脱下最后一个 affix 仍能撤销
    private static final Map<UUID, Map<Attribute, Float>> APPLIED = new HashMap<>();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new HeartBaseListener());
    }

    @SubscribeEvent
    public void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        apply(event.getEntity());
    }

    // 栏位大小变化(玩家脱下 curio_slot_bonus affix → 目标槽位缩容 → 多出物品被自动取下)
    // Curios 在 resize 时不触发 CurioChangeEvent,只触发 SlotModifiersUpdatedEvent
    @SubscribeEvent
    public void onSlotModifiersUpdated(SlotModifiersUpdatedEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        apply(event.getEntity());
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        // 死后重生,旧实体的 APPLIED 记录失效,清掉
        APPLIED.remove(event.getOriginal().getUUID());
        apply(entity);
    }

    @SubscribeEvent
    public void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        apply(player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        APPLIED.remove(event.getEntity().getUUID());
    }

    private static void apply(LivingEntity entity) {
        UUID id = entity.getUUID();
        Map<Attribute, Float> currentBonuses = scanTotalBonus(entity);
        Map<Attribute, Float> prevBonuses = APPLIED.getOrDefault(id, new HashMap<>());

        // 合并所有相关 attribute(当前 + 之前),保证脱下最后一个时仍能撤销
        Set<Attribute> allAttrs = new HashSet<>();
        allAttrs.addAll(currentBonuses.keySet());
        allAttrs.addAll(prevBonuses.keySet());

        for (Attribute attribute : allAttrs) {
            AttributeInstance inst = entity.getAttribute(attribute);
            if (inst == null) continue;
            float current = currentBonuses.getOrDefault(attribute, 0f);
            double expected = attribute.getDefaultValue() + current;
            double actual = inst.getBaseValue();
            double diff = expected - actual;
            if (Math.abs(diff) > 0.001) {
                AttributeHelper.addToBase(entity, attribute, "apotheosis_artifice:attribute_base", diff);
            }
        }

        if (currentBonuses.isEmpty()) {
            APPLIED.remove(id);
        } else {
            APPLIED.put(id, currentBonuses);
        }
    }

    private static Map<Attribute, Float> scanTotalBonus(LivingEntity entity) {
        Map<Attribute, Float> result = new HashMap<>();
        LazyOptional<ICuriosItemHandler> cap = entity.getCapability(CuriosCapability.INVENTORY);
        ICuriosItemHandler handler = cap.orElse(null);
        if (handler == null) return result;

        for (var entry : handler.getCurios().entrySet()) {
            ICurioStacksHandler stacks = entry.getValue();
            IDynamicStackHandler dh = stacks.getStacks();
            for (int i = 0; i < dh.getSlots(); i++) {
                ItemStack stack = dh.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                for (AffixInstance inst : AffixHelper.getAffixes(stack).values()) {
                    if (!inst.isValid()) continue;
                    if (!(inst.affix().get() instanceof AttributeBaseAffix ab)) continue;
                    LootRarity rarity = inst.rarity().get();
                    if (rarity == null) continue;
                    result.merge(ab.getAttribute(), ab.getBonus(rarity, inst.level()), Float::sum);
                }
            }
        }
        return result;
    }
}