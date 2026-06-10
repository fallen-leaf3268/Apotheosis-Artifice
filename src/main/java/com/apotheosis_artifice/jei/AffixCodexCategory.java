package com.apotheosis_artifice.jei;

import java.util.List;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class AffixCodexCategory implements IRecipeCategory<AffixCodexEntry> {

    public static final ResourceLocation UID = new ResourceLocation(ApotheosisArtificeMod.MODID, "affix_codex");
    public static final RecipeType<AffixCodexEntry> TYPE = new RecipeType<>(UID, AffixCodexEntry.class);

    private static final int PANEL_W = 170;
    private static final int PANEL_H = 150;
    private static final int HEADER_Y = 4;
    private static final int HEADER_H = 16;
    private static final int CAT_Y = 24;
    private static final int AFFIX_Y = 34;
    private static final int AFFIX_H = 12;
    private static final int MAX_VISIBLE = 8;

    @Override
    public RecipeType<AffixCodexEntry> getRecipeType() { return TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.apotheosis_artifice.affix_codex");
    }

    @Override
    public IDrawable getBackground() {
        return new IDrawable() {
            @Override public int getWidth() { return PANEL_W; }
            @Override public int getHeight() { return PANEL_H; }
            @Override public void draw(GuiGraphics gfx, int xOffset, int yOffset) {}
        };
    }

    @Override
    public IDrawable getIcon() { return null; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AffixCodexEntry entry, IFocusGroup focuses) {}

    @Override
    public void draw(AffixCodexEntry entry, IRecipeSlotsView slots, GuiGraphics gfx, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int col = 0xFF000000 | entry.rarity().getColor().getValue();

        // ── 一级：品质栏 ──
        String rarityName = entry.rarity().toComponent().getString();
        int rw = font.width(rarityName);
        gfx.drawString(font, rarityName, (PANEL_W - rw) / 2, HEADER_Y + (HEADER_H - font.lineHeight) / 2, col, false);
        gfx.fill(2, HEADER_Y + HEADER_H - 2, PANEL_W - 2, HEADER_Y + HEADER_H, col);

        // ── 二级：分类名 ──
        String catName = entry.category().getName();
        gfx.drawString(font, catName, 5, CAT_Y, 0xFFAAAAAA, false);

        // ── 词缀列表 ──
        List<Affix> affixes = entry.affixes();
        int maxVisible = Math.min(MAX_VISIBLE, affixes.size());
        ItemStack dummy = ItemStack.EMPTY;
        LootRarity rar = entry.rarity();

        for (int i = 0; i < maxVisible; i++) {
            Affix affix = affixes.get(i);
            int ay = AFFIX_Y + i * AFFIX_H;
            int leftCol = 0xFF000000 | rar.getColor().getValue();

            // 左侧颜色条
            gfx.fill(2, ay, 5, ay + AFFIX_H - 1, leftCol);

            // 词缀名称
            String name;
            try { name = affix.getName(true).getString(); }
            catch (Exception e) { name = "???"; }
            gfx.drawString(font, name, 8, ay + 1, 0xFFFFFF, true);

            // 词缀描述（属性值范围）
            try {
                Component desc = affix.getDescription(dummy, rar, 0.5f);
                if (desc == null || desc.getString().isBlank())
                    desc = affix.getAugmentingText(dummy, rar, 0.5f);
                if (desc != null && !desc.getString().isBlank()) {
                    // 截断过长的描述
                    String descStr = desc.getString().trim();
                    if (font.width(descStr) > PANEL_W - 80) {
                        descStr = font.plainSubstrByWidth(descStr, PANEL_W - 83) + "…";
                    }
                    gfx.drawString(font, descStr, PANEL_W - font.width(descStr) - 5, ay + 1, 0xAAAAAA, false);
                }
            } catch (Exception ignored) {}

            // 悬停高亮
            if (mouseX >= 2 && mouseX <= PANEL_W - 2
                && mouseY >= ay && mouseY <= ay + AFFIX_H - 1) {
                gfx.fill(5, ay, PANEL_W - 2, ay + AFFIX_H - 1, 0x20FFFFFF);
            }
        }

        // ── 超出提示 ──
        if (affixes.size() > MAX_VISIBLE) {
            String more = Component.translatable("jei.apotheosis_artifice.more", affixes.size() - MAX_VISIBLE).getString();
            gfx.drawString(font, more, 5, AFFIX_Y + MAX_VISIBLE * AFFIX_H, 0x888888, false);
        }
    }
}
