package com.apotheosis_artifice;

import com.apotheosis_artifice.enchant.MechanicalRavenEnchantScreen;
import com.apotheosis_artifice.enchant.RavenEnchantScreen;
import com.apotheosis_artifice.gemcase.GemCaseScreen;
import com.apotheosis_artifice.gemcase.GemCaseTileRenderer;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ApotheosisArtificeMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    public static final ResourceLocation REFORGING_HAMMER = new ResourceLocation(ApotheosisArtificeMod.MODID, "item/reforging_hammer");

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(REFORGING_HAMMER);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpecial(ApotheosisArtificeMod.LOOT_PINATA_FIREWORK.get(),
            (type, level, x, y, z, dx, dy, dz) -> new FireworkParticles.Starter(level, x, y, z, 0, 0, 0,
                Minecraft.getInstance().particleEngine, createLootPinataFireworks()));
        event.registerSpecial(ApotheosisArtificeMod.LOOT_PINATA_SILENT.get(), (type, level, x, y, z, dx, dy, dz) -> null);
    }

    public static CompoundTag createLootPinataFireworks() {
        CompoundTag burst = new CompoundTag();
        burst.putByte("Type", (byte) 0);
        burst.putIntArray("Colors", new int[]{DyeColor.YELLOW.getFireworkColor(), DyeColor.LIME.getFireworkColor(),
            DyeColor.MAGENTA.getFireworkColor()});
        burst.putBoolean("Trail", false);
        burst.putBoolean("Flicker", true);
        ListTag explosions = new ListTag();
        explosions.add(burst);
        CompoundTag fireworks = new CompoundTag();
        fireworks.put("Explosions", explosions);
        return fireworks;
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ApotheosisArtificeMod.PORTABLE_SALVAGING_MENU.get(), PortableSalvagingScreen::new);
            MenuScreens.register(ApotheosisArtificeMod.GEM_CASE_MENU.get(), GemCaseScreen::new);
            MenuScreens.register(ApotheosisArtificeMod.RAVEN_ENCHANTING_TABLE_MENU.get(), RavenEnchantScreen::new);
            MenuScreens.register(ApotheosisArtificeMod.MECHANICAL_RAVEN_TABLE_MENU.get(), MechanicalRavenEnchantScreen::new);
            BlockEntityRenderers.register(ApotheosisArtificeMod.RAVEN_ENCHANTING_TILE.get(), net.minecraft.client.renderer.blockentity.EnchantTableRenderer::new);
            BlockEntityRenderers.register(ApotheosisArtificeMod.MECHANICAL_RAVEN_TILE.get(), net.minecraft.client.renderer.blockentity.EnchantTableRenderer::new);
            BlockEntityRenderers.register(ApotheosisArtificeMod.GEM_CASE_TILE.get(), GemCaseTileRenderer::new);
            BlockEntityRenderers.register(ApotheosisArtificeMod.ENDER_GEM_CASE_TILE.get(), GemCaseTileRenderer::new);
        });
    }
}
