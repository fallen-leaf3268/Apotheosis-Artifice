package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.enchant.MechanicalRavenEnchantTile;
import com.apotheosis_artifice.enchant.EnchantingDiscounts;
import com.apotheosis_artifice.enchant.MechanicalRavenEnchantingTableBlock;
import com.apotheosis_artifice.enchant.RavenEnchantTile;
import com.apotheosis_artifice.enchant.RavenEnchantingTableBlock;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.compat.EnchHwylaPlugin;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

@Mixin(EnchHwylaPlugin.class)
public class EnchHwylaPluginMixin {

    @Unique
    private static final String APOTHEOSIS_ARTIFICE_REDSTONE_POWERED = "apotheosis_artifice:redstone_powered";

    @Unique
    private static final String APOTHEOSIS_ARTIFICE_MISSING_LAPIS = "apotheosis_artifice:missing_lapis";

    @Inject(method = "register", at = @At("TAIL"), require = 1, remap = false)
    private void apotheosis_artifice_register(IWailaCommonRegistration registration, CallbackInfo ci) {
        if (Apotheosis.enableEnch) {
            registration.registerBlockDataProvider((EnchHwylaPlugin) (Object) this, MechanicalRavenEnchantTile.class);
        }
    }

    @Inject(method = "appendServerData(Lnet/minecraft/nbt/CompoundTag;Lsnownee/jade/api/BlockAccessor;)V", at = @At("TAIL"), require = 1, remap = false)
    private void apotheosis_artifice_appendServerData(CompoundTag data, BlockAccessor accessor, CallbackInfo ci) {
        if (accessor.getBlock() instanceof MechanicalRavenEnchantingTableBlock) {
            boolean powered = accessor.getLevel().hasNeighborSignal(accessor.getPosition());
            data.putBoolean(APOTHEOSIS_ARTIFICE_REDSTONE_POWERED, powered);
            data.putBoolean(APOTHEOSIS_ARTIFICE_MISSING_LAPIS, Apotheosis.enableEnch && powered
                && accessor.getBlockEntity() instanceof MechanicalRavenEnchantTile tile && tile.isMissingAutoEnchantLapis());
        }
    }

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

            float maxE = Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), com.apotheosis_artifice.ApotheosisConfig.getMaxEterna());
            tooltip.add(Component.translatable("info.apotheosis.eterna.t", String.format("%.1f", e), String.format("%.1f", maxE)).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("info.apotheosis.quanta.t", String.format("%.1f", q)).withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("info.apotheosis.arcana.t", String.format("%.1f", a)).withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("info.apotheosis.rectification.t", String.format("%.1f", Mth.clamp(shelf.rectification(), -100, 100))).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("info.apotheosis.clues.t", String.format("%d", shelf.clues())).withStyle(ChatFormatting.DARK_AQUA));
            EnchantingDiscounts.appendBonuses(EnchantingDiscounts.gather(accessor.getLevel(), accessor.getPosition()), tooltip::add);
        }
        if (accessor.getBlock() instanceof MechanicalRavenEnchantingTableBlock && accessor.getServerData().contains(APOTHEOSIS_ARTIFICE_REDSTONE_POWERED)) {
            var theme = IThemeHelper.get();
            var state = accessor.getServerData().getBoolean(APOTHEOSIS_ARTIFICE_REDSTONE_POWERED)
                ? theme.success(Component.translatable("tooltip.jade.state_on"))
                : theme.danger(Component.translatable("tooltip.jade.state_off"));
            tooltip.add(Component.translatable("tooltip.jade.state", state));
        }
        if (accessor.getBlock() instanceof MechanicalRavenEnchantingTableBlock && accessor.getServerData().getBoolean(APOTHEOSIS_ARTIFICE_MISSING_LAPIS)) {
            tooltip.add(IThemeHelper.get().danger(Component.translatable("info.apotheosis_artifice.missing_lapis")));
        }
    }
}
