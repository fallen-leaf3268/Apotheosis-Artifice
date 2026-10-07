package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.spawner.ArtificeSpawnerStats;
import dev.shadowsoffire.apotheosis.spawn.spawner.ApothSpawnerTile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ApothSpawnerTile.SpawnerLogicExt.class)
public abstract class ArtificeSpawnerLogicMixin extends BaseSpawner {
    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z", remap = true), require = 1)
    private boolean artifice$applySpawnAttributes(ServerLevel level, Entity entity) {
        ArtificeSpawnerStats.applySpawnAttributes((ApothSpawnerTile) getSpawnerBlockEntity(), entity);
        return level.tryAddFreshEntityWithPassengers(entity);
    }
}
