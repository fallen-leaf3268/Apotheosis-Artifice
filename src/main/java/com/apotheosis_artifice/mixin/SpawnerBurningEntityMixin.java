package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.spawner.SpawnerStatData;
import com.apotheosis_artifice.spawner.SpawnerStatRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class SpawnerBurningEntityMixin {
    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void artifice$saveExtendedFire(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        Entity entity = (Entity) (Object) this;
        CompoundTag saved = cir.getReturnValue();
        int remaining = entity.getRemainingFireTicks();
        if (SpawnerStatRules.needsExtendedFireSave(entity.getPersistentData().getBoolean(SpawnerStatData.BURNING_ID), remaining)) {
            saved.putInt(SpawnerStatData.EXTENDED_FIRE_TAG, remaining);
            saved.putShort("Fire", Short.MAX_VALUE);
        } else saved.remove(SpawnerStatData.EXTENDED_FIRE_TAG);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void artifice$loadExtendedFire(CompoundTag tag, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!entity.fireImmune() && entity.getPersistentData().getBoolean(SpawnerStatData.BURNING_ID)
            && tag.contains(SpawnerStatData.EXTENDED_FIRE_TAG, Tag.TAG_INT)) {
            int remaining = tag.getInt(SpawnerStatData.EXTENDED_FIRE_TAG);
            if (remaining > Short.MAX_VALUE) entity.setRemainingFireTicks(remaining);
        }
    }
}
