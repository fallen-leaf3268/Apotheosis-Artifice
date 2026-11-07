package com.apotheosis_artifice.curios;

import com.apotheosis_artifice.affix.HeartBaseListener;
import com.apotheosis_artifice.CatOverride;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.apotheosis_artifice.affix.CurioSlotBonusAffix;
import com.apotheosis_artifice.affix.RadianceAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class CurioApotheosisEvents {

    private static final String MODIFIERS = "apotheosis_artifice:curio_attribute_modifiers";
    private static final Set<LivingEntity> CLEARING = Collections.newSetFromMap(new IdentityHashMap<>());

    public void close() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        for (GlobalPos position : RADIANCE_LIGHTS.values()) removeRadianceLight(server, position);
        RADIANCE_LIGHTS.clear();
        RADIANCE_LEVEL.clear();
    }

    public static boolean clearContributions(LivingEntity entity) {
        if (CurioLoadState.isLoading(entity)) return false;
        if (!hasContributions(entity) && !entity.getCapability(CuriosCapability.INVENTORY).isPresent()) return true;
        if (!CLEARING.add(entity)) return false;
        try {
            boolean complete = HeartBaseListener.clearContributions(entity);
            CompoundTag persistent = entity.getPersistentData();
            var curios = entity.getCapability(CuriosCapability.INVENTORY).orElse(null);
            ListTag modifiers = persistent.getList(MODIFIERS, Tag.TAG_COMPOUND);
            removeAttributeContributions(entity.getAttributes(), modifiers);
            for (int index = 0; index < modifiers.size(); index++) {
                CompoundTag entry = modifiers.getCompound(index);
                UUID id = entry.getUUID("id");
                if (entry.contains("slot") && curios != null) {
                    String slot = entry.getString("slot");
                    var stacks = curios.getCurios().get(slot);
                    if (stacks != null) stacks.getCachedModifiers().removeIf(modifier -> modifier.getId().equals(id));
                    curios.removeSlotModifier(slot, id);
                }
            }
            persistent.remove(MODIFIERS);
            if (entity instanceof Player player) clearRadiance(player.getUUID(), player.getServer());
            return complete;
        } finally {
            CLEARING.remove(entity);
        }
    }

    public static boolean hasContributions(LivingEntity entity) {
        return HeartBaseListener.hasContributions(entity)
            || !entity.getPersistentData().getList(MODIFIERS, Tag.TAG_COMPOUND).isEmpty()
            || RADIANCE_LIGHTS.containsKey(entity.getUUID());
    }

    static void removeAttributeContributions(AttributeMap attributes, ListTag modifiers) {
        int size = modifiers.size();
        modifiers.removeIf(tag -> !((CompoundTag) tag).hasUUID("id"));
        if (modifiers.size() != size) {
            com.apotheosis_artifice.ApotheosisArtificeMod.LOGGER.warn("Discarded {} invalid saved Curio modifier UUIDs", size - modifiers.size());
        }
        for (int index = 0; index < modifiers.size(); index++) {
            CompoundTag entry = modifiers.getCompound(index);
            if (entry.contains("slot")) continue;
            ResourceLocation attributeId = ResourceLocation.tryParse(entry.getString("attribute"));
            Attribute attribute = attributeId == null ? null : ForgeRegistries.ATTRIBUTES.getValue(attributeId);
            var instance = attribute == null ? null : attributes.getInstance(attribute);
            if (instance != null) instance.removeModifier(entry.getUUID("id"));
        }
    }

    private static void addModifier(CurioAttributeModifierEvent event, Attribute attribute, AttributeModifier modifier) {
        event.addModifier(attribute, modifier);
        LivingEntity entity = event.getSlotContext().entity();
        if (entity == null || entity.level().isClientSide) return;
        CompoundTag entry = new CompoundTag();
        entry.putUUID("id", modifier.getId());
        if (attribute instanceof SlotAttribute slot) {
            entry.putString("slot", slot.getIdentifier());
        } else {
            ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (id == null) return;
            entry.putString("attribute", id.toString());
        }
        CompoundTag data = entity.getPersistentData();
        ListTag ledger = data.getList(MODIFIERS, Tag.TAG_COMPOUND);
        if (!ledger.contains(entry)) ledger.add(entry);
        data.put(MODIFIERS, ledger);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void curioAttributes(CurioAttributeModifierEvent event) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return;
        if (event.getSlotContext().entity() != null && CurioLoadState.isLoading(event.getSlotContext().entity())) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (!curiosforge_matchesSlot(stack, event.getSlotContext().identifier())) return;

        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        boolean hasCurioCat = afxData != null && afxData.contains("curio_artifice");

        if (hasCurioCat) {
            String cc = afxData.getString("curio_artifice");
            if (!cc.equals("curio") && !cc.startsWith("curios:")) {
                return;
            }
            if (cc.length() > 7 && !cc.substring(7).equals(event.getSlotContext().identifier())) {
                return;
            }
        } else if (!AffixHelper.hasAffixes(stack) && SocketHelper.getGems(stack).gems().isEmpty()) {
            return;
        }

        if (AffixHelper.hasAffixes(stack)) {
            for (var inst : AffixHelper.getAffixes(stack).values()) {
                if (!inst.isValid()) continue;
                if (inst.affix().get() instanceof CurioSlotBonusAffix csb) {
                    if (csb.getFixedUuidValue() != null && event.getSlotContext().entity() != null
                        && !event.getSlotContext().entity().level().isClientSide()) continue;
                    LootRarity rarity = inst.rarity().get();
                    if (rarity == null) continue;
                    float amount = csb.getBonus(rarity, inst.level());
                    String slotId = csb.getSlotId();
                    UUID uuid = slotBonusUUID(csb, event.getSlotContext());
                    addModifier(event,
                        SlotAttribute.getOrCreate(slotId),
                        new AttributeModifier(uuid, "apotheosis_artifice:curio_slot_bonus:" + slotId, amount, Operation.ADDITION));
                } else {
                    inst.addModifiers(EquipmentSlot.CHEST, (attribute, modifier) -> addModifier(event, attribute,
                        new AttributeModifier(affixUUID(event.getSlotContext(), inst.affix().getId(), modifier.getId()),
                            modifier.getName(), modifier.getAmount(), modifier.getOperation())));
                }
            }
        }

        LootCategory gemCat = hasCurioCat
            ? LootCategory.byId(afxData.getString("curio_artifice"))
            : LootCategory.forItem(stack);
        if (gemCat != null && !gemCat.isNone()) {
            SocketHelper.getGems(stack).addModifiers(gemCat, EquipmentSlot.CHEST, (attribute, modifier) -> addModifier(event, attribute, modifier));
        }
    }

    private static UUID affixUUID(top.theillusivec4.curios.api.SlotContext ctx, ResourceLocation affixId, UUID modifierId) {
        return UUID.nameUUIDFromBytes(
            ("apoth_art:" + ctx.identifier() + ":" + ctx.index() + ":" + affixId + ":" + modifierId)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static UUID slotBonusUUID(CurioSlotBonusAffix csb, top.theillusivec4.curios.api.SlotContext ctx) {
        UUID fixed = csb.getFixedUuidValue();
        if (fixed != null) {
            return fixed;
        }
        return UUID.nameUUIDFromBytes(
            ("apoth_art:csb:" + ctx.identifier() + ":" + ctx.index() + ":" + csb.getSlotId())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static final Map<UUID, GlobalPos> RADIANCE_LIGHTS = new HashMap<>();
    private static final Map<UUID, Integer> RADIANCE_LEVEL = new HashMap<>();
    private static final int RADIANCE_RESCAN_INTERVAL = 10;

    @SubscribeEvent
    public void radianceTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel level)) return;

        UUID id = player.getUUID();
        int light;
        if (player.tickCount % RADIANCE_RESCAN_INTERVAL == 0) {
            light = getMaxRadianceLight(player);
            if (light > 0) RADIANCE_LEVEL.put(id, light);
            else RADIANCE_LEVEL.remove(id);
        } else {
            light = RADIANCE_LEVEL.getOrDefault(id, 0);
        }
        GlobalPos prev = RADIANCE_LIGHTS.get(id);

        if (light <= 0 || !player.isAlive()) {
            if (prev != null) {
                removeRadianceLight(player.getServer(), prev);
                RADIANCE_LIGHTS.remove(id);
            }
            return;
        }

        BlockPos desired = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        GlobalPos desiredGp = GlobalPos.of(level.dimension(), desired);

        if (prev != null && !prev.equals(desiredGp)) {
            removeRadianceLight(player.getServer(), prev);
            RADIANCE_LIGHTS.remove(id);
            prev = null;
        }

        BlockState cur = level.getBlockState(desired);
        if (prev == null) {
            if (cur.isAir()) {
                level.setBlock(desired, radianceLightState(light), Block.UPDATE_CLIENTS);
                RADIANCE_LIGHTS.put(id, desiredGp);
            }
        } else if (cur.is(Blocks.LIGHT)) {
            if (cur.getValue(LightBlock.LEVEL) != light) {
                level.setBlock(desired, radianceLightState(light), Block.UPDATE_CLIENTS);
            }
        } else if (cur.isAir()) {
            level.setBlock(desired, radianceLightState(light), Block.UPDATE_CLIENTS);
        }
    }

    @SubscribeEvent
    public void radianceCleanupLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearRadiance(event.getEntity().getUUID(), event.getEntity().getServer());
    }

    @SubscribeEvent
    public void radianceCleanupDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player p) {
            clearRadiance(p.getUUID(), p.getServer());
        }
    }

    private static void clearRadiance(UUID id, MinecraftServer server) {
        RADIANCE_LEVEL.remove(id);
        GlobalPos gp = RADIANCE_LIGHTS.remove(id);
        if (gp != null) removeRadianceLight(server, gp);
    }

    private static BlockState radianceLightState(int light) {
        return Blocks.LIGHT.defaultBlockState()
            .setValue(LightBlock.LEVEL, light)
            .setValue(LightBlock.WATERLOGGED, false);
    }

    private static void removeRadianceLight(MinecraftServer server, GlobalPos gp) {
        if (server == null) return;
        ServerLevel lvl = server.getLevel(gp.dimension());
        if (lvl == null) return;
        if (lvl.getBlockState(gp.pos()).is(Blocks.LIGHT)) {
            lvl.setBlock(gp.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private int getMaxRadianceLight(Player player) {
        LazyOptional<ICuriosItemHandler> curiosInv = player.getCapability(CuriosCapability.INVENTORY);
        ICuriosItemHandler handler = curiosInv.orElse(null);
        if (handler == null) return 0;

        int max = 0;
        for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
            String slotId = entry.getKey();
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (!curiosforge_matchesSlot(stack, slotId)) continue;
                for (AffixInstance inst : AffixHelper.getAffixes(stack).values()) {
                    if (!inst.isValid()) continue;
                    if (inst.affix().get() instanceof RadianceAffix rad) {
                        LootRarity rarity = inst.rarity().get();
                        if (rarity == null) continue;
                        max = Math.max(max, rad.getLight(rarity, inst.level()));
                    }
                }
            }
        }
        return max;
    }

    public static boolean curiosforge_matchesSlot(ItemStack stack, String slotId) {
        if (!CuriosApotheosisModule.isEnabled()) return false;
        return matchesStoredSlot(stack, slotId);
    }

    static boolean matchesStoredSlot(ItemStack stack, String slotId) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        boolean hasCurioCat = afxData != null && afxData.contains("curio_artifice");

        if (hasCurioCat) {
            String cc = afxData.getString("curio_artifice");
            if (!cc.equals("curio") && !cc.startsWith("curios:")) return false;
            if (cc.length() > 7 && !cc.substring(7).equals(slotId)) return false;
        } else if (afxData != null) {
            LootCategory nativeCat = CuriosApotheosisModule.isDisabled()
                ? CatOverride.forNativeItem(stack) : LootCategory.forItem(stack);
            if (!nativeCat.isNone() && !nativeCat.getName().equals("curio") && !nativeCat.getName().startsWith("curios:")) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingHurtEvent event) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return;
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

                    var affixes = AffixHelper.getAffixes(stack);
                    for (AffixInstance affixInst : affixes.values()) {
                        dmg = affixInst.onHurt(event.getSource(), entity, dmg);
                    }

                    dmg = SocketHelper.getGems(stack).onHurt(event.getSource(), entity, dmg);
                }
            }
            event.setAmount(dmg);
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onArrowFired(EntityJoinLevelEvent event) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (arrow.getPersistentData().getBoolean("apoth.generated")) return;
        Entity shooter = arrow.getOwner();
        if (!(shooter instanceof LivingEntity living)) return;

        LazyOptional<ICuriosItemHandler> curiosInv = living.getCapability(CuriosCapability.INVENTORY);
        curiosInv.ifPresent(handler -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stackHandler = entry.getValue().getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    ItemStack stack = stackHandler.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    if (!curiosforge_matchesSlot(stack, entry.getKey())) continue;
                    AffixHelper.getAffixes(stack).values().forEach(inst -> inst.onArrowFired(living, arrow));
                    SocketHelper.getGems(stack).onArrowFired(living, arrow);
                }
            }
        });
    }

    @SubscribeEvent
    public void onShieldBlock(ShieldBlockEvent event) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        LazyOptional<ICuriosItemHandler> curiosInv = entity.getCapability(CuriosCapability.INVENTORY);
        curiosInv.ifPresent(handler -> {
            float blocked = event.getBlockedDamage();
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stackHandler = entry.getValue().getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    ItemStack stack = stackHandler.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    if (!curiosforge_matchesSlot(stack, entry.getKey())) continue;
                    for (AffixInstance inst : AffixHelper.getAffixes(stack).values()) {
                        blocked = inst.onShieldBlock(entity, event.getDamageSource(), blocked);
                    }
                    blocked = SocketHelper.getGems(stack).onShieldBlock(entity, event.getDamageSource(), blocked);
                }
            }
            if (blocked != event.getOriginalBlockedDamage()) {
                event.setBlockedDamage(blocked);
            }
        });
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return;
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;

        LazyOptional<ICuriosItemHandler> curiosInv = player.getCapability(CuriosCapability.INVENTORY);
        curiosInv.ifPresent(handler -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stackHandler = entry.getValue().getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    ItemStack stack = stackHandler.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    if (!curiosforge_matchesSlot(stack, entry.getKey())) continue;
                    for (AffixInstance inst : AffixHelper.getAffixes(stack).values()) {
                        inst.onBlockBreak(player, event.getLevel(), event.getPos(), event.getState());
                    }
                    SocketHelper.getGems(stack).onBlockBreak(player, event.getLevel(), event.getPos(), event.getState());
                }
            }
        });
    }
}
