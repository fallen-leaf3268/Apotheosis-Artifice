package com.apotheosis_artifice.gemcase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.apotheosis_artifice.ApotheosisNetwork;
import com.apotheosis_artifice.ApotheosisNetwork.GemCasePagePacket;
import com.apotheosis_artifice.ApotheosisNetwork.GemCaseUpgradePacket;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.client.AdventureContainerScreen;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class GemCaseScreen extends AdventureContainerScreen<GemCaseMenu> {

    public static final ResourceLocation TEXTURE = new ResourceLocation("apotheosis_artifice:textures/gui/gem_case.png");
    public static final Component TITLE = Component.translatable("container.apotheosis_artifice.gem_case");
    public static final int MAX_ROWS = 3;
    public static final int SLOTS_PER_ROW = 6;
    public static final int SLOTS_PER_PAGE = 6;

    protected int startIndex;
    protected int listPage = 0;
    protected int maxListPage = 0;
    protected int page = 0;
    protected int maxPage = 0;
    protected List<Gem> data = new ArrayList<>();
    protected List<GemCaseSelectButton> gemButtons = new ArrayList<>();
    protected List<UpgradeButton> upgradeButtons = new ArrayList<>();
    protected List<Slot> extractSlots = new ArrayList<>();
    protected EditBox filter;

    public GemCaseScreen(GemCaseMenu menu, Inventory inv, Component title) {
        super(menu, inv, TITLE);
        this.imageHeight = 230;
        this.menu.setNotifier(this::containerChanged);
        this.containerChanged();
    }

    @Override
    protected void init() {
        super.init();
        int left = this.getGuiLeft();
        int top = this.getGuiTop();

        this.filter = this.addRenderableWidget(new EditBox(this.font, left + 16, top + 16, 110, 11, this.filter, Component.empty()));
        this.filter.setBordered(false);
        this.filter.setTextColor(0xFF97714F);
        this.filter.setResponder(t -> this.containerChanged());
        this.filter.setCanLoseFocus(true);

        this.gemButtons.clear();
        for (int i = 0; i < MAX_ROWS * SLOTS_PER_ROW; i++) {
            var btn = new GemCaseSelectButton(this, i, left + 21 + i % SLOTS_PER_ROW * 18, top + 31 + i / SLOTS_PER_ROW * 19);
            this.gemButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        this.extractSlots.clear();
        for (Slot s : this.menu.slots) {
            if (s instanceof GemCaseSlot) this.extractSlots.add(s);
        }

        int totalRarities = this.menu.getRarityOrder().size();
        this.maxPage = totalRarities <= SLOTS_PER_PAGE ? 0 : (int)Math.ceil((totalRarities - SLOTS_PER_PAGE) / (double)(SLOTS_PER_PAGE - 1));
        this.page = Math.min(this.page, this.maxPage);

        int pgY = top + 126;
        this.addRenderableWidget(new PageButton(left + 94, pgY, "<", this, false));
        this.addRenderableWidget(new PageButton(left + 112, pgY, ">", this, true));

        this.upgradeButtons.clear();
        for (int i = 1; i < 6; i++) {
            var btn = new UpgradeButton(this, i, left + 30 + (i - 1) * 18, top + 109);
            this.upgradeButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        this.applyPage();
        this.containerChanged();
    }

    public void containerChanged() {
        this.data.clear();
        for (Gem gem : GemRegistry.INSTANCE.getValues()) {
            this.data.add(gem);
        }
        this.applyFilterSort();

        this.maxListPage = Math.max(0, (this.data.size() - 1) / SLOTS_PER_ROW);
        this.listPage = Math.min(this.listPage, this.maxListPage);
        this.startIndex = this.listPage * SLOTS_PER_ROW;

        Gem selected = this.menu.getSelectedGem();
        List<ResourceLocation> order = this.menu.getRarityOrder();
        int offset = this.page * (SLOTS_PER_PAGE - 1);
        for (int i = 0; i < this.upgradeButtons.size() && offset + i + 1 < order.size(); i++) {
            ResourceLocation currentRarityId = order.get(offset + i);
            ResourceLocation nextRarityId = order.get(offset + i + 1);
            UpgradeButton btn = this.upgradeButtons.get(i);

            DynamicHolder<LootRarity> currentHolder = RarityRegistry.INSTANCE.holder(currentRarityId);
            DynamicHolder<LootRarity> nextHolder = RarityRegistry.INSTANCE.holder(nextRarityId);
            Component prevName = currentHolder.isBound()
                ? gemQualityComponent(currentRarityId, currentHolder.get().getColor())
                : Component.literal(currentRarityId.getPath());
            Component nextName = nextHolder.isBound()
                ? gemQualityComponent(nextRarityId, nextHolder.get().getColor())
                : Component.literal(nextRarityId.getPath());

            btn.upgradeMessage = Component.translatable("container.apotheosis_artifice.gem_case.upgrade", prevName, nextName);

            if (selected == null) {
                btn.active = false;
                btn.inactiveMessage = Component.translatable("container.apotheosis_artifice.gem_case.no_gem_selected").withStyle(ChatFormatting.RED);
                btn.match = null;
                continue;
            }

            int currentCount = this.menu.getGemCount(selected, currentRarityId);
            if (currentCount < 2) {
                btn.active = false;
                btn.inactiveMessage = Component.translatable("container.apotheosis_artifice.gem_case.upgrade_no_gems").withStyle(ChatFormatting.RED);
                btn.match = null;
                continue;
            }

            DynamicHolder<LootRarity> nextRarityHolder = RarityRegistry.INSTANCE.holder(nextRarityId);
            if (!nextRarityHolder.isBound() || nextRarityHolder.get().ordinal() > selected.getMaxRarity().ordinal()) {
                btn.active = false;
                btn.inactiveMessage = Component.translatable("container.apotheosis_artifice.gem_case.upgrade_max_rarity").withStyle(ChatFormatting.RED);
                btn.match = null;
                continue;
            }

            GemCaseTile.RarityUpgradeMatch match = this.menu.getUpgradeMatch(currentRarityId);
            if (match != null && match.canUpgrade()) {
                btn.active = true;
                btn.match = match;
                btn.inactiveMessage = null;
            }
            else {
                btn.active = false;
                btn.match = match;
                if (match != null && !match.hasDust()) {
                    btn.inactiveMessage = Component.translatable("container.apotheosis_artifice.gem_case.upgrade_no_dust").withStyle(ChatFormatting.RED);
                } else {
                    btn.inactiveMessage = Component.translatable("container.apotheosis_artifice.gem_case.upgrade_no_materials").withStyle(ChatFormatting.RED);
                }
            }
        }
        for (int i = 0; i < this.upgradeButtons.size(); i++) {
            if (offset + i + 1 >= order.size()) {
                UpgradeButton btn = this.upgradeButtons.get(i);
                btn.active = false;
                btn.match = null;
                btn.inactiveMessage = null;
            }
        }
    }

    private void applyFilterSort() {
        List<Gem> filtered = new ArrayList<>();
        for (Gem gem : this.data) {
            if (!isAllowedByItem(gem)) continue;
            if (!isAllowedBySearch(gem)) continue;
            filtered.add(gem);
        }
        if (filtered.size() <= MAX_ROWS * SLOTS_PER_ROW) {
            this.listPage = 0;
            this.startIndex = 0;
        }
        Collections.sort(filtered, Comparator.<Gem, Boolean>comparing(g -> this.menu.getGemCount(g) <= 0)
            .thenComparing(g -> g.getId().toString()));
        this.data = filtered;
    }

    private boolean isAllowedBySearch(Gem gem) {
        if (this.filter == null || this.filter.getValue().isEmpty()) return true;
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return true;
        String search = this.filter.getValue().toLowerCase();
        ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
        GemItem.setGem(stack, gem);
        return stack.getDisplayName().getString().toLowerCase().contains(search);
    }

    private boolean isAllowedByItem(Gem gem) {
        ItemStack filterStack = this.menu.getSlot(GemCaseMenu.FILTER_SLOT).getItem();
        if (filterStack.isEmpty()) return true;
        for (LootCategory cat : LootCategory.VALUES) {
            if (cat.isNone() || !cat.isValid(filterStack)) continue;
            if (gem.getBonuses().stream().anyMatch(b -> b.getGemClass().types().contains(cat))) {
                return true;
            }
        }
        return false;
    }

    public Gem getGemAt(int index) {
        int idx = this.startIndex + index;
        if (idx >= 0 && idx < this.data.size()) return this.data.get(idx);
        return null;
    }

    public int getCountAt(int index) {
        Gem gem = this.getGemAt(index);
        return gem == null ? 0 : this.menu.getGemCount(gem);
    }

    public int getFilteredGemCount() { return this.data.size(); }
    public int getStartIndex() { return this.startIndex; }

    private void setPage(int newPage) {
        this.page = net.minecraft.util.Mth.clamp(newPage, 0, this.maxPage);
        ApotheosisNetwork.CHANNEL.sendToServer(new GemCasePagePacket(this.page));
        this.applyPage();
        this.containerChanged();
    }

    private void applyPage() {
        List<ResourceLocation> order = this.menu.getRarityOrder();
        int offset = this.page * (SLOTS_PER_PAGE - 1);
        ResourceLocation dummyId = GemCaseMenu.NONE_RARITY;
        for (int i = 0; i < this.extractSlots.size() && i < SLOTS_PER_PAGE; i++) {
            GemCaseSlot gs = (GemCaseSlot) this.extractSlots.get(i);
            int idx = offset + i;
            gs.rarityId = idx < order.size() ? order.get(idx) : dummyId;
        }
    }

    @Override
    public void containerTick() {
        if (this.filter != null) this.filter.tick();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (this.filter != null) {
            if (this.filter.isHovered() && button == 1) {
                this.filter.setValue("");
                return true;
            }
            if (!this.filter.isMouseOver(mx, my)) {
                this.filter.setFocused(false);
            }
        }
        if (isScrollBarActive() && mx >= this.getGuiLeft() + 13 && mx < this.getGuiLeft() + 17
            && my >= this.getGuiTop() + 29 && my < this.getGuiTop() + 132) {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int dir = delta > 0 ? -1 : 1;
        if (my >= this.getGuiTop() + 16 && my < this.getGuiTop() + 80) {
            this.listPage = net.minecraft.util.Mth.clamp(this.listPage + dir, 0, this.maxListPage);
            this.startIndex = this.listPage * SLOTS_PER_ROW;
            return true;
        }
        if (my >= this.getGuiTop() + 80 && my < this.getGuiTop() + 140) {
            this.setPage(this.page + dir);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private boolean isScrollBarActive() { return this.data.size() > MAX_ROWS * SLOTS_PER_ROW; }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int left = this.getGuiLeft();
        int top = this.getGuiTop();
        gfx.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight, 307, 256);
        gfx.blit(TEXTURE, left - 65, top + 16, 198, 0, 65, 193, 307, 256);

        if (this.maxListPage > 0) {
            float pct = (float) this.listPage / this.maxListPage;
            int scrollbarPos = (int) (90F * pct);
            gfx.blit(TEXTURE, left + 13, top + 29 + scrollbarPos, 303, 0, 4, 12, 307, 256);
        } else {
            gfx.blit(TEXTURE, left + 13, top + 29, 303, 12, 4, 12, 307, 256);
        }

        Gem selected = this.menu.getSelectedGem();
        if (selected != null) {
            LootRarity minR = selected.getMinRarity(), maxR = selected.getMaxRarity();
            List<ResourceLocation> order = this.menu.getRarityOrder();
            int pgOff = this.page * (SLOTS_PER_PAGE - 1);
            for (int i = 0; i < SLOTS_PER_PAGE && pgOff + i < order.size(); i++) {
                DynamicHolder<LootRarity> holder = RarityRegistry.INSTANCE.holder(order.get(pgOff + i));
                if (!holder.isBound()) continue;
                LootRarity rarity = holder.get();
                if (!rarity.isAtLeast(minR) || !rarity.isAtMost(maxR)) continue;
                int count = this.menu.getGemCount(selected, order.get(pgOff + i));
                if (count <= 0) {
                    if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) continue;
                    ItemStack ghost = new ItemStack(Adventure.Items.GEM.get());
                    GemItem.setGem(ghost, selected);
                    dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper.setRarity(ghost, rarity);
                    int sx = left + 21 + i * 18;
                    int sy = top + 91;
                    renderGhostItem(gfx, ghost, sx, sy);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderGemCaseCounts(gfx);
        if (this.filter != null) this.filter.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderGemCaseCounts(GuiGraphics gfx) {
        Gem gem = this.menu.getSelectedGem();
        if (gem == null) return;
        int gx = this.getGuiLeft(), gy = this.getGuiTop();
        for (Slot s : this.menu.slots) {
            if (s instanceof GemCaseSlot gss) {
                int count = this.menu.getGemCount(gem, gss.rarityId);
                if (count > 1) {
                    renderCountText(gfx, GemCaseBlock.formatCount(count), gx + gss.x, gy + gss.y, 300, 0xFFFFFFFF);
                }
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics gfx, int x, int y) {
        if (this.hoveredSlot instanceof GemCaseSlot gss && this.menu.getSelectedGem() != null) {
            this.renderGemCaseExtractTooltip(gfx, x, y, gss);
            return;
        }
        this.renderGemCaseTooltips(gfx, x, y);
        super.renderTooltip(gfx, x, y);
    }

    private void renderGemCaseExtractTooltip(GuiGraphics gfx, int x, int y, GemCaseSlot gss) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return;
        Gem gem = this.menu.getSelectedGem();
        int count = this.menu.getGemCount(gem, gss.rarityId);
        ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
        GemItem.setGem(stack, gem);
        DynamicHolder<LootRarity> holder = RarityRegistry.INSTANCE.holder(gss.rarityId);
        if (holder.isBound()) AffixHelper.setRarity(stack, holder.get());

        List<Component> tooltip = new ArrayList<>(stack.getTooltipLines(Minecraft.getInstance().player,
            Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
        if (!tooltip.isEmpty()) {
            tooltip.set(0, tooltip.get(0).copy().withStyle(ChatFormatting.YELLOW));
        }
        if (count <= 0) {
            tooltip.add(1, Component.translatable("container.apotheosis_artifice.gem_case.none_owned").withStyle(ChatFormatting.RED));
            tooltip.add(2, Component.empty());
        }
        gfx.renderComponentTooltip(this.font, tooltip, x, y);
    }

    private static Component gemQualityComponent(ResourceLocation rarityId, TextColor color) {
        String key = "item.apotheosis.gem." + rarityId;
        String raw = Component.translatable(key, Component.literal("")).getString().trim();
        if (!raw.isEmpty() && !raw.equals(key)) {
            return Component.literal(raw).withStyle(Style.EMPTY.withColor(color));
        }
        String customKey = "gem_quality." + rarityId;
        String custom = Component.translatable(customKey).getString();
        if (!custom.isEmpty() && !custom.equals(customKey)) {
            return Component.literal(custom).withStyle(Style.EMPTY.withColor(color));
        }
        return Component.translatable("rarity." + rarityId).withStyle(Style.EMPTY.withColor(color));
    }

    private void renderGemCaseTooltips(GuiGraphics gfx, int x, int y) {
        for (GemCaseSelectButton btn : this.gemButtons) {
            if (!btn.isHovered()) continue;
            Gem gem = btn.getCurrentGem();
            if (gem == null) continue;
            if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) continue;
            int count = this.menu.getGemCount(gem);
            ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
            GemItem.setGem(stack, gem);
            gfx.renderComponentTooltip(this.font,
                List.of(stack.getHoverName().copy().withStyle(ChatFormatting.WHITE)),
                x, y);
            break;
        }

        for (UpgradeButton btn : this.upgradeButtons) {
            if (!btn.isHovered()) continue;
            List<Component> lines = new ArrayList<>();

            if (btn.active && btn.match != null) {
                if (btn.upgradeMessage != null) lines.add(btn.upgradeMessage);

                var m = btn.match;
                if (m.materialRarity() != null) {
                    lines.add(Component.translatable("container.apotheosis_artifice.gem_case.upgrade_cost",
                        Component.literal(m.dustNeeded() + "x").withStyle(ChatFormatting.GOLD),
                        Component.translatable("item.apotheosis.gem_dust").withStyle(ChatFormatting.GRAY),
                        Component.literal(m.matNeeded() + "x").withStyle(ChatFormatting.GOLD),
                        new ItemStack(m.materialRarity().getMaterial()).getHoverName()));
                }

                if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                    lines.add(Component.translatable("container.apotheosis_artifice.gem_case.upgrade_all").withStyle(ChatFormatting.YELLOW));
                }
            }
            else if (btn.inactiveMessage != null) {
                lines.add(btn.inactiveMessage);
            }

            if (!lines.isEmpty()) {
                gfx.renderComponentTooltip(this.font, lines, x, y);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {}

    // ---- 共享渲染工具 ----

    public static void renderGhostItem(GuiGraphics gfx, ItemStack stack, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 0.3F);
        gfx.renderItem(stack, x, y);
        RenderSystem.setShaderColor(1, 1, 1, 1F);
        RenderSystem.disableBlend();
    }

    public static void renderCountText(GuiGraphics gfx, String text, int x, int y, int zOffset, int color) {
        var font = Minecraft.getInstance().font;
        float scale = 1.0f;
        if (text.length() > 2) scale = 2.0f / text.length();
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, zOffset);
        gfx.pose().scale(scale, scale, 1.0F);
        float tx = (x + 16 - (font.width(text) - 1) * scale) / scale;
        float ty = (y + 16 - (font.lineHeight - 2) * scale) / scale;
        gfx.drawString(font, text, (int) tx, (int) ty, color, true);
        gfx.pose().popPose();
    }

    // ---- 升级按钮 ----

    static class UpgradeButton extends AbstractWidget {
        private final GemCaseScreen screen;
        private final int rarityOrdinal;
        Component upgradeMessage = null;
        Component inactiveMessage = null;
        GemCaseTile.RarityUpgradeMatch match = null;

        public UpgradeButton(GemCaseScreen screen, int rarityOrdinal, int x, int y) {
            super(x, y, 16, 16, Component.empty());
            this.screen = screen;
            this.rarityOrdinal = rarityOrdinal;
        }

        @Override
        public void renderWidget(GuiGraphics gfx, int mx, int my, float pt) {
            int x = this.getX(), y = this.getY();
            if (this.active && this.isHovered()) {
                gfx.fill(x - 1, y - 1, x + 17, y, 0xFFFFFFFF);
                gfx.fill(x - 1, y + 16, x + 17, y + 17, 0xFFFFFFFF);
                gfx.fill(x - 1, y, x, y + 16, 0xFFFFFFFF);
                gfx.fill(x + 16, y, x + 17, y + 16, 0xFFFFFFFF);
            }
            if (!this.active) {
                RenderSystem.setShaderColor(0.4F, 0.4F, 0.4F, 1.0F);
            }
            gfx.blit(TEXTURE, x, y, 291, 29, 16, 16, 307, 256);
            if (!this.active) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        @Override
        public void onClick(double mx, double my) {
            boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            ApotheosisNetwork.CHANNEL.sendToServer(new GemCaseUpgradePacket(this.rarityOrdinal, this.screen.page, shift));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }

    static class PageButton extends AbstractWidget {
        private final GemCaseScreen screen;
        private final boolean forward;
        private final String label;

        public PageButton(int x, int y, String label, GemCaseScreen screen, boolean forward) {
            super(x, y, 16, 9, Component.empty());
            this.screen = screen;
            this.forward = forward;
            this.label = label;
        }

        private boolean canPress() {
            return this.forward ? this.screen.page < this.screen.maxPage : this.screen.page > 0;
        }

        @Override
        public void renderWidget(GuiGraphics gfx, int mx, int my, float pt) {
            int x = this.getX(), y = this.getY();
            boolean can = this.canPress();
            int tl = can ? (this.isHovered() ? 0xFFAAAAAA : 0xFF888888) : 0xFF444444;
            int br = can ? 0xFF444444 : 0xFF222222;
            int bg = can ? 0xFF555555 : 0xFF333333;
            gfx.fill(x, y, x + 16, y + 1, tl);
            gfx.fill(x, y + 8, x + 16, y + 9, br);
            gfx.fill(x, y, x + 1, y + 9, tl);
            gfx.fill(x + 15, y, x + 16, y + 9, br);
            gfx.fill(x + 1, y + 1, x + 15, y + 8, bg);
            if (can && this.isHovered()) {
                gfx.fill(x - 1, y - 1, x + 17, y, 0xFFFFFFFF);
                gfx.fill(x - 1, y + 9, x + 17, y + 10, 0xFFFFFFFF);
                gfx.fill(x - 1, y, x, y + 9, 0xFFFFFFFF);
                gfx.fill(x + 16, y, x + 17, y + 9, 0xFFFFFFFF);
            }
            int textColor = can ? 0xFFFFFFFF : 0xFF666666;
            int tx = x + (16 - Minecraft.getInstance().font.width(this.label)) / 2;
            gfx.drawString(Minecraft.getInstance().font, this.label, tx, y + 1, textColor, false);
        }

        @Override
        public void onClick(double mx, double my) {
            if (this.canPress()) {
                if (this.forward) this.screen.setPage(this.screen.page + 1);
                else this.screen.setPage(this.screen.page - 1);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }
}
