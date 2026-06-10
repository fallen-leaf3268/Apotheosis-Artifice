package com.apotheosis_artifice.enchant;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ApotheosisNetwork;

import dev.shadowsoffire.apotheosis.ench.compat.EnchantingCategory;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class ApotheosisArtificeJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(ApotheosisArtificeMod.MODID, "enchant");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(ApotheosisArtificeMod.RAVEN_ENCHANTING_TABLE_ITEM.get()), EnchantingCategory.TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(new RavenTransferHandler(), EnchantingCategory.TYPE);
    }

    private static class RavenTransferHandler implements IRecipeTransferHandler<RavenEnchantMenu, EnchantingRecipe> {

        @Override
        public Class<? extends RavenEnchantMenu> getContainerClass() {
            return RavenEnchantMenu.class;
        }

        @Override
        public Optional<MenuType<RavenEnchantMenu>> getMenuType() {
            if (RavenEnchantMenu.TYPE != null) return Optional.of(RavenEnchantMenu.TYPE);
            return Optional.ofNullable(ApotheosisArtificeMod.RAVEN_ENCHANTING_TABLE_MENU.get());
        }

        @Override
        public RecipeType<EnchantingRecipe> getRecipeType() {
            return EnchantingCategory.TYPE;
        }

        @Override
        @Nullable
        public IRecipeTransferError transferRecipe(RavenEnchantMenu container, EnchantingRecipe recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {

            if (!doTransfer) return null;

            ApotheosisArtificeMod.LOGGER.info("[TRANSFER] Recipe req: e={} q={} a={}, input={}",
                recipe.getRequirements().eterna(), recipe.getRequirements().quanta(),
                recipe.getRequirements().arcana(), recipe.getInput().toJson());

            ItemStack inputItem = ItemStack.EMPTY;
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && recipe.getInput().test(stack)) {
                    inputItem = stack.copy();
                    inputItem.setCount(1);
                    stack.shrink(1);
                    container.getSlot(0).set(inputItem.copy());
                    container.slotsChanged(container.enchantSlots);
                    break;
                }
            }

            float e = recipe.getRequirements().eterna();
            float q = recipe.getRequirements().quanta();
            float a = recipe.getRequirements().arcana();

            ApotheosisArtificeMod.LOGGER.info("[TRANSFER] Calling transferJEI(e={}, q={}, a={})", e, q, a);
            container.transferJEI(e, q, a);

            Minecraft.getInstance().execute(() -> {
                var sc = Minecraft.getInstance().screen;
                if (sc instanceof RavenEnchantScreen res) {
                    res.transferSetSliders(e, q, a);
                }
            });

            ApotheosisNetwork.CHANNEL.sendToServer(new SetRavenStatsPacket(e, q, a, inputItem));

            return null;
        }
    }
}
