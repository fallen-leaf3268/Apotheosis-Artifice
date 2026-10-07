package com.apotheosis_artifice.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.ApotheosisConfig;
import com.apotheosis_artifice.enchant.EnchantingDiscounts;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import dev.shadowsoffire.apotheosis.util.CommonTooltipUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(CommonTooltipUtil.class)
public class CommonTooltipUtilMixin {

    @Inject(method = "appendBlockStats", at = @At("TAIL"), remap = false)
    private static void artifice$appendShelfDiscounts(Level world, BlockState state, BlockPos pos, Consumer<Component> tooltip, CallbackInfo ci) {
        if (Apotheosis.enableEnch) {
            var bonuses = EnchantingDiscounts.forBlock(state.getBlock());
            if (bonuses.lapis() == 0 && bonuses.experience() == 0) return;
            boolean hasHeader = EnchantingStatRegistry.getEterna(state, world, pos) != 0
                || EnchantingStatRegistry.getQuanta(state, world, pos) != 0
                || EnchantingStatRegistry.getArcana(state, world, pos) != 0
                || EnchantingStatRegistry.getQuantaRectification(state, world, pos) != 0
                || EnchantingStatRegistry.getBonusClues(state, world, pos) != 0;
            EnchantingDiscounts.appendShelfBonuses(bonuses, tooltip, !hasHeader);
        }
    }

    @Inject(method = "appendTableStats", at = @At("TAIL"), remap = false)
    private static void artifice$appendTableDiscounts(Level world, BlockPos pos, Consumer<Component> tooltip, CallbackInfo ci) {
        EnchantingDiscounts.appendBonuses(EnchantingDiscounts.gather(world, pos), tooltip);
    }

    @Redirect(
        method = "appendTableStats",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry;getAbsoluteMaxEterna()F",
            ordinal = 0,
            remap = false),
        remap = false)
    private static float artifice_tableEternaMax() {
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.getMaxEterna());
    }

    @Redirect(
        method = "appendTableStats",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F", ordinal = 0),
        remap = false)
    private static float artifice_tableQuantaNoCap(float a, float b) {
        return b;
    }

    @Redirect(
        method = "appendTableStats",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F", ordinal = 1),
        remap = false)
    private static float artifice_tableArcanaNoCap(float a, float b) {
        return b;
    }
}
