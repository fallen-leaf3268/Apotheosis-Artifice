package com.apotheosis_artifice.enchant;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class MechanicalRavenEnchantScreen extends RavenEnchantScreen {

    private static final ResourceLocation GUI_TEXTURE =
        new ResourceLocation("apotheosis_artifice", "textures/gui/mechanical_enchanting_table.png");

    public MechanicalRavenEnchantScreen(EnchantmentMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    public ResourceLocation getGuiTexture() {
        return GUI_TEXTURE;
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

        if (((MechanicalRavenEnchantMenu) this.menu).isOutputPending()) {
            var slot = this.menu.getSlot(0);
            gfx.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, 0xFFFFC65A);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (this.hoveredSlot == this.menu.getSlot(0) && ((MechanicalRavenEnchantMenu) this.menu).isOutputPending()) {
            tooltip.add(Component.translatable("gui.apotheosis_artifice.mechanical_raven.output_pending").withStyle(ChatFormatting.GOLD));
        }
        return tooltip;
    }
}
