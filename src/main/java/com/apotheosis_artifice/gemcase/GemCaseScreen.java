package com.apotheosis_artifice.gemcase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.glfw.GLFW;

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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
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
    private static final int SCROLLBAR_TOP = 32;
    private static final int SCROLLBAR_TRAVEL = 90;
    private static final int SCROLLBAR_HEIGHT = 12;

    protected int startIndex;
    protected int listPage = 0;
    protected int maxListPage = 0;
    protected int page = 0;
    protected int maxPage = 0;
    protected List<Gem> data = new ArrayList<>();
    protected List<GemCaseSelectButton> gemButtons = new ArrayList<>();
    protected List<UpgradeButton> upgradeButtons = new ArrayList<>();
    protected List<PageButton> pageButtons = new ArrayList<>();
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

        this.filter = this.addRenderableWidget(new EditBox(this.font, left + 24, top + 15, 100, 11, this.filter, Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
                super.renderWidget(new SearchGraphics(gfx), mouseX, mouseY, partialTick);
            }
        });
        this.filter.setBordered(false);
        this.filter.setTextColor(0xFF554536);
        this.filter.setResponder(t -> this.containerChanged());
        this.filter.setCanLoseFocus(true);

        this.gemButtons.clear();
        for (int i = 0; i < MAX_ROWS * SLOTS_PER_ROW; i++) {
            var btn = new GemCaseSelectButton(this, i, left + 21 + i % SLOTS_PER_ROW * 18, top + 31 + i / SLOTS_PER_ROW * 19);
            this.gemButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        this.maxPage = this.menu.getMaxPage();
        this.page = Math.min(this.page, this.maxPage);

        int pgY = top + 109;
        this.pageButtons.clear();
        this.pageButtons.add(this.addRenderableWidget(new PageButton(left + 20, pgY, this, false)));
        this.pageButtons.add(this.addRenderableWidget(new PageButton(left + 119, pgY, this, true)));

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
        this.maxPage = this.menu.getMaxPage();
        int validPage = net.minecraft.util.Mth.clamp(this.page, 0, this.maxPage);
        if (this.page != validPage) {
            this.page = validPage;
            this.applyPage();
            ApotheosisNetwork.CHANNEL.sendToServer(new GemCasePagePacket(this.page));
        }
        for (PageButton button : this.pageButtons) button.updateState();
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

    public long getCountAt(int index) {
        Gem gem = this.getGemAt(index);
        return gem == null ? 0 : this.menu.getGemCount(gem);
    }

    public int getFilteredGemCount() { return this.data.size(); }
    public int getStartIndex() { return this.startIndex; }

    private void setPage(int newPage) {
        newPage = net.minecraft.util.Mth.clamp(newPage, 0, this.maxPage);
        if (this.page == newPage) return;
        this.page = newPage;
        ApotheosisNetwork.CHANNEL.sendToServer(new GemCasePagePacket(this.page));
        this.applyPage();
        this.containerChanged();
    }

    private void applyPage() {
        this.menu.setPage(this.page);
    }

    @Override
    public void containerTick() {
        if (this.filter != null) this.filter.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != GLFW.GLFW_KEY_TAB
            && this.filter != null && this.filter.canConsumeInput()) {
            this.filter.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
            && my >= this.getGuiTop() + SCROLLBAR_TOP
            && my < this.getGuiTop() + SCROLLBAR_TOP + SCROLLBAR_TRAVEL + SCROLLBAR_HEIGHT) {
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

        if (this.menu.getSlot(GemCaseMenu.FILTER_SLOT).hasItem()) {
            gfx.blit(TEXTURE, left + 142, top + 18, 8, 148, 16, 16, 307, 256);
        }

        if (this.maxListPage > 0) {
            float pct = (float) this.listPage / this.maxListPage;
            int scrollbarPos = (int) (SCROLLBAR_TRAVEL * pct);
            gfx.blit(TEXTURE, left + 13, top + SCROLLBAR_TOP + scrollbarPos, 303, 0, 4, SCROLLBAR_HEIGHT, 307, 256);
        } else {
            gfx.blit(TEXTURE, left + 13, top + SCROLLBAR_TOP, 303, 12, 4, SCROLLBAR_HEIGHT, 307, 256);
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
        super.render(new CountGraphics(gfx), mouseX, mouseY, partialTick);
        this.renderGemCaseCounts(gfx);
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
        if (this.hoveredSlot == this.menu.getSlot(GemCaseMenu.FILTER_SLOT) && !this.hoveredSlot.hasItem()) {
            gfx.renderComponentTooltip(this.font,
                List.of(Component.translatable("container.apotheosis_artifice.gem_case.filter_hint")), x, y);
            return;
        }
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
            long count = this.menu.getGemCount(gem);
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
        RenderSystem.setShaderColor(1, 1, 1, 0.65F);
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
            int spriteY = !this.active ? 61 : this.isHovered() ? 45 : 29;
            gfx.blit(TEXTURE, x, y, 291, spriteY, 16, 16, 307, 256);
        }

        @Override
        public void onClick(double mx, double my) {
            boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            ApotheosisNetwork.CHANNEL.sendToServer(new GemCaseUpgradePacket(this.rarityOrdinal, this.screen.page, shift));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }

    private static class CountGraphics extends GuiGraphics {
        private final GuiGraphics source;
        private CountDecoration decoration;

        private CountGraphics(GuiGraphics source) {
            super(Minecraft.getInstance(), source.bufferSource());
            this.source = source;
            this.pose().last().pose().set(source.pose().last().pose());
            this.pose().last().normal().set(source.pose().last().normal());
        }

        @Override
        public void enableScissor(int left, int top, int right, int bottom) {
            this.source.enableScissor(left, top, right, bottom);
        }

        @Override
        public void disableScissor() {
            this.source.disableScissor();
        }

        @Override
        public void renderItemDecorations(Font font, ItemStack stack, int x, int y, String text) {
            CountDecoration previous = this.decoration;
            this.decoration = null;
            String count = Integer.toString(stack.getCount());
            if (stack.getCount() > 1 && (text == null || text.equals(count)) && font.width(count) > 14) {
                this.decoration = new CountDecoration(font, count, x, y);
            }
            try {
                super.renderItemDecorations(font, stack, x, y, text);
            } finally {
                this.decoration = previous;
            }
        }

        @Override
        public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
            CountDecoration count = this.decoration;
            if (count == null || font != count.font() || !count.text().equals(text)
                || x != count.x() + 17 - font.width(text) || y != count.y() + 9
                || color != 0xFFFFFF || !shadow) return super.drawString(font, text, x, y, color, shadow);
            float scale = 14F / font.width(text);
            this.pose().pushPose();
            try {
                this.pose().translate(x + font.width(text) * (1 - scale), y + (1 - scale) * 4, 0);
                this.pose().scale(scale, scale, 1);
                return super.drawString(font, text, 0, 0, color, shadow);
            } finally {
                this.pose().popPose();
            }
        }

        private record CountDecoration(Font font, String text, int x, int y) {}
    }

    private static class SearchGraphics extends GuiGraphics {
        private final GuiGraphics delegate;

        private SearchGraphics(GuiGraphics delegate) {
            super(Minecraft.getInstance(), delegate.bufferSource());
            this.delegate = delegate;
        }

        @Override
        public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
            return this.delegate.drawString(font, text, x, y, color, false);
        }

        @Override
        public int drawString(Font font, Component text, int x, int y, int color) {
            return this.delegate.drawString(font, text, x, y, color, false);
        }

        @Override
        public int drawString(Font font, String text, int x, int y, int color) {
            return this.delegate.drawString(font, text, x, y, color, false);
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int color) {
            this.delegate.fill(x1, y1, x2, y2, color);
        }

        @Override
        public void fill(RenderType type, int x1, int y1, int x2, int y2, int color) {
            this.delegate.fill(type, x1, y1, x2, y2, color);
        }
    }

    static class PageButton extends AbstractWidget {
        private final GemCaseScreen screen;
        private final boolean forward;

        public PageButton(int x, int y, GemCaseScreen screen, boolean forward) {
            super(x, y, 9, 16, Component.empty());
            this.screen = screen;
            this.forward = forward;
        }

        private boolean canPress() {
            return this.forward ? this.screen.page < this.screen.maxPage : this.screen.page > 0;
        }

        private void updateState() {
            this.visible = this.screen.maxPage > 0;
            this.active = this.visible && this.canPress();
        }

        @Override
        public void renderWidget(GuiGraphics gfx, int mx, int my, float pt) {
            int spriteY = (this.forward ? 128 : 80)
                + (!this.canPress() ? 32 : this.isHovered() ? 16 : 0);
            gfx.blit(TEXTURE, this.getX(), this.getY(), 298, spriteY, 9, 16, 307, 256);
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
