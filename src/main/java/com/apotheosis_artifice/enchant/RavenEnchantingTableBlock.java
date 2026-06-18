package com.apotheosis_artifice.enchant;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import com.apotheosis_artifice.ApotheosisArtificeMod;

public class RavenEnchantingTableBlock extends ApothEnchantBlock implements BookTexturedTable {

    public static final ResourceLocation BOOK_TEXTURE_ID = new ResourceLocation(ApotheosisArtificeMod.MODID, "raven_book");

    public RavenEnchantingTableBlock() { super(); }

    @Override
    public ResourceLocation getBookTextureId() { return BOOK_TEXTURE_ID; }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder ctx) {
        ItemStack s = new ItemStack(this);
        var te = ctx.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (te instanceof RavenEnchantTile rt) {
            CompoundTag tag = new CompoundTag();
            rt.saveAdditional(tag);
            if (!tag.isEmpty()) {
                BlockItem.setBlockEntityData(s, rt.getType(), tag);
            }
        }
        return Arrays.asList(s);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RavenEnchantTile(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide && type == ApotheosisArtificeMod.RAVEN_ENCHANTING_TILE.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<?>) (lvl, pos, st, be) -> {
                ((EnchantmentTableBlockEntity) be).bookAnimationTick(lvl, pos, st, (EnchantmentTableBlockEntity) be);
            };
        }
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RavenEnchantTile rt) {
            RavenTableStats stats = rt.getRavenStats();
            NetworkHooks.openScreen((ServerPlayer) player,
                new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new RavenEnchantMenu(id, inv, ContainerLevelAccess.create(level, pos), rt, pos, stats),
                    rt.getDisplayName()),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeFloat(stats.eterna());
                    buf.writeFloat(stats.quanta());
                    buf.writeFloat(stats.arcana());
                });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && state.hasBlockEntity()) {
            level.removeBlockEntity(pos);
        }
    }
}
