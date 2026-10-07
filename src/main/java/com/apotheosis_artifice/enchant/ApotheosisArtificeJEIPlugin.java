package com.apotheosis_artifice.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ApotheosisConfig;
import com.apotheosis_artifice.ApotheosisNetwork;
import com.apotheosis_artifice.adventure.SigilUpgradeRecipe;
import com.apotheosis_artifice.jei.AffixCodexCategory;
import com.apotheosis_artifice.jei.AffixCodexEntry;
import com.apotheosis_artifice.jei.AffixDetailCategory;
import com.apotheosis_artifice.jei.AffixDetailEntry;
import com.apotheosis_artifice.jei.AffixGemCategory;
import com.apotheosis_artifice.jei.AffixGemEntry;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableBlock;
import dev.shadowsoffire.apotheosis.adventure.compat.ApothSmithingCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.ench.compat.EnchantingCategory;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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
    private static volatile ApotheosisArtificeJEIPlugin activePlugin;
    private IJeiRuntime runtime;
    private List<AffixDetailEntry> curioSuffixEntries = List.of();
    private List<AffixDetailEntry> curioPrefixEntries = List.of();
    private List<AffixGemEntry> curioGemEntries = List.of();
    private AffixCodexEntry fullCodex;
    private AffixCodexEntry nativeCodex;
    private boolean nativeCodexAdded;
    private Boolean displayedCuriosEnabled;

    public ApotheosisArtificeJEIPlugin() {
        if (dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) {
            ApothSmithingCategory.registerExtension(CleansingRecipe.class, new CleansingExtension());
            ApothSmithingCategory.registerExtension(SigilUpgradeRecipe.class, new SigilUpgradeExtension());
        }
    }

    @Override public ResourceLocation getPluginUid() { return new ResourceLocation(ApotheosisArtificeMod.MODID, "enchant"); }

    @Override public void registerCategories(IRecipeCategoryRegistration reg) {
        // adventure 模块被禁用时绝不能碰 Adventure$Items：Apotheosis 没在自己的构造期
        // 初始化过它，此处触发 <clinit> 会因 FMLJavaModLoadingContext.get()==null 抛
        // ExceptionInInitializerError 并把类永久毒化（后续宝石柜交互全部 NoClassDefFoundError）。
        ItemStack suffixIcon;
        ItemStack prefixIcon;
        if (dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) {
            suffixIcon = new ItemStack(dev.shadowsoffire.apotheosis.adventure.Adventure.Items.SIGIL_OF_ENHANCEMENT.get());
            prefixIcon = new ItemStack(dev.shadowsoffire.apotheosis.adventure.Adventure.Items.SIGIL_OF_REBIRTH.get());
        } else {
            suffixIcon = new ItemStack(ApotheosisArtificeMod.APOTHEOSIS_REFORGING_TABLE_ITEM.get());
            prefixIcon = new ItemStack(ApotheosisArtificeMod.APOTHEOSIS_REFORGING_TABLE_ITEM.get());
        }
        suffixCategory = new AffixDetailCategory(SUFFIX_TYPE, Component.translatable("jei.apotheosis_artifice.affix_suffix"), suffixIcon);
        prefixCategory = new AffixDetailCategory(PREFIX_TYPE, Component.translatable("jei.apotheosis_artifice.affix_prefix"), prefixIcon);
        reg.addRecipeCategories(new AffixCodexCategory());
        reg.addRecipeCategories(suffixCategory);
        reg.addRecipeCategories(prefixCategory);
        reg.addRecipeCategories(new AffixGemCategory());
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        if (dev.shadowsoffire.apotheosis.Apotheosis.enableEnch) {
            reg.addRecipeCatalyst(new ItemStack(ApotheosisArtificeMod.RAVEN_ENCHANTING_TABLE_ITEM.get()), EnchantingCategory.TYPE);
            reg.addRecipeCatalyst(new ItemStack(ApotheosisArtificeMod.MECHANICAL_RAVEN_TABLE_ITEM.get()), EnchantingCategory.TYPE);
        }
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return; // 词缀/重铸都属 adventure 模块
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
        // adventure 模块禁用时词缀/宝石注册表为空，类别已注册但不添加任何条目
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return;
        // 用 JEI 物品变体列表（含 NBT，如每个法术一支的卷轴）驱动类别物品扫描，
        // 修复"可重铸物品"里法术卷轴只显示一个空 NBT 卷轴的问题
        AffixCodexEntry.bindIngredientManager(reg.getIngredientManager());
        AffixCodexEntry codex = AffixCodexEntry.create();
        fullCodex = codex;
        nativeCodex = codex == null ? null : codex.withoutCurios();
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

        if (com.apotheosis_artifice.ApotheosisConfig.USE_BETTERCOMBAT_HEAVY_OVERRIDE.get()) {
            LootCategory heavyCat = dev.shadowsoffire.apotheosis.adventure.loot.LootCategory.byId("heavy_weapon");
            if (heavyCat != null && !heavyCat.isNone()) {
                for (Affix affix : dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry.INSTANCE.getValues()) {
                    String affixId = affix.getId().toString();
                    if (!affixId.contains("heavy_weapon")) continue;
                    List<AffixDetailEntry.RarityEntry> supported = new ArrayList<>();
                    for (var holder : dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry.INSTANCE.getOrderedRarities()) {
                        if (!holder.isBound()) continue;
                        var rarity = holder.get();
                        try {
                            if (affix.canApplyTo(ItemStack.EMPTY, heavyCat, rarity)) {
                                supported.add(new AffixDetailEntry.RarityEntry(rarity, AffixDetailEntry.buildDescription(affix, rarity)));
                            }
                        } catch (Exception ignored) {}
                    }
                    if (supported.isEmpty()) continue;
                    AffixType type;
                    try { type = affix.getType(); } catch (Exception e) { type = null; }
                    (type == AffixType.STAT ? suffixEntries : prefixEntries).add(new AffixDetailEntry(heavyCat, affix, supported));
                }
            }
        }
        suffixEntries.sort(java.util.Comparator.comparing(a -> a.affix().getName(true).getString()));
        prefixEntries.sort(java.util.Comparator.comparing(a -> a.affix().getName(true).getString()));
        curioSuffixEntries = suffixEntries.stream().filter(e -> isCurioCategory(e.category())).toList();
        curioPrefixEntries = prefixEntries.stream().filter(e -> isCurioCategory(e.category())).toList();
        reg.addRecipes(SUFFIX_TYPE, suffixEntries);
        reg.addRecipes(PREFIX_TYPE, prefixEntries);
        List<AffixGemEntry> gemEntries = new ArrayList<>();
        for (LootCategory cat : LootCategory.VALUES) {
            if (cat.isNone()) continue;
            if (cat.getName().startsWith("curios:") && !"curio".equals(cat.getName())) continue;
            gemEntries.addAll(AffixGemEntry.createAll(cat));
        }
        curioGemEntries = gemEntries.stream().filter(e -> isCurioCategory(e.category())).toList();
        reg.addRecipes(AffixGemCategory.TYPE, gemEntries);
    }

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
        this.nativeCodexAdded = false;
        this.displayedCuriosEnabled = null;
        activePlugin = this;
        refreshReforgingViews();
    }

    @Override public void onRuntimeUnavailable() {
        runtime = null;
        if (activePlugin == this) activePlugin = null;
    }

    public static void refreshReforgingViews() {
        Minecraft.getInstance().execute(() -> {
            ApotheosisArtificeJEIPlugin plugin = activePlugin;
            if (plugin != null) plugin.updateReforgingViews();
        });
    }

    private void updateReforgingViews() {
        if (runtime == null) return;
        boolean enabled = ApotheosisConfig.isCuriosReforgingEnabled();
        if (displayedCuriosEnabled != null && displayedCuriosEnabled == enabled) return;
        var manager = runtime.getRecipeManager();
        if (enabled) {
            manager.unhideRecipes(SUFFIX_TYPE, curioSuffixEntries);
            manager.unhideRecipes(PREFIX_TYPE, curioPrefixEntries);
            manager.unhideRecipes(AffixGemCategory.TYPE, curioGemEntries);
        } else {
            manager.hideRecipes(SUFFIX_TYPE, curioSuffixEntries);
            manager.hideRecipes(PREFIX_TYPE, curioPrefixEntries);
            manager.hideRecipes(AffixGemCategory.TYPE, curioGemEntries);
        }
        if (fullCodex != null && !fullCodex.equals(nativeCodex)) {
            if (enabled) {
                if (nativeCodexAdded) manager.hideRecipes(AffixCodexCategory.TYPE, List.of(nativeCodex));
                manager.unhideRecipes(AffixCodexCategory.TYPE, List.of(fullCodex));
            } else {
                manager.hideRecipes(AffixCodexCategory.TYPE, List.of(fullCodex));
                if (nativeCodex != null) {
                    if (!nativeCodexAdded) {
                        manager.addRecipes(AffixCodexCategory.TYPE, List.of(nativeCodex));
                        nativeCodexAdded = true;
                    } else manager.unhideRecipes(AffixCodexCategory.TYPE, List.of(nativeCodex));
                }
            }
        }
        displayedCuriosEnabled = enabled;
    }

    private static boolean isCurioCategory(LootCategory category) {
        String name = category.getName();
        return name.equals("curio") || name.startsWith("curios:");
    }

    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableEnch) return;
        TRANSFER_HELPER = reg.getTransferHelper();
        reg.addRecipeTransferHandler(new MechanicalRavenTransferHandler(), EnchantingCategory.TYPE);
        reg.addRecipeTransferHandler(new RavenTransferHandler(), EnchantingCategory.TYPE);
    }

    private static IRecipeTransferHandlerHelper TRANSFER_HELPER;

    static ItemStack findMechanicalTransferInput(Inventory inventory, Ingredient ingredient, boolean maxTransfer) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                ItemStack requested = stack.copy();
                requested.setCount(maxTransfer ? stack.getMaxStackSize() : 1);
                return requested;
            }
        }
        return ItemStack.EMPTY;
    }

    private static class CleansingExtension implements ApothSmithingCategory.Extension<CleansingRecipe> {
        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, CleansingRecipe recipe, IFocusGroup focuses) {
            List<ItemStack> inputs = List.of(Items.GOLDEN_SWORD, Items.DIAMOND_PICKAXE, Items.IRON_CHESTPLATE, Items.BOW)
                .stream().map(ItemStack::new).map(stack -> {
                    SocketHelper.setSockets(stack, 1);
                    return stack;
                }).toList();
            List<ItemStack> outputs = inputs.stream().map(stack ->
                recipe.assemble(new SimpleContainer(ItemStack.EMPTY, stack, ItemStack.EMPTY), RegistryAccess.EMPTY)).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, 35, 1).addItemStacks(inputs);
            builder.addSlot(RecipeIngredientRole.INPUT, 53, 1)
                .addItemStack(new ItemStack(ApotheosisArtificeMod.SIGIL_OF_CLEANSING.get()));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 1).addItemStacks(outputs);
        }

        @Override
        public void draw(CleansingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
            Component text = Component.translatable("text.apotheosis.gems_returned");
            var font = Minecraft.getInstance().font;
            gfx.drawString(font, text, 70 - font.width(text) / 2, 23, 0, false);
        }
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
            Inventory inv = player.getInventory();
            ItemStack inputItem = ItemStack.EMPTY;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && recipe.getInput().test(stack)) {
                    inputItem = stack.copy(); inputItem.setCount(1);
                    break;
                }
            }
            if (inputItem.isEmpty()) {
                if (TRANSFER_HELPER != null) {
                    return TRANSFER_HELPER.createUserErrorWithTooltip(
                        Component.translatable("jei.apotheosis_artifice.transfer.no_matching_item"));
                }
                return null;
            }
            if (!doTransfer) return null;
            float e = recipe.getRequirements().eterna();
            float q = recipe.getRequirements().quanta();
            float a = recipe.getRequirements().arcana();
            container.transferJEI(e, q, a);
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
            ItemStack inputItem = findMechanicalTransferInput(player.getInventory(), recipe.getInput(), maxTransfer);
            if (inputItem.isEmpty()) {
                return TRANSFER_HELPER == null ? null : TRANSFER_HELPER.createUserErrorWithTooltip(
                    Component.translatable("jei.apotheosis_artifice.transfer.no_matching_item"));
            }
            if (!doTransfer) return null;
            ItemStack sendItem = inputItem;
            float e = recipe.getRequirements().eterna();
            float q = recipe.getRequirements().quanta();
            float a = recipe.getRequirements().arcana();
            container.transferJEI(e, q, a);
            ApotheosisNetwork.CHANNEL.sendToServer(new SetRavenStatsPacket(e, q, a, sendItem));
            return null;
        }
    }
}
