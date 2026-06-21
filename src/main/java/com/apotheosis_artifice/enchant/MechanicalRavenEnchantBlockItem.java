package com.apotheosis_artifice.enchant;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.List;

public class MechanicalRavenEnchantBlockItem extends BlockItem {

    public MechanicalRavenEnchantBlockItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && (getBoundLibPos(stack) != null || getContBoundPos(stack) != null)) {
            if (!level.isClientSide) {
                CompoundTag beTag = stack.getTagElement("BlockEntityTag");
                if (beTag != null) {
                    beTag.remove("lb_dim"); beTag.remove("lb_x"); beTag.remove("lb_y"); beTag.remove("lb_z"); beTag.remove("lb_name");
                    beTag.remove("cont_dim"); beTag.remove("cont_x"); beTag.remove("cont_y"); beTag.remove("cont_z"); beTag.remove("cont_name");
                }
                player.displayClientMessage(
                    Component.translatable("info.apotheosis_artifice.binder.unbound").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;

        var blockId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());

        if (blockId != null && "apotheosis".equals(blockId.getNamespace())
            && ("library".equals(blockId.getPath()) || "ender_library".equals(blockId.getPath()))) {
            return bindLibrary(level, pos, ctx);
        }

        var be = level.getBlockEntity(pos);
        if (be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
            return bindContainer(level, pos, ctx);
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return super.useOn(ctx);
    }

    private InteractionResult bindLibrary(Level level, BlockPos pos, UseOnContext ctx) {
        if (!level.isClientSide) {
            ItemStack stack = ctx.getItemInHand();
            CompoundTag beTag = stack.getOrCreateTagElement("BlockEntityTag");
            beTag.putString("lb_dim", level.dimension().location().toString());
            beTag.putInt("lb_x", pos.getX()); beTag.putInt("lb_y", pos.getY()); beTag.putInt("lb_z", pos.getZ());
            Component blockName = new net.minecraft.world.item.ItemStack(level.getBlockState(pos).getBlock()).getHoverName();
            beTag.putString("lb_name", Component.Serializer.toJson(blockName));
            ctx.getPlayer().displayClientMessage(
                Component.translatable("info.apotheosis_artifice.binder.bound", blockName, pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult bindContainer(Level level, BlockPos pos, UseOnContext ctx) {
        if (!level.isClientSide) {
            ItemStack stack = ctx.getItemInHand();
            CompoundTag beTag = stack.getOrCreateTagElement("BlockEntityTag");
            beTag.putString("cont_dim", level.dimension().location().toString());
            beTag.putInt("cont_x", pos.getX()); beTag.putInt("cont_y", pos.getY()); beTag.putInt("cont_z", pos.getZ());
            Component blockName = new net.minecraft.world.item.ItemStack(level.getBlockState(pos).getBlock()).getHoverName();
            beTag.putString("cont_name", Component.Serializer.toJson(blockName));
            ctx.getPlayer().displayClientMessage(
                Component.literal("§a容器绑定至 ").append(blockName).append(" " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    public static BlockPos getBoundLibPos(ItemStack stack) {
        CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        if (beTag == null || !beTag.contains("lb_x")) return null;
        return new BlockPos(beTag.getInt("lb_x"), beTag.getInt("lb_y"), beTag.getInt("lb_z"));
    }

    @Nullable
    public static ResourceLocation getBoundLibDim(ItemStack stack) {
        CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        if (beTag == null || !beTag.contains("lb_dim")) return null;
        String dim = beTag.getString("lb_dim");
        return dim.isEmpty() ? null : ResourceLocation.tryParse(dim);
    }

    @Nullable
    public static BlockPos getContBoundPos(ItemStack stack) {
        CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        if (beTag == null || !beTag.contains("cont_x")) return null;
        return new BlockPos(beTag.getInt("cont_x"), beTag.getInt("cont_y"), beTag.getInt("cont_z"));
    }

    @Nullable
    public static ResourceLocation getContBoundDim(ItemStack stack) {
        CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        if (beTag == null || !beTag.contains("cont_dim")) return null;
        String dim = beTag.getString("cont_dim");
        return dim.isEmpty() ? null : ResourceLocation.tryParse(dim);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        BlockPos libPos = getBoundLibPos(stack);
        BlockPos contPos = getContBoundPos(stack);
        boolean hasAny = libPos != null || contPos != null;

        if (libPos != null) {
            CompoundTag beTag = stack.getTagElement("BlockEntityTag");
            Component name = beTag != null && beTag.contains("lb_name") ? Component.Serializer.fromJson(beTag.getString("lb_name")) : null;
            ResourceLocation dim = getBoundLibDim(stack);
            String dimS = dim != null ? " §7[" + dim + "]" : "";
            if (name != null) {
                list.add(Component.literal("").append(name).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" " + libPos.getX() + ", " + libPos.getY() + ", " + libPos.getZ()).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(dimS).withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
        if (contPos != null) {
            CompoundTag beTag = stack.getTagElement("BlockEntityTag");
            Component name = beTag != null && beTag.contains("cont_name") ? Component.Serializer.fromJson(beTag.getString("cont_name")) : null;
            ResourceLocation dim = getContBoundDim(stack);
            String dimS = dim != null ? " §7[" + dim + "]" : "";
            list.add(Component.literal("")
                .append(name != null ? name : Component.literal("?"))
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" " + contPos.getX() + ", " + contPos.getY() + ", " + contPos.getZ()).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(dimS).withStyle(ChatFormatting.DARK_GRAY)));
        }
        if (!hasAny) {
            list.add(Component.translatable("info.apotheosis_artifice.mechanical_raven.bind_hint").withStyle(ChatFormatting.GRAY));
        }
    }
}
