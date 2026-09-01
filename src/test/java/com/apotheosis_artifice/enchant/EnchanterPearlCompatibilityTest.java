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
    void manualRavenEnchantingUsesOptionalEnchanterPearlCompatibility() throws IOException {
        String menu = read("enchant", "RavenEnchantMenu.java");
        String mechanicalMenu = read("enchant", "MechanicalRavenEnchantMenu.java");
        String compat = read("compat", "EnigmaticLegacyCompat.java");

        assertTrue(menu.contains("EnigmaticLegacyCompat.isEnchanterPearlActive(player)"));
        assertTrue(menu.contains("EnigmaticLegacyCompat.mergePearlEnchantments"));
        assertTrue(menu.contains("public int getGoldCount()"));
        assertTrue(menu.contains("EnchantmentUtils.chargeExperience"));
        assertTrue(menu.contains("ApothMiscUtil.getExpCostForSlot"));
        assertTrue(menu.contains("((EnchantedTrigger) CriteriaTriggers.ENCHANTED_ITEM).trigger"));
        assertTrue(mechanicalMenu.contains("EnigmaticLegacyCompat.isEnchanterPearlActive(this.player)"));
        assertTrue(compat.contains("\"enigmaticlegacy\""));
        assertTrue(compat.contains("\"enchanter_pearl\""));
        assertTrue(compat.contains("isPresent"));
        assertTrue(compat.contains("mergeEnchantments"));
        assertTrue(compat.contains("maybeApplyEternalBinding"));
        assertTrue(compat.contains("private static boolean available"));
        assertTrue(compat.contains("if (!available) return false"));
    }

    @Test
    void mechanicalAutomaticEnchantingNeverUsesPlayerPearlCompatibility() throws IOException {
        String tile = read("enchant", "MechanicalRavenEnchantTile.java");

        assertFalse(tile.contains("EnigmaticLegacyCompat"));
        assertFalse(tile.contains("enchanter_pearl"));
    }

    @Test
    void ordinaryApotheosisEnchantingTableHandlesEnchanterPearlServerClick() throws IOException {
        Path mixinPath = MAIN_JAVA.resolve("mixin").resolve("ApothEnchantmentMenuMixin.java");

        assertTrue(Files.exists(mixinPath));
        String mixin = Files.readString(mixinPath);
        String config = Files.readString(MIXIN_CONFIG);
        assertTrue(config.contains("\"ApothEnchantmentMenuMixin\""));
        assertTrue(mixin.contains("EnigmaticLegacyCompat.isEnchanterPearlActive(player)"));
        assertTrue(mixin.contains("EnigmaticLegacyCompat.mergePearlEnchantments"));
        assertTrue(mixin.contains("ApothEnchantmentMenu.class"));
        assertTrue(mixin.contains("getClass() != ApothEnchantmentMenu.class"));
        assertTrue(mixin.contains("@Shadow(remap = false)"));
        assertFalse(mixin.contains("MechanicalRavenEnchantTile"));
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

    private static String read(String directory, String file) throws IOException {
        return Files.readString(MAIN_JAVA.resolve(directory).resolve(file));
    }
}
