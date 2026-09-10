package com.apotheosis_artifice.affix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.apotheosis_artifice.ApotheosisEvents;
import com.apotheosis_artifice.AttributeHelper;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.event.SlotModifiersUpdatedEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class HeartBaseListener {

    private static final String APPLIED = "apotheosis_artifice:attribute_base_bonuses";
    private static final String UNIQUE_SLOTS = "apotheosis_artifice:unique_slot_bonuses";
    private static final Set<LivingEntity> APPLYING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<LivingEntity> PENDING = Collections.newSetFromMap(new IdentityHashMap<>());

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
        apply(entity);
    }

    @SubscribeEvent
    public void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (!player.getPersistentData().contains(APPLIED)) {
            CompoundTag legacyBonuses = new CompoundTag();
            scanTotalBonus(player).forEach((attribute, bonus) -> {
                AttributeInstance inst = player.getAttribute(attribute);
                ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attribute);
                if (inst != null && key != null && inst.getBaseValue() == attribute.getDefaultValue() + bonus) {
                    legacyBonuses.putFloat(key.toString(), bonus);
                }
            });
            player.getPersistentData().put(APPLIED, legacyBonuses);
        }
        apply(player);
    }

    private static void apply(LivingEntity entity) {
        if (!APPLYING.add(entity)) {
            PENDING.add(entity);
            return;
        }
        try {
            do {
                PENDING.remove(entity);
                applyBaseBonuses(entity);
                applyUniqueSlotBonuses(entity);
            } while (PENDING.remove(entity));
        } finally {
            APPLYING.remove(entity);
            PENDING.remove(entity);
        }
    }

    private static void applyBaseBonuses(LivingEntity entity) {
        Map<Attribute, Float> currentBonuses = scanTotalBonus(entity);
        CompoundTag savedBonuses = entity.getPersistentData().getCompound(APPLIED);
        Map<Attribute, Float> prevBonuses = new HashMap<>();
        for (String key : savedBonuses.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            Attribute attribute = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
            if (attribute != null) prevBonuses.put(attribute, savedBonuses.getFloat(key));
        }
        CompoundTag nextBonuses = new CompoundTag();

        // 合并所有相关 attribute(当前 + 之前),保证脱下最后一个时仍能撤销
        Set<Attribute> allAttrs = new HashSet<>();
        allAttrs.addAll(currentBonuses.keySet());
        allAttrs.addAll(prevBonuses.keySet());

        for (Attribute attribute : allAttrs) {
            AttributeInstance inst = entity.getAttribute(attribute);
            if (inst == null) continue;
            float current = currentBonuses.getOrDefault(attribute, 0f);
            double previous = prevBonuses.getOrDefault(attribute, 0f);
            double actual = inst.getBaseValue();
            double expected = resolveBaseValue(actual, previous, current);
            double diff = expected - actual;
            if (diff != 0) {
                AttributeHelper.addToBase(entity, attribute, "apotheosis_artifice:attribute_base", diff);
            }
            ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (key != null && current != 0) nextBonuses.putFloat(key.toString(), current);
        }

        entity.getPersistentData().put(APPLIED, nextBonuses);
    }

    public static double resolveBaseValue(double currentBase, double previousBonus, double nextBonus) {
        return currentBase - previousBonus + nextBonus;
    }

    public record SlotBonus(String slot, UUID uuid, float amount) {}

    public static Map<String, Map<UUID, Float>> aggregateUniqueBonuses(Iterable<SlotBonus> bonuses) {
        Map<String, Map<UUID, Float>> result = new HashMap<>();
        for (SlotBonus bonus : bonuses) {
            result.computeIfAbsent(bonus.slot(), ignored -> new HashMap<>())
                .merge(bonus.uuid(), bonus.amount(), Math::max);
        }
        return result;
    }

    private static void applyUniqueSlotBonuses(LivingEntity entity) {
        ICuriosItemHandler handler = entity.getCapability(CuriosCapability.INVENTORY).orElse(null);
        if (handler == null) return;
        List<SlotBonus> bonuses = new ArrayList<>();
        for (var entry : handler.getCurios().entrySet()) {
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.isEmpty() || !ApotheosisEvents.curiosforge_matchesSlot(stack, entry.getKey())) continue;
                for (AffixInstance inst : AffixHelper.getAffixes(stack).values()) {
                    if (!inst.isValid() || !(inst.affix().get() instanceof CurioSlotBonusAffix bonus)) continue;
                    if (bonus.getFixedUuid().isEmpty()) continue;
                    bonuses.add(new SlotBonus(bonus.getSlotId(), UUID.fromString(bonus.getFixedUuid()),
                        bonus.getBonus(inst.rarity().get(), inst.level())));
                }
            }
        }
        Map<String, Map<UUID, Float>> current = aggregateUniqueBonuses(bonuses);
        CompoundTag previous = entity.getPersistentData().getCompound(UNIQUE_SLOTS);
        CompoundTag next = new CompoundTag();
        Set<String> slotIds = new HashSet<>(previous.getAllKeys());
        slotIds.addAll(current.keySet());
        for (String slotId : slotIds) {
            Map<UUID, Float> amounts = current.getOrDefault(slotId, Map.of());
            CompoundTag saved = previous.getCompound(slotId);
            Set<UUID> ids = new HashSet<>(amounts.keySet());
            for (String uuid : saved.getAllKeys()) ids.add(UUID.fromString(uuid));
            CompoundTag nextSlot = new CompoundTag();
            ICurioStacksHandler slots = handler.getCurios().get(slotId);
            for (UUID uuid : ids) {
                Float amount = amounts.get(uuid);
                if (amount != null) nextSlot.putFloat(uuid.toString(), amount);
                if (slots == null) continue;
                slots.getCachedModifiers().removeIf(modifier -> modifier.getId().equals(uuid));
                AttributeModifier existing = slots.getModifiers().get(uuid);
                if (amount != null && existing != null && existing.getAmount() == amount
                    && existing.getOperation() == AttributeModifier.Operation.ADDITION) continue;
                handler.removeSlotModifier(slotId, uuid);
                if (amount != null) {
                    handler.addTransientSlotModifier(slotId, uuid, "apotheosis_artifice:curio_slot_bonus:" + slotId,
                        amount, AttributeModifier.Operation.ADDITION);
                }
            }
            if (!nextSlot.isEmpty()) next.put(slotId, nextSlot);
        }
        entity.getPersistentData().put(UNIQUE_SLOTS, next);
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
                if (!ApotheosisEvents.curiosforge_matchesSlot(stack, entry.getKey())) continue;
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
