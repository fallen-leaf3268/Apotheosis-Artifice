package com.apotheosis_artifice;

import com.apotheosis_artifice.enchant.EnchantingDiscounts;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ApotheosisEvents {

    @SubscribeEvent(priority = EventPriority.LOW)
    public void enchantingShelfTooltip(ItemTooltipEvent event) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableEnch
            || !(event.getItemStack().getItem() instanceof BlockItem item)) return;
        var bonuses = EnchantingDiscounts.forBlock(item.getBlock());
        if (bonuses.lapis() == 0 && bonuses.experience() == 0) return;
        var tooltip = event.getToolTip();
        boolean hasHeader = tooltip.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents contents
            && contents.getKey().equals("info.apotheosis.ench_stats"));
        EnchantingDiscounts.appendShelfBonuses(bonuses, tooltip::add, !hasHeader);
    }

    public static volatile long recipeCacheVersion = 0;

    @SubscribeEvent
    public void onTagsUpdated(net.minecraftforge.event.TagsUpdatedEvent ev) {
        recipeCacheVersion++;
    }
}
