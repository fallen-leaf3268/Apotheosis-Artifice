package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.affix.HeartBaseListener;
import com.apotheosis_artifice.curios.CurioLoadState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class CurioEntityLoadMixin implements CurioLoadState {
    @Unique
    private boolean artifice$loadingCurioData;

    @Override
    public boolean artifice$isLoadingCurioData() {
        return artifice$loadingCurioData;
    }

    @Inject(method = "load", at = @At("HEAD"), require = 1)
    private void artifice$beginCurioLoad(CompoundTag tag, CallbackInfo ci) {
        artifice$loadingCurioData = true;
    }

    @Inject(method = "load", at = @At("RETURN"), require = 1)
    private void artifice$finishCurioLoad(CompoundTag tag, CallbackInfo ci) {
        artifice$loadingCurioData = false;
        if ((Object) this instanceof LivingEntity entity && !entity.level().isClientSide()) {
            HeartBaseListener.reconcileAfterLoad(entity);
        }
    }
}
