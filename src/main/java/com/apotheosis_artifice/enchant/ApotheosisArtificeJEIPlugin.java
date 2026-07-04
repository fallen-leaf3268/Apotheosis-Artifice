package com.apotheosis_artifice.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ApotheosisNetwork;
import com.apotheosis_artifice.jei.AffixCodexCategory;
import com.apotheosis_artifice.jei.AffixCodexEntry;
import com.apotheosis_artifice.jei.AffixDetailCategory;
import com.apotheosis_artifice.jei.AffixDetailEntry;
import com.apotheosis_artifice.jei.AffixGemCategory;
import com.apotheosis_artifice.jei.AffixGemEntry;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableBlock;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.ench.compat.EnchantingCategory;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public class ApotheosisArtificeJEIPlugin implements IModPlugin {

    public static final ResourceLocation SUFFIX_UID = new ResourceLocation(ApotheosisArtificeMod.MODID, "affix_suffix");
    public static final RecipeType<AffixDetailEntry> SUFFIX_TYPE = new RecipeType<>(SUFFIX_UID, AffixDetailEntry.class);
    public static final ResourceLocation PREFIX_UID = new ResourceLocation(ApotheosisArtificeMod.MODID, "affix_prefix");
    public static final RecipeType<AffixDetailEntry> PREFIX_TYPE = new RecipeType<>(PREFIX_UID, AffixDetailEntry.class);

    private AffixDetailCategory suffixCategory;
    private AffixDetailCategory prefixCategory;

    @Override public ResourceLocation getPluginUid() { return new ResourceLocation(ApotheosisArtificeMod.MODID, "enchant"); }

    @Override public void registerCategories(IRecipeCategoryRegistration reg) {
        suffixCategory = new AffixDetailCategory(SUFFIX_TYPE, Component.translatable("jei.apotheosis_artifice.affix_suffix"),
            new ItemStack(dev.shadowsoffire.apotheosis.adventure.Adventure.Items.SIGIL_OF_ENHANCEMENT.get()));
        prefixCategory = new AffixDetailCategory(PREFIX_TYPE, Component.translatable("jei.apotheosis_artifice.affix_prefix"),
            new ItemStack(dev.shadowsoffire.apotheosis.adventure.Adventure.Items.SIGIL_OF_REBIRTH.get()));
        reg.addRecipeCategories(new AffixCodexCategory());
        reg.addRecipeCategories(suffixCategory);
        reg.addRecipeCategories(prefixCategory);
        reg.addRecipeCategories(new AffixGemCategory());
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(ApotheosisArtificeMod.RAVEN_ENCHANTING_TABLE_ITEM.get()), EnchantingCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(ApotheosisArtificeMod.MECHANICAL_RAVEN_TABLE_ITEM.get()), EnchantingCategory.TYPE);
        var ourTable = new ItemStack(ApotheosisArtificeMod.APOTHEOSIS_REFORGING_TABLE_ITEM.get());
        for (var type : List.of(AffixCodexCategory.TYPE, SUFFIX_TYPE, PREFIX_TYPE)) reg.addRecipeCatalyst(ourTable, type);
        reg.addRecipeCatalyst(ourTable, AffixGemCategory.TYPE);
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block instanceof ReforgingTableBlock) {
                ItemStack stack = new ItemStack(block);
                if (!stack.isEmpty() && !ItemStack.matches(stack, ourTable)) {
                    for (var type : List.of(AffixCodexCategory.TYPE, SUFFIX_TYPE, PREFIX_TYPE, AffixGemCategory.TYPE))
                        reg.addRecipeCatalyst(stack, type);
                }
            }
        }
    }

    @Override public void registerRecipes(IRecipeRegistration reg) {
        // 用 JEI 物品变体列表（含 NBT，如每个法术一支的卷轴）驱动类别物品扫描，
        // 修复"可重铸物品"里法术卷轴只显示一个空 NBT 卷轴的问题
        AffixCodexEntry.bindIngredientManager(reg.getIngredientManager());
        AffixCodexEntry codex = AffixCodexEntry.create();
        if (codex != null) reg.addRecipes(AffixCodexCategory.TYPE, List.of(codex));
        List<AffixDetailEntry> suffixEntries = new ArrayList<>();
        List<AffixDetailEntry> prefixEntries = new ArrayList<>();
        var availableCurioSlots = new java.util.HashSet<String>();
        try {
            for (var entry : top.theillusivec4.curios.api.CuriosApi.getSlots().entrySet()) {
                availableCurioSlots.add("curios:" + entry.getKey());
            }
        } catch (Exception ignored) {}
        for (LootCategory cat : LootCategory.VALUES) {
            if (cat.isNone()) continue;
            String cn = cat.getName();
            if (cn.startsWith("curios:") && !availableCurioSlots.contains(cn)) continue;
            if (cn.startsWith("curios:")) {
                List<ItemStack> catItems = AffixCodexEntry.getCategoryItems(cat);
                if (catItems.stream().noneMatch(s -> !s.is(net.minecraft.world.item.Items.BARRIER))) continue;
            }
            for (Affix affix : dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry.INSTANCE.getValues()) {
                List<AffixDetailEntry.RarityEntry> supported = new ArrayList<>();
                for (var holder : dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry.INSTANCE.getOrderedRarities()) {
                    if (!holder.isBound()) continue;
                    var rarity = holder.get();
                    try {
                        boolean match = affix.canApplyTo(ItemStack.EMPTY, cat, rarity);
                        if (match && cn.startsWith("curios:") && !"curio".equals(cn)) {
                            LootCategory genericCurio = LootCategory.byId("curio");
                            if (genericCurio != null && affix.canApplyTo(ItemStack.EMPTY, genericCurio, rarity)) {
                                match = false;
                            }
                        }
                        if (match) supported.add(new AffixDetailEntry.RarityEntry(rarity, AffixDetailEntry.buildDescription(affix, rarity)));
                    } catch (Exception ignored) {}
                }
                if (supported.isEmpty()) continue;
                AffixType type;
                try { type = affix.getType(); } catch (Exception e) { type = null; }
                (type == AffixType.STAT ? suffixEntries : prefixEntries).add(new AffixDetailEntry(cat, affix, supported));
            }
        }
        suffixEntries.sort(java.util.Comparator.comparing(a -> a.affix().getName(true).getString()));
        prefixEntries.sort(java.util.Comparator.comparing(a -> a.affix().getName(true).getString()));
        reg.addRecipes(SUFFIX_TYPE, suffixEntries);
        reg.addRecipes(PREFIX_TYPE, prefixEntries);
        List<AffixGemEntry> gemEntries = new ArrayList<>();
        for (LootCategory cat : LootCategory.VALUES) {
            if (cat.isNone()) continue;
            // curios:xxx 已合并到通用 curio，不单独生成条目
            if (cat.getName().startsWith("curios:") && !"curio".equals(cat.getName())) continue;
            gemEntries.addAll(AffixGemEntry.createAll(cat));
        }
        reg.addRecipes(AffixGemCategory.TYPE, gemEntries);
    }

    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(new MechanicalRavenTransferHandler(), EnchantingCategory.TYPE);
        reg.addRecipeTransferHandler(new RavenTransferHandler(), EnchantingCategory.TYPE);
    }

    private static class RavenTransferHandler implements IRecipeTransferHandler<RavenEnchantMenu, EnchantingRecipe> {
        @Override public Class<? extends RavenEnchantMenu> getContainerClass() { return RavenEnchantMenu.class; }
        @Override public Optional<MenuType<RavenEnchantMenu>> getMenuType() {
            if (RavenEnchantMenu.TYPE != null) return Optional.of(RavenEnchantMenu.TYPE);
            return Optional.ofNullable(ApotheosisArtificeMod.RAVEN_ENCHANTING_TABLE_MENU.get());
        }
        @Override public RecipeType<EnchantingRecipe> getRecipeType() { return EnchantingCategory.TYPE; }
        @Override @Nullable
        public IRecipeTransferError transferRecipe(RavenEnchantMenu container, EnchantingRecipe recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
            if (container instanceof MechanicalRavenEnchantMenu) return null;
            if (!doTransfer) return null;
            Inventory inv = player.getInventory();
            ItemStack inputItem = ItemStack.EMPTY;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && recipe.getInput().test(stack)) {
                    inputItem = stack.copy(); inputItem.setCount(1); stack.shrink(1);
                    container.getSlot(0).set(inputItem.copy());
                    container.slotsChanged(container.enchantSlots);
                    break;
                }
            }
            float e = recipe.getRequirements().eterna();
            float q = recipe.getRequirements().quanta();
            float a = recipe.getRequirements().arcana();
            container.transferJEI(e, q, a);
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().screen instanceof RavenEnchantScreen res)
                    res.transferSetSliders(e, q, a);
            });
            ApotheosisNetwork.CHANNEL.sendToServer(new SetRavenStatsPacket(e, q, a, inputItem));
            return null;
        }
    }

    private static class MechanicalRavenTransferHandler implements IRecipeTransferHandler<MechanicalRavenEnchantMenu, EnchantingRecipe> {
        @Override public Class<? extends MechanicalRavenEnchantMenu> getContainerClass() { return MechanicalRavenEnchantMenu.class; }
        @Override public Optional<MenuType<MechanicalRavenEnchantMenu>> getMenuType() {
            return Optional.ofNullable(ApotheosisArtificeMod.MECHANICAL_RAVEN_TABLE_MENU.get());
        }
        @Override public RecipeType<EnchantingRecipe> getRecipeType() { return EnchantingCategory.TYPE; }
        @Override @Nullable
        public IRecipeTransferError transferRecipe(MechanicalRavenEnchantMenu container, EnchantingRecipe recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
            if (!doTransfer) return null;
            Inventory inv = player.getInventory();
            ItemStack inputItem = ItemStack.EMPTY;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && recipe.getInput().test(stack)) {
                    inputItem = stack.copy();
                    inputItem.setCount(maxTransfer ? Math.min(stack.getCount(), stack.getMaxStackSize()) : 1);
                    break;
                }
            }
            if (inputItem.isEmpty()) return null;
            ItemStack sendItem = inputItem;
            float e = recipe.getRequirements().eterna();
            float q = recipe.getRequirements().quanta();
            float a = recipe.getRequirements().arcana();
            container.transferJEI(e, q, a);
            if (Minecraft.getInstance().screen instanceof MechanicalRavenEnchantScreen res) {
                res.transferSetSliders(e, q, a);
            }
            ApotheosisNetwork.CHANNEL.sendToServer(new SetRavenStatsPacket(e, q, a, sendItem));
            return null;
        }
    }
}
