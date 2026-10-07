package com.apotheosis_artifice.spawner;

import java.math.BigDecimal;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apotheosis.spawn.modifiers.SpawnerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class SpawnerPresetRuneItem extends Item {

    private final SpawnerPreset preset;

    public SpawnerPresetRuneItem(Properties properties, SpawnerPreset preset) {
        super(properties);
        this.preset = preset;
    }

    public SpawnerPreset preset() {
        return this.preset;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.apotheosis_artifice.spawner_preset.absolute").withStyle(ChatFormatting.GRAY));
        for (SpawnerPreset.Stat stat : SpawnerPreset.Stat.values()) {
            tooltip.add(Component.translatable("tooltip.apotheosis_artifice.spawner_preset.stat",
                SpawnerStats.REGISTRY.get(stat.id()).name(), formatValue(stat, this.preset.stats().value(stat)))
                .withStyle(ChatFormatting.GRAY));
        }
    }

    static Component formatValue(SpawnerPreset.Stat stat, int value) {
        if (stat == SpawnerPreset.Stat.MIN_DELAY || stat == SpawnerPreset.Stat.MAX_DELAY) {
            String seconds = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(20)).stripTrailingZeros().toPlainString();
            return Component.translatable("tooltip.apotheosis_artifice.spawner_preset.ticks", value, seconds);
        }
        if (stat == SpawnerPreset.Stat.REQ_PLAYER_RANGE) {
            return Component.translatable("tooltip.apotheosis_artifice.spawner_preset.blocks", value);
        }
        return Component.literal(Integer.toString(value));
    }
}
