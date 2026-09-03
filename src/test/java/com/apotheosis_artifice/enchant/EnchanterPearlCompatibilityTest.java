package com.apotheosis_artifice.enchant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class EnchanterPearlCompatibilityTest {

    private static final Path MAIN_JAVA = Path.of("src", "main", "java", "com", "apotheosis_artifice");
    private static final Path MIXIN_CONFIG = Path.of("src", "main", "resources", "apotheosis_artifice.mixins.json");

    @Test
    void enchanterPearlCompatibilityOnlyEnablesTreasure() throws IOException {
        String compat = read("compat", "EnigmaticLegacyCompat.java");

        assertTrue(compat.contains("TableStats enableTreasure(TableStats stats, Player player)"));
        assertTrue(compat.contains("stats.eterna(), stats.quanta(), stats.arcana()"));
        assertTrue(compat.contains("stats.rectification(), stats.clues(), stats.blacklist(), true"));
        assertTrue(compat.contains("stats.treasure()"));
        assertTrue(compat.contains("isEnchanterPearlActive(player)"));
        assertTrue(compat.contains("\"enigmaticlegacy\""));
        assertTrue(compat.contains("\"enchanter_pearl\""));
        assertTrue(compat.contains("isPresent"));
        assertTrue(compat.contains("private static boolean available"));
        assertTrue(compat.contains("if (!available) return false"));
        assertFalse(compat.contains("mergePearlEnchantments"));
        assertFalse(compat.contains("mergeEnchantments"));
        assertFalse(compat.contains("maybeApplyEternalBinding"));
    }

    @Test
    void mechanicalAutomaticEnchantingNeverUsesPlayerPearlCompatibility() throws IOException {
        String tile = read("enchant", "MechanicalRavenEnchantTile.java");

        assertFalse(tile.contains("EnigmaticLegacyCompat"));
        assertFalse(tile.contains("enchanter_pearl"));
        assertFalse(tile.substring(tile.indexOf("doEnchant")).contains("enableTreasure"));
    }

    @Test
    void playerOperatedMenusApplyTreasureStatsWithoutReplacingEnchanting() throws IOException {
        String mixin = read("mixin", "ApothEnchantmentMenuMixin.java");
        String ravenMenu = read("enchant", "RavenEnchantMenu.java");
        String mechanicalMenu = read("enchant", "MechanicalRavenEnchantMenu.java");
        String config = Files.readString(MIXIN_CONFIG);

        assertTrue(config.contains("\"ApothEnchantmentMenuMixin\""));
        assertTrue(mixin.contains("EnigmaticLegacyCompat.enableTreasure(this.stats, this.artifice$menuPlayer)"));
        assertTrue(mixin.contains("new StatsMessage(this.stats)"));
        assertTrue(mixin.contains("level().isClientSide"));
        assertTrue(ravenMenu.contains("EnigmaticLegacyCompat.enableTreasure("));
        assertFalse(ravenMenu.contains("mergePearlEnchantments"));
        assertFalse(ravenMenu.contains("public int getGoldCount()"));
        assertFalse(mixin.contains("mergePearlEnchantments"));
        assertFalse(mixin.contains("EnchantmentHelper.enchantItem"));
    }

    @Test
    void mechanicalRavenShowsPearlEnchantmentsAsAvailableWithoutLapis() throws IOException {
        String mechanicalMenu = read("enchant", "MechanicalRavenEnchantMenu.java");

        int method = mechanicalMenu.indexOf("public int getGoldCount()");
        int pearlCheck = mechanicalMenu.indexOf(
            "EnigmaticLegacyCompat.isEnchanterPearlActive(this.player)", method);
        int fuelRead = mechanicalMenu.indexOf("this.tile.getFuelInv()", method);
        assertTrue(method >= 0);
        assertTrue(pearlCheck > method);
        assertTrue(fuelRead > pearlCheck);
        assertTrue(mechanicalMenu.substring(pearlCheck, fuelRead).contains("return 64;"));
    }

    @Test
    void socketingRecipeOverwriteUsesMappedDevelopmentName() throws IOException {
        String mixin = read("mixin", "SocketingRecipeMixin.java");

        assertTrue(mixin.contains("@Overwrite(remap = true)"));
        assertTrue(mixin.contains("@author apotheosis_artifice"));
        assertTrue(mixin.contains("@reason Safely reject invalid socketing inputs"));
        assertTrue(mixin.contains("public ItemStack assemble(Container inv, RegistryAccess regs)"));
        assertFalse(mixin.contains("public ItemStack m_5874_("));
    }

    @Test
    void easyMagicCoreCompatibilityIsOptionalAndSharedByAllTables() throws IOException {
        Path compatPath = MAIN_JAVA.resolve("compat").resolve("EasyMagicCompat.java");
        Path storagePath = MAIN_JAVA.resolve("compat").resolve("EasyMagicEnchantingStorage.java");
        Path tileMixinPath = MAIN_JAVA.resolve("mixin").resolve("ApothEnchantTileEasyMagicMixin.java");
        Path removalMixinPath = MAIN_JAVA.resolve("mixin").resolve("EnchantmentMenuEasyMagicMixin.java");

        assertTrue(Files.exists(compatPath));
        assertTrue(Files.exists(storagePath));
        assertTrue(Files.exists(tileMixinPath));
        assertTrue(Files.exists(removalMixinPath));

        String compat = Files.readString(compatPath);
        String storage = Files.readString(storagePath);
        String tileMixin = Files.readString(tileMixinPath);
        String removalMixin = Files.readString(removalMixinPath);
        String ordinaryMenuMixin = read("mixin", "ApothEnchantmentMenuMixin.java");
        String ravenMenu = read("enchant", "RavenEnchantMenu.java");
        String mechanicalMenu = read("enchant", "MechanicalRavenEnchantMenu.java");
        String mechanicalTile = read("enchant", "MechanicalRavenEnchantTile.java");
        String mixinConfig = Files.readString(MIXIN_CONFIG);

        assertTrue(compat.contains("ModList.get().isLoaded(\"easymagic\")"));
        assertTrue(compat.contains("rerollEnchantments"));
        assertTrue(compat.contains("rerollExperienceCost"));
        assertTrue(compat.contains("rerollCatalystCost"));
        assertTrue(compat.contains("dedicatedRerollButton"));
        assertTrue(compat.contains("\"reroll_catalysts\""));
        assertFalse(compat.contains("import fuzs.easymagic"));
        assertTrue(storage.contains("Container getEasyMagicInventory()"));
        assertTrue(tileMixin.contains("artifice_easy_magic_inventory"));
        assertTrue(ordinaryMenuMixin.contains("data == 4"));
        assertTrue(ordinaryMenuMixin.contains("EasyMagicCompat.tryReroll"));
        assertTrue(removalMixin.contains("removed"));
        assertTrue(removalMixin.contains("quickMoveStack"));
        assertTrue(ravenMenu.contains("id == 4"));
        assertTrue(mechanicalMenu.contains("dedicatedCatalystIdx"));
        assertFalse(mechanicalMenu.contains("idx >= 38"));
        assertTrue(mechanicalTile.contains("EasyMagicEnchantingStorage"));
        assertFalse(mechanicalTile.substring(mechanicalTile.indexOf("doEnchant")).contains("tryReroll"));
        assertTrue(mixinConfig.contains("\"ApothEnchantTileEasyMagicMixin\""));
        assertTrue(mixinConfig.contains("ApothEnchantScreenEasyMagicMixin"));
        assertTrue(mixinConfig.contains("EnchantmentMenuEasyMagicMixin"));
    }

    @Test
    void easyMagicInventoryKeepsItsFixedSizeWhenTileDataLoads() throws IOException {
        String tileMixin = read("mixin", "ApothEnchantTileEasyMagicMixin.java");

        assertFalse(tileMixin.contains("artifice$easyMagicInventory.clearContent()"));
        assertTrue(tileMixin.contains("artifice$easyMagicInventory.setItem(0, ItemStack.EMPTY)"));
        assertTrue(tileMixin.contains("artifice$easyMagicInventory.setItem(1, ItemStack.EMPTY)"));
    }

    @Test
    void easyMagicBindingDoesNotRefreshOverriddenMenuDuringSuperclassConstruction() throws IOException {
        String menuMixin = read("mixin", "ApothEnchantmentMenuMixin.java");
        String ravenMenu = read("enchant", "RavenEnchantMenu.java");

        assertTrue(menuMixin.contains("getClass() == ApothEnchantmentMenu.class"));
        assertTrue(menuMixin.contains("ApothEnchantmentMenuMixin.this.slotsChanged(this)"));
        assertTrue(ravenMenu.contains("if (this.ravenStats == null) return;"));
    }

    @Test
    void easyMagicFuelAndRerollSlotsUseTheSameInventoryAsEnchantingLogic() throws IOException {
        String menuMixin = read("mixin", "ApothEnchantmentMenuMixin.java");
        String tileMixin = read("mixin", "ApothEnchantTileEasyMagicMixin.java");
        String compat = read("compat", "EasyMagicCompat.java");

        assertFalse(menuMixin.contains("new Slot(oldFuel.container"));
        assertTrue(menuMixin.contains("new Slot(inventory, 1,"));
        assertTrue(menuMixin.contains("new Slot(inventory, 2, 41, 47)"));
        assertTrue(tileMixin.contains("new SimpleContainer(3)"));
        assertTrue(tileMixin.contains("ItemStack fuel = this.artifice$easyMagicInventory.getItem(1)"));
        assertTrue(tileMixin.contains("ItemStack catalyst = this.artifice$easyMagicInventory.getItem(2)"));
        assertTrue(compat.contains("return menu.enchantSlots.getItem(2).getCount()"));
    }

    @Test
    void easyMagicInventoryChangesRefreshTheMenuWithoutRetainingClosedMenus() throws IOException {
        String menuMixin = read("mixin", "ApothEnchantmentMenuMixin.java");
        String tileMixin = read("mixin", "ApothEnchantTileEasyMagicMixin.java");

        assertTrue(menuMixin.contains("new SimpleContainer(3) {"));
        assertTrue(menuMixin.contains("ApothEnchantmentMenuMixin.this.slotsChanged(this)"));
        assertTrue(tileMixin.contains("menu.enchantSlots == this"));
        assertTrue(tileMixin.contains("menu.slotsChanged(this)"));
        assertFalse(menuMixin.contains("ContainerListener"));
    }

    @Test
    void easyMagicPersistentInputInitializesApotheosisStatsAfterMenuConstruction() throws IOException {
        String menuMixin = read("mixin", "ApothEnchantmentMenuMixin.java");
        String ravenMenu = read("enchant", "RavenEnchantMenu.java");

        assertTrue(menuMixin.contains("getClass() == ApothEnchantmentMenu.class"));
        assertTrue(menuMixin.contains("this.slotsChanged(this.enchantSlots)"));
        assertTrue(ravenMenu.contains("private void refreshEasyMagicStats()"));
        assertTrue(ravenMenu.contains("if (EasyMagicCompat.isLoaded()) this.slotsChanged(this.enchantSlots)"));
        assertTrue(ravenMenu.split("refreshEasyMagicStats\\(\\);", -1).length - 1 >= 2);
    }

    @Test
    void persistentInputRefreshesPreviewAfterClientScreenIsReady() throws IOException {
        String screenMixin = Files.readString(MAIN_JAVA.resolve("mixin").resolve("client")
            .resolve("ApothEnchantScreenEasyMagicMixin.java"));
        String menuMixin = read("mixin", "ApothEnchantmentMenuMixin.java");
        String ravenScreen = read("enchant", "RavenEnchantScreen.java");
        String mechanicalScreen = read("enchant", "MechanicalRavenEnchantScreen.java");
        String statsPacket = read("enchant", "SetRavenStatsPacket.java");

        assertTrue(screenMixin.contains("private boolean artifice$previewSyncPending = true;"));
        assertTrue(screenMixin.contains("if (!this.artifice$previewSyncPending) return;"));
        assertTrue(screenMixin.contains("handleInventoryButtonClick(this.menu.containerId, 5)"));
        assertTrue(menuMixin.contains("if (data == 5)"));
        assertTrue(menuMixin.contains("menu.slotsChanged(menu.enchantSlots);"));
        assertFalse(ravenScreen.contains("previewSyncPending"));
        assertFalse(mechanicalScreen.contains("initSyncDone"));
        assertFalse(statsPacket.contains("boolean refreshPreview"));
    }

    @Test
    void easyMagicRerollButtonUsesExternalApotheosisLayoutAndNativeTranslationKey() throws IOException {
        String screenMixin = Files.readString(MAIN_JAVA.resolve("mixin").resolve("client")
            .resolve("ApothEnchantScreenEasyMagicMixin.java"));
        String zhCn = Files.readString(Path.of("src", "main", "resources", "assets",
            "apotheosis_artifice", "lang", "zh_cn.json"));
        String enUs = Files.readString(Path.of("src", "main", "resources", "assets",
            "apotheosis_artifice", "lang", "en_us.json"));

        assertTrue(screenMixin.contains("return this.leftPos - 41;"));
        assertTrue(screenMixin.contains("@Inject(method = \"renderBg\", at = @At(\"HEAD\"))"));
        assertTrue(screenMixin.contains("private void artifice$renderDedicatedCatalystSlot("));
        assertFalse(screenMixin.contains("this.leftPos + (EasyMagicCompat.dedicatedRerollButton()"));
        assertTrue(screenMixin.contains("Component.translatable(\"container.enchant.reroll\")"));
        assertTrue(zhCn.contains("\"container.enchant.reroll\": \"刷新附魔选项\""));
        assertFalse(enUs.contains("\"container.enchant.reroll\""));
    }

    @Test
    void easyMagicRerollButtonRestoresNativeIconAndCostLayers() throws IOException {
        String screenMixin = Files.readString(MAIN_JAVA.resolve("mixin").resolve("client")
            .resolve("ApothEnchantScreenEasyMagicMixin.java"));

        assertTrue(screenMixin.contains("artifice$renderRerollContents(graphics"));
        assertTrue(screenMixin.contains("graphics.blit(ARTIFICE_REROLL_TEXTURE, x + 12, y + 6, 64"));
        assertTrue(screenMixin.contains("artifice$renderCostOrb("));
        assertTrue(screenMixin.contains("Math.min(2, cost / 5) * 13"));
        assertTrue(screenMixin.contains("graphics.drawString(this.font, value"));
    }

    @Test
    void easyMagicRerollButtonUsesAttachedVanillaStyleFrame() throws IOException {
        String screenMixin = Files.readString(MAIN_JAVA.resolve("mixin").resolve("client")
            .resolve("ApothEnchantScreenEasyMagicMixin.java"));

        int frame = screenMixin.indexOf("this.artifice$renderAttachedFrame(graphics, x, y);");
        int button = screenMixin.indexOf("graphics.blit(ARTIFICE_REROLL_TEXTURE, x, y");
        assertTrue(frame >= 0);
        assertTrue(button >= 0);
        assertTrue(frame < button);
        assertTrue(screenMixin.contains("private void artifice$renderAttachedFrame("));
        assertTrue(screenMixin.contains("0xFFC6C6C6"));
        assertTrue(screenMixin.contains("0xFFFFFFFF"));
        assertTrue(screenMixin.contains("0xFF555555"));
        assertTrue(screenMixin.contains("0xFF373737"));
        assertTrue(screenMixin.contains("graphics.fill(x - 5, y - 5"));
        assertTrue(screenMixin.contains("graphics.fill(x - 1, y - 1"));
        assertTrue(screenMixin.contains("0xFF8B8B8B"));
    }

    @Test
    void enchanterPearlWaivesOnlyOrdinaryRerollCatalystCost() throws IOException {
        String compat = read("compat", "EasyMagicCompat.java");
        String screenMixin = Files.readString(MAIN_JAVA.resolve("mixin").resolve("client")
            .resolve("ApothEnchantScreenEasyMagicMixin.java"));

        assertTrue(compat.contains("public static int rerollCatalystCost(Player player)"));
        assertTrue(compat.contains("!dedicatedRerollButton() && EnigmaticLegacyCompat.isEnchanterPearlActive(player)"));
        assertTrue(compat.contains("int catalystCost = rerollCatalystCost(player);"));
        assertTrue(compat.contains("int experienceCost = rerollExperienceCost();"));
        assertTrue(screenMixin.split("EasyMagicCompat.rerollCatalystCost\\(this.minecraft.player\\)", -1).length - 1 >= 2);
    }

    @Test
    void mechanicalRavenPersistsRerollSeedBeforeOfferRecalculation() throws IOException {
        String mechanicalMenu = read("enchant", "MechanicalRavenEnchantMenu.java");
        String compat = read("compat", "EasyMagicCompat.java");

        assertTrue(mechanicalMenu.contains("public void persistEnchantmentSeed(int seed)"));
        assertTrue(mechanicalMenu.contains("this.tile.setEnchantmentSeed(seed);"));
        assertTrue(mechanicalMenu.contains("this.tile.setChanged();"));
        int persistSeed = compat.indexOf("mechanical.persistEnchantmentSeed(newSeed);");
        int recalculateOffers = compat.indexOf("menu.slotsChanged(menu.enchantSlots);");
        assertTrue(persistSeed >= 0);
        assertTrue(recalculateOffers >= 0);
        assertTrue(persistSeed < recalculateOffers);
        assertFalse(mechanicalMenu.contains("boolean rerolled = super.clickMenuButton(player, id);"));
    }

    @Test
    void mechanicalRavenPreservesManualEnchantSeedDuringNestedBroadcast() throws IOException {
        String mechanicalMenu = read("enchant", "MechanicalRavenEnchantMenu.java");

        assertTrue(mechanicalMenu.contains("private boolean manualEnchantInProgress;"));
        assertTrue(mechanicalMenu.contains("id >= 0 && id < 3"));
        assertTrue(mechanicalMenu.contains("this.manualEnchantInProgress = true;"));
        assertTrue(mechanicalMenu.contains("this.persistEnchantmentSeed(this.enchantmentSeed.get());"));
        assertTrue(mechanicalMenu.contains("this.manualEnchantInProgress = false;"));
        assertTrue(mechanicalMenu.contains("if (!this.manualEnchantInProgress)"));
    }

    @Test
    void easyMagicLenientBookshelvesRemainOptionalForApotheosisTables() throws IOException {
        String compat = read("compat", "EasyMagicCompat.java");
        String menuMixin = read("mixin", "ApothEnchantmentMenuMixin.java");

        assertTrue(compat.contains("public static boolean lenientBookshelves()"));
        assertTrue(compat.contains("getBoolean(\"lenientBookshelves\", true)"));
        assertTrue(compat.contains("return available && enabled;"));
        assertFalse(compat.contains("import fuzs.easymagic"));
        assertTrue(menuMixin.contains("method = \"canReadStatsFrom\""));
        assertTrue(menuMixin.contains("EasyMagicCompat.lenientBookshelves()"));
        assertTrue(menuMixin.contains("getCollisionShape(level, between) != Shapes.block()"));
        assertTrue(menuMixin.contains("cir.setReturnValue(true);"));
    }

    @Test
    void enchanterPearlWaivesManualEnchantingLapisWithoutEasyMagic() throws IOException {
        String menuMixin = read("mixin", "ApothEnchantmentMenuMixin.java");

        assertTrue(menuMixin.contains("@ModifyVariable(method = \"clickMenuButton\""));
        assertTrue(menuMixin.contains("ordinal = 1"));
        assertTrue(menuMixin.contains("artifice$provideVirtualPearlFuel"));
        assertTrue(menuMixin.contains("id < 0 || id >= 3"));
        assertTrue(menuMixin.contains("EnigmaticLegacyCompat.isEnchanterPearlActive(player)"));
        assertTrue(menuMixin.contains("new ItemStack(Items.LAPIS_LAZULI, 64)"));
        assertTrue(menuMixin.contains("virtualFuel.setCount(64);"));
    }

    private static String read(String directory, String file) throws IOException {
        return Files.readString(MAIN_JAVA.resolve(directory).resolve(file));
    }
}
