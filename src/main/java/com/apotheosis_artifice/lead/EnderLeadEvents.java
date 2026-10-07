package com.apotheosis_artifice.lead;

import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "apotheosis_artifice")
public final class EnderLeadEvents {
    private EnderLeadEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getItemStack().getItem() instanceof EnderLeadAccess lead)) return;
        var result = EnderLeadBehavior.capture(event.getItemStack(), event.getEntity(), event.getTarget(), event.getHand(), lead.artifice$getLeadTier());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public static void addLegacyTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof EnderLeadAccess && !(event.getItemStack().getItem() instanceof EnderLeadItem)) {
            EnderLeadBehavior.appendDescription(event.getItemStack(), event.getToolTip());
        }
    }
}
