package com.apotheosis_artifice.enchant;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public class MechanicalRavenEnchantScreen extends RavenEnchantScreen {

    private static final ResourceLocation CHEST_BG = new ResourceLocation("textures/gui/container/generic_54.png");

    private static final int INPUT_X = 15, INPUT_Y = 17;
    private static final int OUTPUT_X = 35, OUTPUT_Y = 17;

    private boolean initSyncDone = false;

    public MechanicalRavenEnchantScreen(EnchantmentMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (!initSyncDone) {
            initSyncDone = true;
            var rs = ((MechanicalRavenEnchantMenu) this.menu).getRavenStats();
            com.apotheosis_artifice.ApotheosisNetwork.CHANNEL.sendToServer(
                new SetRavenStatsPacket(rs.eterna(), rs.quanta(), rs.arcana()));
        }
    }

    @Override
    protected ItemStack getInputItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void acceptClues(int slot, java.util.List<EnchantmentInstance> clues, boolean all) {
        super.acceptClues(slot, clues, all);
        if (slot < 0 || slot >= this.menu.enchantClue.length) return;
        if (!clues.isEmpty()) {
            this.menu.enchantClue[slot] = BuiltInRegistries.ENCHANTMENT.getId(clues.get(0).enchantment);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float pt, int mx, int my) {
        super.renderBg(gfx, pt, mx, my);
        int x = this.leftPos;
        int y = this.topPos;

        gfx.blit(CHEST_BG, x + INPUT_X - 1, y + INPUT_Y - 1, 7, 17, 18, 18);
        gfx.blit(CHEST_BG, x + OUTPUT_X - 1, y + OUTPUT_Y - 1, 7, 17, 18, 18);
    }
}
