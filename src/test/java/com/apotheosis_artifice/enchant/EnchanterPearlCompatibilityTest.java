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
        assertFalse(mechanicalMenu.contains("EnigmaticLegacyCompat"));
        assertFalse(mixin.contains("mergePearlEnchantments"));
        assertFalse(mixin.contains("EnchantmentHelper.enchantItem"));
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
    void easyMagicRerollButtonUsesExternalApotheosisLayoutAndNativeTranslationKey() throws IOException {
        String screenMixin = Files.readString(MAIN_JAVA.resolve("mixin").resolve("client")
            .resolve("ApothEnchantScreenEasyMagicMixin.java"));
        String zhCn = Files.readString(Path.of("src", "main", "resources", "assets",
            "apotheosis_artifice", "lang", "zh_cn.json"));
        String enUs = Files.readString(Path.of("src", "main", "resources", "assets",
            "apotheosis_artifice", "lang", "en_us.json"));

        assertTrue(screenMixin.contains("return this.leftPos - 40;"));
        assertFalse(screenMixin.contains("this.leftPos + (EasyMagicCompat.dedicatedRerollButton()"));
        assertTrue(screenMixin.contains("Component.translatable(\"container.enchant.reroll\")"));
        assertTrue(zhCn.contains("\"container.enchant.reroll\": \"刷新附魔选项\""));
        assertFalse(enUs.contains("\"container.enchant.reroll\""));
    }

    private static String read(String directory, String file) throws IOException {
        return Files.readString(MAIN_JAVA.resolve(directory).resolve(file));
    }
}
