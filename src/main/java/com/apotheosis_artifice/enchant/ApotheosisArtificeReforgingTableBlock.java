package com.apotheosis_artifice.enchant;

import java.util.List;

import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableBlock;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ApotheosisArtificeReforgingTableBlock extends ReforgingTableBlock {

    public ApotheosisArtificeReforgingTableBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 1200.0F).lightLevel(s -> 15), 0);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return false;
        return super.canSurvive(state, level, pos);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return null;
        return super.getMenuProvider(state, level, pos);
    }

    @Override
    public LootRarity getMaxRarity() {
        if (RarityRegistry.INSTANCE.getOrderedRarities().isEmpty()) return null;
        return RarityRegistry.getMaxRarity().get();
    }

    @Override
    public void appendHoverText(ItemStack pStack, BlockGetter pLevel, List<Component> list, TooltipFlag pFlag) {
        LootRarity max = this.getMaxRarity();
        if (max != null) {
            list.add(Component.translatable("info.apotheosis_artifice.max_rarity", max.toComponent()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
        net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
        net.minecraft.world.phys.BlockHitResult hit) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return net.minecraft.world.InteractionResult.PASS;
        return super.use(state, level, pos, player, hand, hit);
    }
}
