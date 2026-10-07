package com.apotheosis_artifice.spawner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import com.apotheosis_artifice.mixin.EchoingLootInvoker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "apotheosis_artifice")
public final class SpawnerStatEvents {
    private static final ThreadLocal<Set<LivingEntity>> ACTIVE_ECHOES = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    private SpawnerStatEvents() {}

    @SubscribeEvent
    public static void echoDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        int echoes = SpawnerStatRules.normalizeEchoes(entity.getPersistentData().getInt(SpawnerStatData.ECHOING_ID));
        if (echoes == 0 || entity.level().isClientSide) return;
        if (!entity.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
            || !((EchoingLootInvoker) entity).artifice$shouldDropLoot()) return;
        Set<LivingEntity> active = ACTIVE_ECHOES.get();
        if (!active.add(entity)) return;
        var extraDrops = new ArrayList<ItemEntity>();
        var previousCapture = entity.captureDrops(extraDrops);
        try {
            for (int i = 0; i < echoes; i++) ((EchoingLootInvoker) entity).artifice$dropFromLootTable(event.getSource(), event.isRecentlyHit());
        } finally {
            entity.captureDrops(previousCapture);
            active.remove(entity);
            if (active.isEmpty()) ACTIVE_ECHOES.remove();
        }
        event.getDrops().addAll(extraDrops);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void echoExperience(LivingExperienceDropEvent event) {
        int echoes = SpawnerStatRules.normalizeEchoes(event.getEntity().getPersistentData().getInt(SpawnerStatData.ECHOING_ID));
        if (echoes > 0 && !event.getEntity().level().isClientSide) {
            event.setDroppedExperience(SpawnerStatRules.echoExperience(event.getDroppedExperience(), echoes));
        }
    }
}
