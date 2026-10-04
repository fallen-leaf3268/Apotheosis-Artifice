package com.apotheosis_artifice.enchant;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public class HolyClothItem extends Item {

    private static final int LEVEL_COST = 30;

    public HolyClothItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || stack.isEmpty() || !slot.isActive()) return false;
        ItemStack target = slot.getItem();
        if (target.isEmpty() || !cleanseTags(target, false)) return false;
        if (!slot.allowModification(player)
            && (slot.container != player.getInventory() || !slot.mayPlace(target))) return false;
        boolean creative = player.getAbilities().instabuild;
        if (tryCleanse(stack, target, player.experienceLevel, creative)) {
            if (!creative) player.giveExperienceLevels(-LEVEL_COST);
            slot.setChanged();
        }
        return true;
    }

    static boolean tryCleanse(ItemStack cloth, ItemStack target, int levels, boolean creative) {
        if (cloth.isEmpty() || target.isEmpty() || !creative && levels < LEVEL_COST) return false;
        if (!cleanseTags(target, true)) return false;
        if (!creative) cloth.shrink(1);
        return true;
    }

    private static boolean cleanseTags(ItemStack target, boolean remove) {
        if (target.getTag() == null) return false;
        boolean changed = false;
        for (String key : new String[]{"Enchantments", "StoredEnchantments"}) {
            ListTag entries = target.getTag().getList(key, Tag.TAG_COMPOUND);
            boolean changedList = false;
            for (int i = entries.size() - 1; i >= 0; i--) {
                ResourceLocation id = EnchantmentHelper.getEnchantmentId(entries.getCompound(i));
                if (id == null || !BuiltInRegistries.ENCHANTMENT.getOptional(id).map(Enchantment::isCurse).orElse(false)) continue;
                if (!remove) return true;
                entries.remove(i);
                changedList = true;
                changed = true;
            }
            if (changedList && entries.isEmpty()) target.removeTagKey(key);
            if (target.getTag() == null) break;
        }
        return changed;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".desc", LEVEL_COST).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
