package com.apotheosis_artifice.jei;

import java.util.List;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
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

public class AffixGemCategory implements IRecipeCategory<AffixGemEntry> {

    public static final ResourceLocation UID = new ResourceLocation(ApotheosisArtificeMod.MODID, "affix_gem");
    public static final RecipeType<AffixGemEntry> TYPE = new RecipeType<>(UID, AffixGemEntry.class);

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 46;
    private static final int NAME_Y = 19;
    private static final int SLOT_Y = 28;
    private static final int SLOT_SIZE = 18;

    @Override
    public RecipeType<AffixGemEntry> getRecipeType() { return TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.apotheosis_artifice.affix_gem");
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
    public void setRecipe(IRecipeLayoutBuilder builder, AffixGemEntry entry, IFocusGroup focuses) {
        // 分类输入槽
        builder.addSlot(RecipeIngredientRole.INPUT, 2, 0)
            .addItemStacks(AffixCodexEntry.getCategoryItems(entry.category()));

        // 宝石名（固定史诗品质颜色）
        Component gemNameComp = Component.literal(entry.gem().getId().getPath());
        try {
            var epicHolder = dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry.INSTANCE.holder(new ResourceLocation("apotheosis:epic"));
            if (epicHolder.isBound()) {
                var color = epicHolder.get().getColor();
                String rawName = Component.translatable("item.apotheosis.gem." + entry.gem().getId()).getString();
                gemNameComp = Component.literal(rawName).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(color));
            }
        } catch (Exception ignored) {}
        final Component finalGemName = gemNameComp;

        // 从左到右：低稀有度 → 高稀有度
        List<AffixGemEntry.RarityEntry> rarities = entry.rarities();
        for (int j = 0; j < rarities.size(); j++) {
            AffixGemEntry.RarityEntry re = rarities.get(j);
            int sx = 2 + j * SLOT_SIZE;

            // 创建该稀有度的宝石物品栈
            ItemStack gemStack = new ItemStack(Adventure.Items.GEM.get());
            GemItem.setGem(gemStack, entry.gem());
            AffixHelper.setRarity(gemStack, re.rarity());

            builder.addSlot(RecipeIngredientRole.INPUT, sx, SLOT_Y)
                .addItemStack(gemStack)
                .addTooltipCallback((slotView, tooltip) -> {
                    tooltip.clear();
                    // 第1行：宝石名（保留稀有度颜色，去括号）
                    String raw = gemStack.getDisplayName().getString().replace("[", "").replace("]", "").replace("(", "").replace(")", "");
                    int gemColor = re.rarity().getColor().getValue();
                    tooltip.add(Component.literal(raw).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(gemColor)));
                    // 唯一属性
                    if (entry.gem().isUnique()) {
                        tooltip.add(Component.translatable("text.apotheosis.unique").withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xC73912)));
                    }
                    // 该栏位 + 属性
                    Component bonus = re.bonusTooltip();
                    String catLocal;
                    if (entry.category().getName().startsWith("curio:")) {
                        catLocal = Component.translatable("curios.identifier." + entry.category().getName().substring(6)).getString();
                    } else {
                        catLocal = Component.translatable("text.apotheosis.category." + entry.category().getName()).getString();
                    }
                    if (bonus != null && !bonus.getString().isBlank()) {
                        tooltip.add(Component.literal("§e" + catLocal + ": ").append(bonus));
                    }
                });
        }
    }

    @Override
    public void draw(AffixGemEntry entry, IRecipeSlotsView slots, GuiGraphics gfx, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // 分类名（金色）
        String catRaw = entry.category().getName();
        String catLocalized;
        if (catRaw.startsWith("curio:")) {
            catLocalized = Component.translatable("curios.identifier." + catRaw.substring(6)).getString();
        } else {
            catLocalized = Component.translatable("text.apotheosis.category." + catRaw).getString();
        }
        gfx.drawString(font, catLocalized, 22, 3, 0xFFFFAA00, false);

        // 宝石名（去掉稀有度前缀，保留颜色）
        String name;
        try {
            var topRarity = entry.rarities().get(entry.rarities().size() - 1).rarity();
            name = Component.translatable("item.apotheosis.gem." + entry.gem().getId()).getString();
        } catch (Exception e) {
            name = entry.gem().getId().getPath();
        }
        gfx.drawString(font, name, 2, NAME_Y, 0xFFFF55FF, false);
    }
}
