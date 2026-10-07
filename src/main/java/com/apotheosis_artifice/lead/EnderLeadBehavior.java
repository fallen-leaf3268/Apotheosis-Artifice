package com.apotheosis_artifice.lead;

import java.util.List;
import java.util.function.Function;

import com.mojang.logging.LogUtils;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.spawn.SpawnerModule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.Tags;
import org.slf4j.Logger;

public final class EnderLeadBehavior {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TagKey<EntityType<?>> CAPTURING_NOT_SUPPORTED = entityTag("apotheosis_artifice", "capturing_not_supported");
    private static final TagKey<EntityType<?>> COMMON_CAPTURING_NOT_SUPPORTED = entityTag("c", "capturing_not_supported");
    private static final TagKey<EntityType<?>> FORGE_CAPTURING_NOT_SUPPORTED = entityTag("forge", "capturing_not_supported");
    private static final TagKey<EntityType<?>> BLACKLISTED_FROM_SPAWNERS = entityTag("apotheosis_artifice", "blacklisted_from_spawners");
    private static final TagKey<EntityType<?>> UPSTREAM_BLACKLISTED_FROM_SPAWNERS = entityTag("apothic_spawners", "blacklisted_from_spawners");

    private EnderLeadBehavior() {}

    private static TagKey<EntityType<?>> entityTag(String namespace, String path) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(namespace, path));
    }

    public static boolean canCapture(Entity target, EnderLeadTier tier) {
        EntityType<?> type = target.getType();
        boolean blacklisted = type.is(CAPTURING_NOT_SUPPORTED) || type.is(COMMON_CAPTURING_NOT_SUPPORTED)
            || type.is(FORGE_CAPTURING_NOT_SUPPORTED);
        return tier.canCapture(new EnderLeadTier.CaptureTarget(target instanceof Mob, target instanceof Animal,
            target instanceof AmbientCreature, target instanceof WaterAnimal, target.isAlive(), type.is(Tags.EntityTypes.BOSSES),
            blacklisted, target.isRemoved(), target.isPassenger(), target.isVehicle(), type.canSerialize()));
    }

    public static InteractionResult capture(ItemStack stack, Player player, Entity target, InteractionHand hand, EnderLeadTier tier) {
        if (!Apotheosis.enableGarden || player.isSpectator() || stack.getCount() != 1 || EnderLeadData.containsEntity(stack.getTag())
            || !player.level().mayInteract(player, target.blockPosition()) || !canCapture(target, tier)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player.level() instanceof ServerLevel)) return InteractionResult.FAIL;
        CompoundTag original = stack.getTag();
        CompoundTag itemTag = original == null ? new CompoundTag() : original;
        try {
            boolean captured = EnderLeadData.capture(itemTag, () -> {
                CompoundTag saved = new CompoundTag();
                return target.save(saved) ? saved : null;
            }, () -> {
                stack.setTag(itemTag);
                try { target.discard(); }
                catch (RuntimeException error) { LOGGER.warn("Failed removing captured entity {}", target.getUUID(), error); }
                return target.isRemoved();
            }, target.getDisplayName().getString());
            if (!captured) {
                if (original == null && itemTag.isEmpty()) stack.setTag(null);
                return InteractionResult.FAIL;
            }
            player.getInventory().setChanged();
            playSound(player.level(), player.blockPosition());
            return InteractionResult.CONSUME;
        } catch (RuntimeException error) {
            LOGGER.warn("Failed saving entity {} into an ender lead", target.getUUID(), error);
            return InteractionResult.FAIL;
        }
    }

    public static InteractionResult useOn(UseOnContext context, EnderLeadTier tier) {
        ItemStack stack = context.getItemInHand();
        if (!EnderLeadData.containsEntity(stack.getTag())) return InteractionResult.PASS;
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (player == null || player.isSpectator() || stack.getCount() != 1 || !level.mayInteract(player, clicked)
            || !player.mayUseItemAt(clicked, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.FAIL;
        try {
            var saved = EnderLeadData.entityTag(stack.getTag());
            if (saved.isEmpty()) return failure(player, "invalid_data");
            CompoundTag entityTag = saved.orElseThrow();
            var tree = EnderLeadData.tree(entityTag, EnderLeadBehavior::knownType);
            if (tree.isEmpty()) return failure(player, "invalid_data");
            List<EnderLeadData.StoredEntity> expected = tree.orElseThrow();
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(expected.get(0).typeId()));
            if (tier.canBind(Apotheosis.enableGarden, Apotheosis.enableSpawner)
                && level.getBlockEntity(clicked) instanceof SpawnerBlockEntity spawner) {
                if (expected.size() != 1 || entityType.is(BLACKLISTED_FROM_SPAWNERS) || entityType.is(UPSTREAM_BLACKLISTED_FROM_SPAWNERS)
                    || SpawnerModule.bannedMobs.contains(EntityType.getKey(entityType))) return failure(player, "binding_blocked");
                spawner.setEntityId(entityType, level.getRandom());
                spawner.setChanged();
                var state = level.getBlockState(clicked);
                level.sendBlockUpdated(clicked, state, state, 3);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, clicked);
                EnderLeadData.clear(stack.getTag());
                consumeUse(stack, player, context.getHand());
                playSound(level, clicked);
                return InteractionResult.CONSUME;
            }
            BlockPos spawnPos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(context.getClickedFace());
            if (!level.mayInteract(player, spawnPos) || !player.mayUseItemAt(spawnPos, context.getClickedFace(), stack)) return failure(player, "release_blocked");
            for (var stored : expected) for (ServerLevel dimension : serverLevel.getServer().getAllLevels()) {
                if (dimension.getEntity(stored.uuid()) != null) return failure(player, "release_blocked");
            }
            Entity root = EntityType.loadEntityRecursive(entityTag, serverLevel, Function.identity());
            if (root == null) return failure(player, "invalid_data");
            List<Entity> entities = root.getSelfAndPassengers().toList();
            if (entities.size() != expected.size()) return failure(player, "invalid_data");
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = entities.get(i);
                var stored = expected.get(i);
                if (!entity.getUUID().equals(stored.uuid()) || !EntityType.getKey(entity.getType()).toString().equals(stored.typeId())) return failure(player, "invalid_data");
                entity.moveTo(spawnPos.getX() + .5D, spawnPos.getY(), spawnPos.getZ() + .5D, 0F, 0F);
                if (!serverLevel.getWorldBorder().isWithinBounds(entity.getBoundingBox()) || !serverLevel.noCollision(entity)) return failure(player, "release_blocked");
            }
            boolean released = EnderLeadData.release(stack.getTag(), entities,
                entity -> serverLevel.addFreshEntity(entity) && serverLevel.getEntity(entity.getUUID()) == entity,
                entity -> { if (serverLevel.getEntity(entity.getUUID()) == entity) entity.discard(); },
                () -> consumeUse(stack, player, context.getHand()));
            if (!released) return failure(player, "release_blocked");
            player.getInventory().setChanged();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
            playSound(level, spawnPos);
            return InteractionResult.CONSUME;
        } catch (RuntimeException error) {
            LOGGER.warn("Failed releasing an ender lead at {}", clicked, error);
            return failure(player, "release_blocked");
        }
    }

    private static boolean knownType(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && BuiltInRegistries.ENTITY_TYPE.getOptional(key).filter(EntityType::canSerialize).isPresent();
    }

    private static InteractionResult failure(Player player, String key) {
        player.displayClientMessage(Component.translatable("message.apotheosis_artifice.ender_lead." + key).withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
    }

    private static void consumeUse(ItemStack stack, Player player, InteractionHand hand) {
        stack.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(hand));
    }

    private static void playSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.AMBIENT, 1F, 1F);
    }

    private static EntityType<?> heldType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(EnderLeadData.ENTITY_DATA, Tag.TAG_COMPOUND)) return null;
        ResourceLocation id = ResourceLocation.tryParse(tag.getCompound(EnderLeadData.ENTITY_DATA).getString("id"));
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    public static Component displayName(ItemStack stack, Component base) {
        EntityType<?> type = heldType(stack);
        if (type == null) return base;
        CompoundTag tag = stack.getTag();
        Component name = tag.contains(EnderLeadData.NAME, Tag.TAG_STRING) ? Component.literal(tag.getString(EnderLeadData.NAME)) : type.getDescription();
        ChatFormatting color = switch (type.getCategory()) {
            case AMBIENT, CREATURE -> ChatFormatting.DARK_GREEN;
            case MONSTER -> ChatFormatting.RED;
            case WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, WATER_CREATURE, AXOLOTLS -> ChatFormatting.BLUE;
            default -> ChatFormatting.WHITE;
        };
        return Component.translatable("info.apotheosis_artifice.ender_lead.title", base, name.copy().withStyle(color));
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        EntityType<?> type = heldType(stack);
        tooltip.add(type == null ? Component.translatable("info.apotheosis.noentity").withStyle(ChatFormatting.GRAY)
            : Component.translatable("info.apotheosis.containedentity", type.getDescription()).withStyle(ChatFormatting.GRAY));
        appendDescription(stack, tooltip);
    }

    public static void appendDescription(ItemStack stack, List<Component> tooltip) {
        if (!(stack.getItem() instanceof EnderLeadAccess lead)) return;
        String tier = lead.artifice$getLeadTier().name().toLowerCase(java.util.Locale.ROOT);
        tooltip.add(Component.translatable("tooltip.apotheosis_artifice.ender_lead." + tier).withStyle(ChatFormatting.GRAY));
        if (lead.artifice$getLeadTier() == EnderLeadTier.NORMAL && Apotheosis.enableGarden && Apotheosis.enableEnch
            && heldType(stack) == EntityType.WITCH) {
            tooltip.add(Component.translatable("tooltip.apotheosis_artifice.ender_lead.witch_infusion").withStyle(ChatFormatting.GOLD));
        }
    }
}
