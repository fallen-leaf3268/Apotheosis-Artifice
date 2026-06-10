package com.apotheosis_artifice;

import com.apotheosis_artifice.enchant.RavenEnchantScreen;
import com.apotheosis_artifice.gemcase.GemCaseScreen;
import com.apotheosis_artifice.gemcase.GemCaseTileRenderer;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ApotheosisArtificeMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ApotheosisArtificeMod.PORTABLE_SALVAGING_MENU.get(), PortableSalvagingScreen::new);
            MenuScreens.register(ApotheosisArtificeMod.GEM_CASE_MENU.get(), GemCaseScreen::new);
            MenuScreens.register(ApotheosisArtificeMod.RAVEN_ENCHANTING_TABLE_MENU.get(), RavenEnchantScreen::new);
            BlockEntityRenderers.register(ApotheosisArtificeMod.RAVEN_ENCHANTING_TILE.get(), net.minecraft.client.renderer.blockentity.EnchantTableRenderer::new);
            BlockEntityRenderers.register(ApotheosisArtificeMod.GEM_CASE_TILE.get(), GemCaseTileRenderer::new);
            BlockEntityRenderers.register(ApotheosisArtificeMod.ENDER_GEM_CASE_TILE.get(), GemCaseTileRenderer::new);
        });
    }
}
