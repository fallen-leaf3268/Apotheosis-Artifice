package com.apotheosis_artifice.jei;

import java.util.List;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Level 1: 显示所有可重铸的分类（不分品质分组）
 */
public class AffixCodexCategory implements IRecipeCategory<AffixCodexEntry> {

    public static final ResourceLocation UID = new ResourceLocation(ApotheosisArtificeMod.MODID, "affix_codex");
    public static final RecipeType<AffixCodexEntry> TYPE = new RecipeType<>(UID, AffixCodexEntry.class);

    private static final int PANEL_W = 160;
    private static final int PANEL_H = 140;
    private static final int HEADER_Y = 4;
    private static final int HEADER_H = 14;
    private static final int SLOT_START_Y = 24;
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 7;
    private static final int TEXT_Y_OFF = 12;

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
    public void setRecipe(IRecipeLayoutBuilder builder, AffixCodexEntry entry, IFocusGroup focuses) {
        List<LootCategory> cats = entry.categories();
        for (int i = 0; i < cats.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int sx = 8 + col * SLOT_SIZE;
            int sy = SLOT_START_Y + row * SLOT_SIZE;
            LootCategory cat = cats.get(i);
            List<ItemStack> stacks = AffixCodexEntry.getCategoryItems(cat);
            builder.addSlot(RecipeIngredientRole.OUTPUT, sx, sy)
                .addItemStacks(stacks)
                .addTooltipCallback((slotView, tooltip) -> {
                    String key = cat.getName().startsWith("curio:")
                        ? "curios.identifier." + cat.getName().substring(6)
                        : "text.apotheosis.category." + cat.getName();
                    tooltip.add(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.GRAY));
                    tooltip.add(Component.translatable("jei.apotheosis_artifice.click_for_detail"));
                });
        }
    }

    @Override
    public void draw(AffixCodexEntry entry, IRecipeSlotsView slots, GuiGraphics gfx, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // 标题
        String title = Component.translatable("jei.apotheosis_artifice.affix_codex").getString();
        int tw = font.width(title);
        gfx.drawString(font, title, (PANEL_W - tw) / 2, HEADER_Y + (HEADER_H - font.lineHeight) / 2, 0xFF97714F, false);

    }
}
