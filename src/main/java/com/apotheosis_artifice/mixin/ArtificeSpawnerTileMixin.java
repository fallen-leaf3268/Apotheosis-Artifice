package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.spawner.ArtificeSpawnerData;
import com.apotheosis_artifice.spawner.SpawnerStatData;
import dev.shadowsoffire.apotheosis.spawn.spawner.ApothSpawnerTile;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ApothSpawnerTile.class)
public class ArtificeSpawnerTileMixin implements ArtificeSpawnerData {
    @Unique
    private final SpawnerStatData artifice$stats = new SpawnerStatData();

    public SpawnerStatData artifice$getSpawnerStats() { return artifice$stats; }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void artifice$saveStats(CompoundTag tag, CallbackInfo ci) {
        artifice$stats.save(tag);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void artifice$loadStats(CompoundTag tag, CallbackInfo ci) {
        artifice$stats.load(tag);
    }
}
