package com.apotheosis_artifice;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

public class PortableSalvagingItem extends Item {

    static final ThreadLocal<UUID> openingToolId = new ThreadLocal<>();

    public PortableSalvagingItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            // 给每把工具分配唯一 ID
            if (!stack.getOrCreateTag().hasUUID("ToolId")) {
                stack.getTag().putUUID("ToolId", UUID.randomUUID());
            }
            openingToolId.set(stack.getTag().getUUID("ToolId"));

            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.apotheosis.salvage");
                }
                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                    return new PortableSalvagingMenu(id, inv);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apotheosis_artifice.portable_salvaging_tool.desc").withStyle(ChatFormatting.GRAY));
    }
}
