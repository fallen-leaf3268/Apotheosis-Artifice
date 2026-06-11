package com.apotheosis_artifice.jei;

import java.util.List;

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
 * Level 2: 词缀/宝石详情。每页一个条目，词缀名置前，材料图标紧随其后。
 * 可创建多个实例（后缀/前缀/宝石），通过不同的 RecipeType 区分。
 */
public class AffixDetailCategory implements IRecipeCategory<AffixDetailEntry> {

    private final RecipeType<AffixDetailEntry> type;
    private final Component title;

    public AffixDetailCategory(RecipeType<AffixDetailEntry> type, Component title) {
        this.type = type;
        this.title = title;
    }

    @Override
    public RecipeType<AffixDetailEntry> getRecipeType() { return type; }

    @Override
    public Component getTitle() { return title; }

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 46;
    private static final int NAME_Y = 19;
    private static final int SLOT_Y = 28;
    private static final int SLOT_SIZE = 18;

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
    public void setRecipe(IRecipeLayoutBuilder builder, AffixDetailEntry entry, IFocusGroup focuses) {
        Font font = Minecraft.getInstance().font;

        // 分类输入槽（左上角），用于 JEI 跳转（多物品循环）
        builder.addSlot(RecipeIngredientRole.INPUT, 2, 0)
            .addItemStacks(AffixCodexEntry.getCategoryItems(entry.category()));

        String tmp;
        try { tmp = entry.affix().getName(true).getString(); }
        catch (Exception e) { tmp = "???"; }
        final String afxName = tmp;
        final String rangeColor = entry.affix().getType() == dev.shadowsoffire.apotheosis.adventure.affix.AffixType.STAT ? "§9" : "§6";

        // 材料图标行靠左
        List<AffixDetailEntry.RarityEntry> rarities = entry.rarities();
        int slotStartX = 2;

        for (int j = 0; j < rarities.size(); j++) {
            AffixDetailEntry.RarityEntry re = rarities.get(j);
            int sx = slotStartX + j * SLOT_SIZE;
            ItemStack matStack = new ItemStack(re.rarity().getMaterial());

            builder.addSlot(RecipeIngredientRole.INPUT, sx, SLOT_Y)
                .addItemStack(matStack)
                .addTooltipCallback((slotView, tooltip) -> {
                    tooltip.clear();
                    tooltip.add(matStack.getDisplayName().copy());
                    tooltip.add(Component.literal("§6" + afxName));
                    String range = re.rangeTooltip().getString();
                    if (!range.isBlank()) {
                        tooltip.add(Component.literal(rangeColor + range.replace("§7", "")));
                    }
                });
        }
    }

    @Override
    public void draw(AffixDetailEntry entry, IRecipeSlotsView slots, GuiGraphics gfx, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // 分类名（翻译）
        String catRaw = entry.category().getName();
        String catLocalized;
        if (catRaw.startsWith("curio:")) {
            catLocalized = Component.translatable("curios.identifier." + catRaw.substring(6)).getString();
        } else {
            catLocalized = Component.translatable("text.apotheosis.category." + catRaw).getString();
        }
        gfx.drawString(font, catLocalized, 22, 3, 0xFFFFFF, false);

        // 词缀名
        String name;
        try { name = entry.affix().getName(true).getString(); }
        catch (Exception e) { name = "???"; }
        gfx.drawString(font, name, 2, NAME_Y, 0xFFFFAA00, false);
    }
}
