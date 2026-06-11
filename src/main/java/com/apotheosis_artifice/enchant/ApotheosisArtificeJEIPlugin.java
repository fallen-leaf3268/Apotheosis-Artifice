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

    // Level 2.1 — 后缀词条（属性词条 / STAT）
    public static final ResourceLocation SUFFIX_UID = new ResourceLocation(ApotheosisArtificeMod.MODID, "affix_suffix");
    public static final RecipeType<AffixDetailEntry> SUFFIX_TYPE = new RecipeType<>(SUFFIX_UID, AffixDetailEntry.class);

    // Level 2.2 — 前缀词条（特殊词条 / ABILITY + POTION）
    public static final ResourceLocation PREFIX_UID = new ResourceLocation(ApotheosisArtificeMod.MODID, "affix_prefix");
    public static final RecipeType<AffixDetailEntry> PREFIX_TYPE = new RecipeType<>(PREFIX_UID, AffixDetailEntry.class);

    private AffixDetailCategory suffixCategory;
    private AffixDetailCategory prefixCategory;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(ApotheosisArtificeMod.MODID, "enchant");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        suffixCategory = new AffixDetailCategory(SUFFIX_TYPE,
            Component.translatable("jei.apotheosis_artifice.affix_suffix"));
        prefixCategory = new AffixDetailCategory(PREFIX_TYPE,
            Component.translatable("jei.apotheosis_artifice.affix_prefix"));

        reg.addRecipeCategories(new AffixCodexCategory());
        reg.addRecipeCategories(suffixCategory);
        reg.addRecipeCategories(prefixCategory);
        reg.addRecipeCategories(new AffixGemCategory());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(ApotheosisArtificeMod.RAVEN_ENCHANTING_TABLE_ITEM.get()), EnchantingCategory.TYPE);

        // 优先注册本 Mod 的神化重铸台作为 JEI 图标
        var ourTable = new ItemStack(ApotheosisArtificeMod.CURIOS_REFORGING_TABLE_ITEM.get());
        for (var type : List.of(AffixCodexCategory.TYPE, SUFFIX_TYPE, PREFIX_TYPE)) {
            reg.addRecipeCatalyst(ourTable, type);
        }
        // 可镶嵌宝石
        reg.addRecipeCatalyst(ourTable, AffixGemCategory.TYPE);

        // 其他重铸台
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block instanceof ReforgingTableBlock) {
                ItemStack stack = new ItemStack(block);
                if (!stack.isEmpty() && !ItemStack.matches(stack, ourTable)) {
                    reg.addRecipeCatalyst(stack, AffixCodexCategory.TYPE);
                    reg.addRecipeCatalyst(stack, SUFFIX_TYPE);
                    reg.addRecipeCatalyst(stack, PREFIX_TYPE);
                    reg.addRecipeCatalyst(stack, AffixGemCategory.TYPE);
                }
            }
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        // Level 1: 分类选择
        AffixCodexEntry codex = AffixCodexEntry.create();
        if (codex != null) {
            reg.addRecipes(AffixCodexCategory.TYPE, List.of(codex));
        }

        // Level 2.1 + 2.2: 按词缀类型拆分
        List<AffixDetailEntry> suffixEntries = new ArrayList<>();
        List<AffixDetailEntry> prefixEntries = new ArrayList<>();

        for (LootCategory cat : LootCategory.VALUES) {
            if (cat.isNone() || "curio".equals(cat.getName())) continue;
            for (Affix affix : dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry.INSTANCE.getValues()) {
                List<AffixDetailEntry.RarityEntry> supported = new ArrayList<>();
                for (var holder : dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry.INSTANCE.getOrderedRarities()) {
                    if (!holder.isBound()) continue;
                    var rarity = holder.get();
                    try {
                        if (affix.canApplyTo(ItemStack.EMPTY, cat, rarity)) {
                            supported.add(new AffixDetailEntry.RarityEntry(rarity, AffixDetailEntry.buildDescription(affix, rarity)));
                        }
                    } catch (Exception ignored) {}
                }
                if (supported.isEmpty()) continue;

                AffixType type;
                try { type = affix.getType(); } catch (Exception e) { type = null; }
                List<AffixDetailEntry> target = (type == AffixType.STAT) ? suffixEntries : prefixEntries;
                target.add(new AffixDetailEntry(cat, affix, supported));
            }
        }

        // 排序
        suffixEntries.sort(java.util.Comparator.comparing(a -> a.affix().getName(true).getString()));
        prefixEntries.sort(java.util.Comparator.comparing(a -> a.affix().getName(true).getString()));

        reg.addRecipes(SUFFIX_TYPE, suffixEntries);
        reg.addRecipes(PREFIX_TYPE, prefixEntries);

        // Level 2.3: 可镶嵌宝石
        List<AffixGemEntry> gemEntries = new ArrayList<>();
        for (LootCategory cat : LootCategory.VALUES) {
            if (cat.isNone() || "curio".equals(cat.getName())) continue;
            gemEntries.addAll(AffixGemEntry.createAll(cat));
        }
        reg.addRecipes(AffixGemCategory.TYPE, gemEntries);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(new RavenTransferHandler(), EnchantingCategory.TYPE);
    }

    private static class RavenTransferHandler implements IRecipeTransferHandler<RavenEnchantMenu, EnchantingRecipe> {
        @Override
        public Class<? extends RavenEnchantMenu> getContainerClass() { return RavenEnchantMenu.class; }

        @Override
        public Optional<MenuType<RavenEnchantMenu>> getMenuType() {
            if (RavenEnchantMenu.TYPE != null) return Optional.of(RavenEnchantMenu.TYPE);
            return Optional.ofNullable(ApotheosisArtificeMod.RAVEN_ENCHANTING_TABLE_MENU.get());
        }

        @Override
        public RecipeType<EnchantingRecipe> getRecipeType() { return EnchantingCategory.TYPE; }

        @Override @Nullable
        public IRecipeTransferError transferRecipe(RavenEnchantMenu container, EnchantingRecipe recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
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
}
