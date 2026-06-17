package com.apotheosis_artifice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.apotheosis_artifice.affix.RadianceAffix;
import com.apotheosis_artifice.mixin.AttributeAffixAccessor;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
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
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class ApotheosisEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void curioAttributes(CurioAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        boolean hasCurioCat = afxData != null && afxData.contains("curio_artifice");

        if (hasCurioCat) {
            String cc = afxData.getString("curio_artifice");
            if (!cc.startsWith("curio")) {
                return;
            }
            if (cc.length() > 6 && !cc.substring(6).equals(event.getSlotContext().identifier())) {
                return;
            }
        } else if (!AffixHelper.hasAffixes(stack) && SocketHelper.getGems(stack).gems().isEmpty()) {
            return;
        }

        if (AffixHelper.hasAffixes(stack)) {
            for (var inst : AffixHelper.getAffixes(stack).values()) {
                if (inst.affix().get() instanceof AttributeAffix attrAfx) {
                    AttributeAffixAccessor acc = (AttributeAffixAccessor) (Object) attrAfx;
                    LootRarity rarity = inst.rarity().get();
                    if (rarity == null) continue;
                    ResourceLocation rid = RarityRegistry.INSTANCE.getKey(rarity);
                    if (rid == null) continue;
                    for (var e : acc.getValues().entrySet()) {
                        ResourceLocation eid = RarityRegistry.INSTANCE.getKey(e.getKey());
                        if (rid.equals(eid)) {
                            double v = e.getValue().get(inst.level());
                            UUID uuid = affixUUID(stack, inst.affix().getId());
                            event.addModifier(acc.getAttribute(), new AttributeModifier(uuid, "affix:" + inst.affix().getId(), v, acc.getOperation()));
                            break;
                        }
                    }
                }
            }
        }

        LootCategory gemCat = hasCurioCat
            ? LootCategory.byId(afxData.getString("curio_artifice"))
            : LootCategory.forItem(stack);
        if (gemCat != null && !gemCat.isNone()) {
            SocketHelper.getGems(stack).addModifiers(gemCat, EquipmentSlot.CHEST, event::addModifier);
            if (!"curio".equals(gemCat.getName())) {
                LootCategory genericCurio = LootCategory.byId("curio");
                if (genericCurio != null && !genericCurio.isNone()) {
                    SocketHelper.getGems(stack).addModifiers(genericCurio, EquipmentSlot.CHEST, event::addModifier);
                }
            }
        }
    }

    private static UUID affixUUID(ItemStack stack, ResourceLocation affixId) {
        return UUID.nameUUIDFromBytes(("apoth_art:" + affixId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // ───────────────────────────── 光辉词缀：可移动光源 ─────────────────────────────
    // 记录每名玩家当前放置的隐形光块位置（含维度），用于移动时清除旧光源。
    private static final Map<UUID, GlobalPos> RADIANCE_LIGHTS = new HashMap<>();

    @SubscribeEvent
    public void radianceTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel level)) return; // 仅服务端处理

        int light = getMaxRadianceLight(player);
        UUID id = player.getUUID();
        GlobalPos prev = RADIANCE_LIGHTS.get(id);

        // 无光辉 / 玩家死亡 → 清除并退出
        if (light <= 0 || !player.isAlive()) {
            if (prev != null) {
                removeRadianceLight(player.getServer(), prev);
                RADIANCE_LIGHTS.remove(id);
            }
            return;
        }

        BlockPos desired = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        GlobalPos desiredGp = GlobalPos.of(level.dimension(), desired);

        // 位置或维度变化 → 先清除旧光源
        if (prev != null && !prev.equals(desiredGp)) {
            removeRadianceLight(player.getServer(), prev);
            RADIANCE_LIGHTS.remove(id);
            prev = null;
        }

        BlockState cur = level.getBlockState(desired);
        if (prev == null) {
            // 仅在空气处放置，绝不覆盖玩家放置的方块
            if (cur.isAir()) {
                level.setBlock(desired, radianceLightState(light), Block.UPDATE_ALL);
                RADIANCE_LIGHTS.put(id, desiredGp);
            }
        } else if (cur.is(Blocks.LIGHT)) {
            // 同一格：亮度变化时更新
            if (cur.getValue(LightBlock.LEVEL) != light) {
                level.setBlock(desired, radianceLightState(light), Block.UPDATE_ALL);
            }
        } else if (cur.isAir()) {
            // 我们的光块被外部清除了，重新放置
            level.setBlock(desired, radianceLightState(light), Block.UPDATE_ALL);
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
            lvl.setBlock(gp.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** 扫描玩家所有匹配槽位的饰品，取“光辉”词缀的最大光照强度。 */
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
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        boolean hasCurioCat = afxData != null && afxData.contains("curio_artifice");

        if (hasCurioCat) {
            String cc = afxData.getString("curio_artifice");
            if (!cc.startsWith("curio")) return false;
            if (cc.length() > 6 && !cc.substring(6).equals(slotId)) return false;
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
                    AffixHelper.copyFrom(stack, arrow);
                }
            }
        });
    }

    @SubscribeEvent
    public void onShieldBlock(ShieldBlockEvent event) {
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
