package com.apotheosis_artifice.gemcase;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class GemCaseBlockItem extends BlockItem {

    private final int maxCount;

    public GemCaseBlockItem(Block block, Item.Properties props) {
        super(block, props);
        this.maxCount = block instanceof GemCaseBlock gcb ? gcb.getMaxCount() : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> tooltip, TooltipFlag flag) {
        int stored = 0;
        int uniqueGems = 0;
        CompoundTag tag = BlockItem.getBlockEntityData(stack);
        if (tag != null && tag.contains("gems")) {
            CompoundTag gems = tag.getCompound("gems");
            uniqueGems = gems.size();
            for (String gemKey : gems.getAllKeys()) {
                CompoundTag rarityTag = gems.getCompound(gemKey);
                for (String rarityKey : rarityTag.getAllKeys()) {
                    stored += rarityTag.getInt(rarityKey);
                }
            }
        }
        if (stored > 0) {
            tooltip.add(Component.translatable("tooltip.apotheosis_artifice.gem_case.capacity",
                GemCaseBlock.formatCount(stored), GemCaseBlock.formatCount(maxCount)).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.apotheosis_artifice.gem_case.unique_gems",
                uniqueGems).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.apotheosis_artifice.gem_case.capacity_max",
                GemCaseBlock.formatCount(maxCount)).withStyle(ChatFormatting.GOLD));
        }
    }
}
