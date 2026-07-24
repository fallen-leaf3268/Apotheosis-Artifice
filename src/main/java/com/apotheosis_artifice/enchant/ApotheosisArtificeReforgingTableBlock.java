package com.apotheosis_artifice.enchant;

import java.util.List;

import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableBlock;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ApotheosisArtificeReforgingTableBlock extends ReforgingTableBlock {

    public ApotheosisArtificeReforgingTableBlock() {
        // 传 0 给父类 maxRarity 字段，因为父类 getMaxRarity() 用 byOrdinal(this.maxRarity) 取值。
        // 传一个安全的序数避免 IndexOutOfBoundsException，实际逻辑完全由我们的覆写控制。
        super(BlockBehaviour.Properties.of().strength(5.0F, 1200.0F).lightLevel(s -> 15), 0);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return false;
        return super.canSurvive(state, level, pos);
    }

    @Override
    public LootRarity getMaxRarity() {
        // adventure 模块禁用/稀有度未加载时列表为空，RarityRegistry.getMaxRarity()
        // 内部 list.get(size-1) 会抛 IndexOutOfBoundsException（JEI 构建搜索树取
        // tooltip 时崩溃），先判空。
        if (RarityRegistry.INSTANCE.getOrderedRarities().isEmpty()) return null;
        return RarityRegistry.getMaxRarity().get();
    }

    @Override
    public void appendHoverText(ItemStack pStack, BlockGetter pLevel, List<Component> list, TooltipFlag pFlag) {
        // 不调 super.appendHoverText()，父类的实现直接访问 this.maxRarity 字段
        // 且引用的是 Apotheosis 本体的方块翻译键，不适合我们使用。
        LootRarity max = this.getMaxRarity();
        if (max != null) {
            list.add(Component.translatable("info.apotheosis_artifice.max_rarity", max.toComponent()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
        net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
        net.minecraft.world.phys.BlockHitResult hit) {
        // adventure 模块禁用时 Apoth.Menus.REFORGING 未注册，父类开菜单会崩
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return net.minecraft.world.InteractionResult.PASS;
        return super.use(state, level, pos, player, hand, hit);
    }
}
