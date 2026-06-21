package com.apotheosis_artifice.enchant;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class MechanicalRavenEnchantingTableBlock extends RavenEnchantingTableBlock {

    public MechanicalRavenEnchantingTableBlock() { super(); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MechanicalRavenEnchantTile(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MechanicalRavenEnchantTile rt) {
            RavenTableStats stats = rt.getRavenStats();
            // 保存 menu 引用，供 buf 回调写入附魔槽内容
            final MechanicalRavenEnchantMenu[] menuRef = new MechanicalRavenEnchantMenu[1];
            NetworkHooks.openScreen((ServerPlayer) player,
                new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> {
                        menuRef[0] = new MechanicalRavenEnchantMenu(id, inv, ContainerLevelAccess.create(level, pos), rt, pos, stats);
                        return menuRef[0];
                    },
                    rt.getDisplayName()),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeFloat(stats.eterna());
                    buf.writeFloat(stats.quanta());
                    buf.writeFloat(stats.arcana());
                    buf.writeItem(menuRef[0] != null ? menuRef[0].getSlot(0).getItem() : rt.getIOInv().getStackInSlot(0));
                });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MechanicalRavenEnchantTile rt) {
                // 物品保存在附魔台中，不掉落
                level.removeBlockEntity(pos);
            } else {
                super.onRemove(state, level, pos, newState, moved);
            }
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ApotheosisArtificeMod.MECHANICAL_RAVEN_TILE.get()) return null;
        if (level.isClientSide) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<?>) (lvl, pos, st, be) ->
                ((EnchantmentTableBlockEntity) be).bookAnimationTick(lvl, pos, st, (EnchantmentTableBlockEntity) be);
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<?>) (Level lvl, BlockPos p, BlockState st, BlockEntity be) ->
            MechanicalRavenEnchantTile.tick(lvl, p, st, (MechanicalRavenEnchantTile) be);
    }
}
