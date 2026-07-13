package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.enchant.RavenEnchantTile;
import com.apotheosis_artifice.enchant.RavenEnchantingTableBlock;

import dev.shadowsoffire.apotheosis.ench.compat.EnchHwylaPlugin;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

@Mixin(EnchHwylaPlugin.class)
public class EnchHwylaPluginMixin {

    @Inject(method = "appendTooltip", at = @At("TAIL"), require = 1, remap = false)
    private void apotheosis_artifice_appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config, CallbackInfo ci) {
        if (accessor.getBlock() instanceof RavenEnchantingTableBlock) {
            var shelf = ApothEnchantmentMenu.gatherStats(accessor.getLevel(), accessor.getPosition(), 0);

            float e = 0, q = 0, a = 0;
            var be = accessor.getBlockEntity();
            if (be instanceof RavenEnchantTile rt) {
                var s = rt.getRavenStats();
                e = s.eterna(); q = s.quanta(); a = s.arcana();
            } else {
                e = shelf.eterna(); q = shelf.quanta(); a = shelf.arcana();
            }

            float maxE = Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), com.apotheosis_artifice.ApotheosisConfig.MAX_ETERNA.get());
            tooltip.add(Component.translatable("info.apotheosis.eterna.t", String.format("%.1f", e), String.format("%.1f", maxE)).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("info.apotheosis.quanta.t", String.format("%.1f", q)).withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("info.apotheosis.arcana.t", String.format("%.1f", a)).withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("info.apotheosis.rectification.t", String.format("%.1f", Mth.clamp(shelf.rectification(), -100, 100))).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("info.apotheosis.clues.t", String.format("%d", shelf.clues())).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
