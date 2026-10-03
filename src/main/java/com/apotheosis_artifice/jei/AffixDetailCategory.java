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
    private final net.minecraft.world.item.ItemStack iconStack;

    public AffixDetailCategory(RecipeType<AffixDetailEntry> type, Component title, net.minecraft.world.item.ItemStack iconStack) {
        this.type = type;
        this.title = title;
        this.iconStack = iconStack;
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
    private static final int SOURCE_X = PANEL_W - 14;
    private static final int SOURCE_Y = 1;
    private static final int[] SOURCE_GEAR = {
        0x0F0, 0x6F6, 0x7FE, 0x3FC, 0xF9F, 0xF0F,
        0xF0F, 0xF9F, 0x3FC, 0x7FE, 0x6F6, 0x0F0
    };

    public static String sourcePath(ResourceLocation id, String directory) {
        return "data\\" + id.getNamespace() + "\\" + directory + "\\" + id.getPath().replace('/', '\\') + ".json";
    }

    private static boolean isSourceHovered(double mouseX, double mouseY) {
        return mouseX >= SOURCE_X && mouseX < SOURCE_X + 12 && mouseY >= SOURCE_Y && mouseY < SOURCE_Y + 12;
    }

    public static List<Component> splitTooltipLines(Component text) {
        if (!text.getString().contains("\n")) return List.of(text.copy());
        var lines = new java.util.ArrayList<net.minecraft.network.chat.MutableComponent>();
        lines.add(Component.empty());
        text.visit((net.minecraft.network.chat.Style style, String segment) -> {
            String[] parts = segment.split("\n", -1);
            for (int index = 0; index < parts.length; index++) {
                if (index > 0) lines.add(Component.empty());
                if (!parts[index].isEmpty()) lines.get(lines.size() - 1).append(Component.literal(parts[index]).withStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return List.copyOf(lines);
    }

    public static List<Component> sourceTooltip(ResourceLocation id, String directory, double mouseX, double mouseY) {
        if (!isSourceHovered(mouseX, mouseY)) return List.of();
        return List.of(Component.translatable("jei.apotheosis_artifice.data_source", sourcePath(id, directory))
            .withStyle(net.minecraft.ChatFormatting.GOLD));
    }

    static void drawSourceHeader(GuiGraphics gfx, Font font, String name, double mouseX, double mouseY) {
        gfx.drawString(font, font.plainSubstrByWidth(name, SOURCE_X - 24), 22, 3, 0xFFFFAA00, false);
        int color = isSourceHovered(mouseX, mouseY) ? 0xFFE0A63A : 0xFF6C6C6C;
        for (int y = 0; y < SOURCE_GEAR.length; y++) {
            for (int x = 0; x < 12; x++) {
                if ((SOURCE_GEAR[y] & (1 << x)) != 0) {
                    gfx.fill(SOURCE_X + x, SOURCE_Y + y, SOURCE_X + x + 1, SOURCE_Y + y + 1, color);
                }
            }
        }
    }

    @Override
    public List<Component> getTooltipStrings(AffixDetailEntry entry, IRecipeSlotsView slots, double mouseX, double mouseY) {
        return sourceTooltip(entry.affix().getId(), "affixes", mouseX, mouseY);
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
    public IDrawable getIcon() {
        return new IDrawable() {
            @Override public int getWidth() { return 16; }
            @Override public int getHeight() { return 16; }
            @Override public void draw(GuiGraphics gfx, int x, int y) {
                gfx.renderItem(iconStack, x, y);
            }
        };
    }

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
        final String rangeColor = entry.affix().getType() == dev.shadowsoffire.apotheosis.adventure.affix.AffixType.STAT ? "§9" : "§e";

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
                    // 材料名去括号，颜色来自稀有度
                    int matColor = re.rarity().getColor().getValue();
                    String matName = matStack.getDisplayName().getString()
                        .replace("[", "").replace("]", "")
                        .replace("(", "").replace(")", "");
                    tooltip.add(Component.literal(matName).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(matColor)));
                    tooltip.add(Component.literal("§e" + entry.affix().getName(true).getString()));
                    Component range = re.rangeTooltip();
                    if (range != null && !range.getString().isBlank()) {
                        tooltip.addAll(splitTooltipLines(range));
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
        if (catRaw.startsWith("curios:")) {
            catLocalized = Component.translatable("curios.identifier." + catRaw.substring(7)).getString();
        } else {
            catLocalized = Component.translatable("text.apotheosis.category." + catRaw).getString();
        }
        drawSourceHeader(gfx, font, catLocalized, mouseX, mouseY);

        // 词缀名
        String name;
        try { name = entry.affix().getName(true).getString(); }
        catch (Exception e) { name = "???"; }
        gfx.drawString(font, name, 2, NAME_Y, 0xFFFF55, false);
    }
}
