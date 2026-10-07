package com.apotheosis_artifice.spawner;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.spawn.modifiers.SpawnerModifier;
import dev.shadowsoffire.apotheosis.spawn.modifiers.SpawnerStats;
import dev.shadowsoffire.apotheosis.spawn.spawner.ApothSpawnerTile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApotheosisArtificeMod.MODID)
public final class SpawnerPresetEvents {

    private static final SpawnerPresetConfirmation<ServerPlayer> CONFIRMATIONS = new SpawnerPresetConfirmation<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        boolean mainhandRune = event.getEntity().getMainHandItem().getItem() instanceof SpawnerPresetRuneItem;
        boolean offhand = event.getHand() == InteractionHand.OFF_HAND;
        if (!(event.getItemStack().getItem() instanceof SpawnerPresetRuneItem rune)) {
            if (offhand && mainhandRune && event.getLevel().getBlockEntity(event.getPos()) instanceof ApothSpawnerTile) {
                cancel(event);
            }
            return;
        }
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof ApothSpawnerTile tile)) {
            if (event.getEntity() instanceof ServerPlayer player) {
                CONFIRMATIONS.clear(player);
            }
            return;
        }
        if (event.getLevel().isClientSide) {
            cancel(event);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (offhand && mainhandRune) {
            cancel(event);
            return;
        }
        var otherStack = player.getItemInHand(offhand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        var recipe = SpawnerModifier.findMatch(tile, event.getItemStack(), otherStack);
        if (recipe == null) {
            CONFIRMATIONS.clear(player);
            return;
        }
        var current = new SpawnerPreset.Stats(SpawnerStats.MIN_DELAY.getValue(tile), SpawnerStats.MAX_DELAY.getValue(tile),
            SpawnerStats.SPAWN_COUNT.getValue(tile), SpawnerStats.MAX_NEARBY_ENTITIES.getValue(tile), SpawnerStats.REQ_PLAYER_RANGE.getValue(tile));
        var actualTarget = current;
        for (var modifier : recipe.getStatModifiers()) {
            for (SpawnerPreset.Stat stat : SpawnerPreset.Stat.values()) {
                if (modifier.stat() == SpawnerStats.REGISTRY.get(stat.id())) {
                    actualTarget = actualTarget.apply(stat, (Short) modifier.value(), (Short) modifier.min(), (Short) modifier.max());
                    break;
                }
            }
        }
        var target = new SpawnerPresetConfirmation.Target(event.getLevel().dimension().location().toString(),
            event.getPos().asLong(), tile, rune, rune.preset(), current, actualTarget, recipe);
        SpawnerPresetConfirmation.Decision decision = CONFIRMATIONS.evaluate(player, target, System.nanoTime(), offhand, mainhandRune);
        if (decision == SpawnerPresetConfirmation.Decision.APPLY) {
            return;
        }
        cancel(event);
        if (decision == SpawnerPresetConfirmation.Decision.PROMPT) {
            player.sendSystemMessage(Component.translatable("message.apotheosis_artifice.spawner_preset.downgrade",
                event.getItemStack().getHoverName()).withStyle(ChatFormatting.YELLOW));
            for (SpawnerPreset.Change change : SpawnerPreset.downgrades(current, actualTarget)) {
                player.sendSystemMessage(Component.translatable("message.apotheosis_artifice.spawner_preset.change",
                    SpawnerStats.REGISTRY.get(change.stat().id()).name(),
                    SpawnerPresetRuneItem.formatValue(change.stat(), change.current()),
                    SpawnerPresetRuneItem.formatValue(change.stat(), change.target())).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CONFIRMATIONS.clear(player);
        }
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
