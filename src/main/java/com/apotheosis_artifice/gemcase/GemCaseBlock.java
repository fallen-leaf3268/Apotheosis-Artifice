package com.apotheosis_artifice.gemcase;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.placebo.menu.MenuUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GemCaseBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    private static final DecimalFormat FMT = new DecimalFormat("##.#");

    private final int maxCount;

    public GemCaseBlock(int maxCount) {
        super(Properties.of().mapColor(MapColor.WOOD).strength(2.5F).noOcclusion());
        this.maxCount = maxCount;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public int getMaxCount() { return maxCount; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            MenuUtil.openGui(player, pos, GemCaseMenu::new);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return new dev.shadowsoffire.placebo.menu.SimplerMenuProvider<>(world, pos, GemCaseMenu::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (maxCount == Integer.MAX_VALUE) {
            return new GemCaseTile.AdvancedGemCaseTile(pos, state);
        }
        return new GemCaseTile.BasicGemCaseTile(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ApotheosisArtificeMod.GEM_CASE_TILE.get() && type != ApotheosisArtificeMod.ENDER_GEM_CASE_TILE.get()) return null;
        return level.isClientSide ? (lvl, pos, st, be) -> ((GemCaseTile) be).clientTick(lvl, pos, st) : null;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack s = new ItemStack(this);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof GemCaseTile tile) {
            CompoundTag tag = new CompoundTag();
            tile.saveGemData(tag);
            if (!tag.isEmpty()) {
                BlockItem.setBlockEntityData(s, tile.getType(), tag);
            }
        }
        return s;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        CompoundTag tag = BlockItem.getBlockEntityData(stack);
        if (tag != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GemCaseTile tile) {
                tile.loadGemData(tag);
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder ctx) {
        ItemStack s = new ItemStack(this);
        BlockEntity te = ctx.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (te instanceof GemCaseTile tile) {
            CompoundTag tag = new CompoundTag();
            tile.saveGemData(tag);
            if (!tag.isEmpty()) {
                BlockItem.setBlockEntityData(s, tile.getType(), tag);
            }
        }
        return Arrays.asList(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    public static String formatCount(int n) {
        if (n <= 9999) return String.valueOf(n);
        int log = (int) Math.log10(n);
        if (log <= 6) {
            return FMT.format(n / 1000D) + "K";
        }
        else if (log <= 8) {
            return FMT.format(n / 1000000D) + "M";
        }
        else {
            return FMT.format(n / 1000000000D) + "B";
        }
    }
}
