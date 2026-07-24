package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.Apoth.Tiles;
import net.minecraft.world.level.block.entity.BlockEntityType;

@Mixin(BlockEntityType.class)
public class ReforgingTileMixin {

    @Inject(method = "isValid", at = @At("RETURN"), cancellable = true)
    private void apotheosis_artifice_isValid(net.minecraft.world.level.block.state.BlockState state, CallbackInfoReturnable<Boolean> ci) {
        if (!Apotheosis.enableAdventure) return;
        if (!ci.getReturnValue() && (Object) this == Tiles.REFORGING_TABLE.get()) {
            if (state.is(ApotheosisArtificeMod.APOTHEOSIS_REFORGING_TABLE.get())) {
                ci.setReturnValue(true);
            }
        }
    }
}
