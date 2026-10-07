package com.apotheosis_artifice.enchant;

import java.util.LinkedHashMap;

import com.apotheosis_artifice.adventure.SigilAffixHelper;
import com.apotheosis_artifice.adventure.SigilUpgradeRecipe;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.compat.ApothSmithingCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class SigilUpgradeExtension implements ApothSmithingCategory.Extension<SigilUpgradeRecipe> {
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SigilUpgradeRecipe recipe, IFocusGroup focuses) {
        ItemStack input = sample();
        ItemStack output = ItemStack.EMPTY;
        if (!input.isEmpty()) {
            if (recipe.operation() == SigilUpgradeRecipe.Operation.SUPREMACY) {
                output = recipe.assemble(new SimpleContainer(ItemStack.EMPTY, input, new ItemStack(recipe.sigil())), RegistryAccess.EMPTY);
            } else {
                output = input.copy();
                SigilAffixHelper.applyMalicePreview(output, 0, 1);
            }
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 1).addItemStack(input);
        builder.addSlot(RecipeIngredientRole.INPUT, 53, 1).addItemStack(new ItemStack(recipe.sigil()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 1).addItemStack(output);
    }

    private static ItemStack sample() {
        for (var rarity : RarityRegistry.INSTANCE.getOrderedRarities()) {
            if (!rarity.isBound()) continue;
            ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
            var affixes = new LinkedHashMap<DynamicHolder<? extends Affix>, AffixInstance>();
            for (Affix affix : AffixRegistry.INSTANCE.getValues()) {
                if (affix.getType() == AffixType.DURABILITY || !affix.canApplyTo(stack, LootCategory.forItem(stack), rarity.get())) continue;
                var holder = AffixRegistry.INSTANCE.holder(affix.getId());
                var instance = new AffixInstance(holder, stack, rarity, .65F);
                if (!SigilAffixHelper.isLevelIndependent(instance)) affixes.put(holder, instance);
                if (affixes.size() == 2) break;
            }
            if (affixes.size() == 2) {
                AffixHelper.setRarity(stack, rarity.get());
                AffixHelper.setAffixes(stack, affixes);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void draw(SigilUpgradeRecipe recipe, IRecipeSlotsView slots, GuiGraphics gfx, double mouseX, double mouseY) {
        String operation = recipe.operation().name().toLowerCase(java.util.Locale.ROOT);
        var text = Component.translatable("jei.apotheosis_artifice." + operation);
        var font = Minecraft.getInstance().font;
        gfx.drawString(font, text, 70 - font.width(text) / 2, 23, 0, false);
    }
}
