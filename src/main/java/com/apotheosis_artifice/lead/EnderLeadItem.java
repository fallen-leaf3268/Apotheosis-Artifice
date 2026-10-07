package com.apotheosis_artifice.lead;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class EnderLeadItem extends Item implements EnderLeadAccess {
    private final EnderLeadTier tier;

    public EnderLeadItem(Properties properties, EnderLeadTier tier) {
        super(properties.stacksTo(1).durability(tier.durability()));
        this.tier = tier;
    }

    public EnderLeadTier artifice$getLeadTier() { return tier; }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return EnderLeadBehavior.capture(stack, player, target, hand, tier);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return EnderLeadBehavior.useOn(context, tier);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return !tier.unbreakable() && super.isDamageable(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) { return false; }

    @Override
    public boolean isFoil(ItemStack stack) { return tier == EnderLeadTier.OCCULT || EnderLeadData.containsEntity(stack.getTag()); }

    @Override
    public CompoundTag getShareTag(ItemStack stack) { return EnderLeadData.shareTag(stack.getTag()); }

    @Override
    public Component getName(ItemStack stack) { return EnderLeadBehavior.displayName(stack, super.getName(stack)); }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        EnderLeadBehavior.appendTooltip(stack, tooltip);
    }
}
