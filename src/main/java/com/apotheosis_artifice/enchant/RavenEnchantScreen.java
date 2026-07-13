package com.apotheosis_artifice.enchant;

import org.jetbrains.annotations.Nullable;
import com.apotheosis_artifice.ApotheosisConfig;
import com.apotheosis_artifice.ApotheosisNetwork;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantScreen;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;

public class RavenEnchantScreen extends ApothEnchantScreen {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RavenEnchantScreen.class);

    private static final net.minecraft.resources.ResourceLocation CURIOS_TEX =
        new net.minecraft.resources.ResourceLocation("apotheosis_artifice", "textures/gui/enchanting_table.png");

    private static final int BAR_X = 59, BAR_W = 110, BAR_H = 5;
    private static final int ETERNA_Y = 75, QUANTA_Y = 85, ARCANA_Y = 95;
    private static final int HANDLE_U = 122, HANDLE_W = 4, HANDLE_H = 7;
    private static final int HANDLE_V_ETERNA = 197, HANDLE_V_QUANTA = 204, HANDLE_V_ARCANA = 211;

    private final RavenEnchantMenu ravenMenu;
    protected float curE, curQ, curA;
    @Nullable private DragStat dragging;
    protected boolean dirty;

    public RavenEnchantScreen(EnchantmentMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        this.ravenMenu = (RavenEnchantMenu) container;
        var s = ravenMenu.getRavenStats();
        this.curE = Math.min(s.eterna(), eternaMax());
        this.curQ = Mth.clamp(s.quanta(), 0, ApotheosisConfig.MAX_QUANTA.get());
        this.curA = Mth.clamp(s.arcana(), 0, ApotheosisConfig.MAX_ARCANA.get());
        LOGGER.info("RavenEnchantScreen created: max_quanta={}, max_arcana={}, eternaMax={}", ApotheosisConfig.MAX_QUANTA.get(), ApotheosisConfig.MAX_ARCANA.get(), eternaMax());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.curE = Math.min(this.curE, eternaMax());
        this.curQ = Mth.clamp(this.curQ, 0, ApotheosisConfig.MAX_QUANTA.get());
        this.curA = Mth.clamp(this.curA, 0, ApotheosisConfig.MAX_ARCANA.get());
        this.eterna = this.curE;
        this.lastEterna = this.eterna;
        this.quanta = this.curQ;
        this.lastQuanta = this.quanta;
        this.arcana = this.curA;
        this.lastArcana = this.arcana;
        if (this.dirty) {
            ItemStack input = getInputItem();
            ApotheosisNetwork.CHANNEL.sendToServer(new SetRavenStatsPacket(this.curE, this.curQ, this.curA, input.isEmpty() ? ItemStack.EMPTY : input.copy()));
            this.dirty = false;
        }
    }

    /**
     * 获取用于同步的输入物品。子类可覆写以使用不同的槽位索引。
     */
    protected ItemStack getInputItem() {
        return this.menu.getSlot(0).getItem();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        LOGGER.info("mouseClicked: mx={}, my={}, max_quanta={}, max_arcana={}", mx, my, ApotheosisConfig.MAX_QUANTA.get(), ApotheosisConfig.MAX_ARCANA.get());
        if (hoverBar(mx, my, ETERNA_Y)) { dragging = DragStat.E; updateVal(mx, eternaMax()); return true; }
        if (hoverBar(mx, my, QUANTA_Y)) { dragging = DragStat.Q; updateVal(mx, ApotheosisConfig.MAX_QUANTA.get()); return true; }
        if (hoverBar(mx, my, ARCANA_Y)) { dragging = DragStat.A; updateVal(mx, ApotheosisConfig.MAX_ARCANA.get()); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging != null) {
            float max = switch (dragging) {
                case E -> eternaMax();
                case Q -> ApotheosisConfig.MAX_QUANTA.get();
                case A -> ApotheosisConfig.MAX_ARCANA.get();
            };
            LOGGER.info("mouseDragged: mx={}, max={}, dragging={}", mx, max, dragging);
            updateVal(mx, max);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (dragging != null) { dragging = null; dirty = true; return true; }
        return super.mouseReleased(mx, my, btn);
    }

    public void transferSetSliders(float e, float q, float a) {
        this.curE = Mth.clamp(e, 0, eternaMax());
        this.curQ = Mth.clamp(q, 0, ApotheosisConfig.MAX_QUANTA.get());
        this.curA = Mth.clamp(a, 0, ApotheosisConfig.MAX_ARCANA.get());
        this.dirty = true;
    }

    private void updateVal(double mx, float max) {
        double t = Mth.clamp((mx - (this.leftPos + BAR_X)) / (double)BAR_W, 0, 1);
        float v = (float)(Math.round(t * max * 2) / 2.0);
        v = Mth.clamp(v, 0, max);
        LOGGER.info("updateVal: mx={}, max={}, t={}, v={}", mx, max, t, v);
        switch (dragging) {
            case E -> { if (v != curE) { curE = v; dirty = true; } }
            case Q -> { if (v != curQ) { curQ = v; dirty = true; } }
            case A -> { if (v != curA) { curA = v; dirty = true; } }
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        float[] jei = RavenEnchantMenu.consumeJEISliders();
        if (jei != null) {
            this.curE = Mth.clamp(jei[0], 0, eternaMax());
            this.curQ = Mth.clamp(jei[1], 0, ApotheosisConfig.MAX_QUANTA.get());
            this.curA = Mth.clamp(jei[2], 0, ApotheosisConfig.MAX_ARCANA.get());
        }
        this.ravenMenu.syncStatsToSliders(this.curE, this.curQ, this.curA);
        super.render(gfx, mx, my, pt);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float pt, int mx, int my) {
        float displayMaxQ = ApotheosisConfig.MAX_QUANTA.get();
        float displayMaxA = ApotheosisConfig.MAX_ARCANA.get();
        // 转换为百分比给父类渲染条
        this.quanta = displayMaxQ > 0 ? (this.curQ / displayMaxQ) * 100.0f : 0;
        this.arcana = displayMaxA > 0 ? (this.curA / displayMaxA) * 100.0f : 0;
        // 父类按 curE/getAbsoluteMaxEterna 计算条宽；若我们的上限更大需按比例缩放，防止填充条溢出
        float absMaxE = EnchantingStatRegistry.getAbsoluteMaxEterna();
        this.eterna = eternaMax() > 0 ? this.curE * (absMaxE / eternaMax()) : this.curE;
        this.lastEterna = this.eterna;
        this.lastQuanta = this.quanta;
        this.lastArcana = this.arcana;
        super.renderBg(gfx, pt, mx, my);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        drawHandle(gfx, x + BAR_X, y + ETERNA_Y, this.curE, eternaMax(), HANDLE_V_ETERNA);
        drawHandle(gfx, x + BAR_X, y + QUANTA_Y, this.curQ, displayMaxQ, HANDLE_V_QUANTA);
        drawHandle(gfx, x + BAR_X, y + ARCANA_Y, this.curA, displayMaxA, HANDLE_V_ARCANA);
    }

    private void drawHandle(GuiGraphics gfx, int bx, int by, float val, float max, int v) {
        float clampedVal = Mth.clamp(val, 0, max);
        // 计算像素位置，钳制不超过纹理最大宽度
        int barPixelWidth = (int)(clampedVal * (BAR_W / Math.max(max, 1)));
        barPixelWidth = Math.min(barPixelWidth, BAR_W);
        int tipX = bx + barPixelWidth;
        gfx.blit(CURIOS_TEX, tipX - HANDLE_W/2, by + (BAR_H - HANDLE_H)/2, HANDLE_U, v, HANDLE_W, HANDLE_H);
    }

    private boolean hoverBar(double mx, double my, int barY) {
        return mx >= this.leftPos + BAR_X && mx < this.leftPos + BAR_X + BAR_W
            && my >= this.topPos + barY - 3 && my < this.topPos + barY + BAR_H + 3;
    }

    private static float eternaMax() {
        return Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
    }

    private enum DragStat { E, Q, A }
}
