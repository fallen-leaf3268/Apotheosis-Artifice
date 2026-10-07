package com.apotheosis_artifice.enchant;

import java.util.function.Consumer;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public final class EnchantingDiscounts {

    private EnchantingDiscounts() {}

    public record Bonuses(int lapis, int experience) {
        public Bonuses {
            lapis = Math.max(0, Math.min(3, lapis));
            experience = Math.max(0, Math.min(100, experience));
        }
    }

    public static Bonuses gather(Level world, BlockPos tablePos) {
        if (!Apotheosis.enableEnch || world == null) return new Bonuses(0, 0);
        int lapis = 0;
        int experience = 0;
        for (BlockPos offset : EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
            if (!ApothEnchantmentMenu.canReadStatsFrom(world, tablePos, offset)) continue;
            Bonuses bonuses = forBlock(world.getBlockState(tablePos.offset(offset)).getBlock());
            lapis += bonuses.lapis();
            experience += bonuses.experience();
        }
        return new Bonuses(lapis, experience);
    }

    public static Bonuses forBlock(Block block) {
        if (!Apotheosis.enableEnch || block == null) return new Bonuses(0, 0);
        var id = ForgeRegistries.BLOCKS.getKey(block);
        return id == null ? new Bonuses(0, 0) : EnchantingDiscountRegistry.INSTANCE.getBonuses(id);
    }

    public static int lapisCost(EnchantmentMenu menu, int base) {
        return EnchantingCostRules.lapisCost(base,
            menu instanceof EnchantingDiscountAccess access ? access.getLapisDiscount() : 0);
    }

    public static int experienceCost(EnchantmentMenu menu, int base) {
        return EnchantingCostRules.experienceCost(base,
            menu instanceof EnchantingDiscountAccess access ? access.getExperienceDiscount() : 0);
    }

    public static void appendBonuses(Bonuses bonuses, Consumer<Component> tooltip) {
        if (bonuses.lapis() > 0) tooltip.accept(Component.translatable("info.apotheosis_artifice.lapis_discount", bonuses.lapis())
            .withStyle(ChatFormatting.BLUE));
        if (bonuses.experience() > 0) tooltip.accept(Component.translatable("info.apotheosis_artifice.experience_discount", String.format("%.2f", (float) bonuses.experience()))
            .withStyle(ChatFormatting.GREEN));
    }

    public static void appendShelfBonuses(Bonuses bonuses, Consumer<Component> tooltip, boolean needsHeader) {
        if (bonuses.lapis() == 0 && bonuses.experience() == 0) return;
        if (needsHeader) tooltip.accept(Component.translatable("info.apotheosis.ench_stats").withStyle(ChatFormatting.GOLD));
        if (bonuses.lapis() > 0) tooltip.accept(Component.translatable("info.apotheosis_artifice.lapis_discount.p", bonuses.lapis())
            .withStyle(ChatFormatting.BLUE));
        if (bonuses.experience() > 0) tooltip.accept(Component.translatable("info.apotheosis_artifice.experience_discount.p", String.format("%.2f", (float) bonuses.experience()))
            .withStyle(ChatFormatting.GREEN));
    }
}
