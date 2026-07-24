package com.apotheosis_artifice.gemcase;

import com.apotheosis_artifice.ApotheosisNetwork;
import com.apotheosis_artifice.ApotheosisNetwork.GemCaseSelectPacket;
import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class GemCaseSelectButton extends AbstractWidget {

    private final GemCaseScreen screen;
    private final int index;
    private Gem gem;
    private int count;

    public GemCaseSelectButton(GemCaseScreen screen, int index, int x, int y) {
        super(x, y, 16, 16, Component.empty());
        this.screen = screen;
        this.index = index;
    }

    public Gem getCurrentGem() { return this.gem; }

    @Override
    public void renderWidget(GuiGraphics gfx, int mx, int my, float pt) {
        if (this.screen.getStartIndex() + this.index >= this.screen.getFilteredGemCount()) {
            this.gem = null;
            return;
        }

        this.gem = this.screen.getGemAt(this.index);
        this.count = this.screen.getCountAt(this.index);
        if (this.gem == null) return;
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return;

        GemCaseMenu menu = this.screen.getMenu();
        int x = this.getX(), y = this.getY();

        ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
        GemItem.setGem(stack, this.gem);

        if (this.count <= 0) {
            GemCaseScreen.renderGhostItem(gfx, stack, x, y);
        } else {
            gfx.renderItem(stack, x, y);
        }

        if (this.count > 1) {
            GemCaseScreen.renderCountText(gfx, GemCaseBlock.formatCount(this.count), x, y, 200, 0xFFFFFFFF);
        }

        if (this.isHovered()) {
            gfx.fill(x, y, x + 16, y + 16, 0x40FFFFFF);
        }
    }

    @Override
    public void onClick(double mx, double my) {
        if (this.gem != null) {
            this.screen.getMenu().setSelectedGem(this.gem);
            DynamicHolder<Gem> holder = GemRegistry.INSTANCE.holder(this.gem);
            if (holder.isBound()) {
                ApotheosisNetwork.CHANNEL.sendToServer(new GemCaseSelectPacket(holder.getId().toString()));
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
