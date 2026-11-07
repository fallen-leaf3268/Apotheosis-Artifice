package com.apotheosis_artifice.curios;

import net.minecraft.world.entity.LivingEntity;

public interface CurioLoadState {
    boolean artifice$isLoadingCurioData();

    static boolean isLoading(LivingEntity entity) {
        return entity instanceof CurioLoadState state && state.artifice$isLoadingCurioData();
    }
}
