package com.apotheosis_artifice.enchant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.apotheosis_artifice.compat.EasyMagicInventoryMigrator;
import org.junit.jupiter.api.Test;

class EnchanterPearlCompatibilityTest {

    @Test
    void jeiSupplementLinesPreserveInheritedAndPotionColors() {
        var text = net.minecraft.network.chat.Component.literal("Cast ").withStyle(net.minecraft.ChatFormatting.YELLOW)
            .append(net.minecraft.network.chat.Component.literal("Speed III").withStyle(net.minecraft.ChatFormatting.BLUE)
                .append(net.minecraft.network.chat.Component.literal(" [III - IV]").withStyle(net.minecraft.ChatFormatting.GRAY)))
            .append("\nSupported spells only");
        var original = text.copy();
        var lines = com.apotheosis_artifice.jei.AffixDetailCategory.splitTooltipLines(text);
        assertEquals(List.of("Cast Speed III [III - IV]", "Supported spells only"), lines.stream().map(net.minecraft.network.chat.Component::getString).toList());
        var colors = new ArrayList<Integer>();
        for (var line : lines) {
            line.visit((net.minecraft.network.chat.Style style, String segment) -> {
                if (!segment.isEmpty()) colors.add(style.getColor().getValue());
                return java.util.Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
        }
        assertEquals(List.of(0xFFFF55, 0x5555FF, 0xAAAAAA, 0xFFFF55), colors);
        assertEquals(original, text);
        var single = net.minecraft.network.chat.Component.translatable("test.key", "value");
        assertEquals(List.of(single), com.apotheosis_artifice.jei.AffixDetailCategory.splitTooltipLines(single));
        assertEquals(List.of("a", "", "b", ""), com.apotheosis_artifice.jei.AffixDetailCategory.splitTooltipLines(
            net.minecraft.network.chat.Component.literal("a\n\nb\n")).stream().map(net.minecraft.network.chat.Component::getString).toList());
    }

    private static final Path MAIN_JAVA = Path.of("src", "main", "java", "com", "apotheosis_artifice");
    private static final Path MIXIN_CONFIG = Path.of("src", "main", "resources", "apotheosis_artifice.mixins.json");

    @Test
    void jeiSourcePathsPreserveNamespacesAndNestedDirectories() throws Exception {
        assertEquals("data\\apotheosis_artifice\\affixes\\attribute\\armor.json",
            com.apotheosis_artifice.jei.AffixDetailCategory.sourcePath(new net.minecraft.resources.ResourceLocation("apotheosis_artifice", "attribute/armor"), "affixes"));
        assertEquals("data\\apotheosis\\gems\\the_end\\endersurge.json",
            com.apotheosis_artifice.jei.AffixDetailCategory.sourcePath(new net.minecraft.resources.ResourceLocation("apotheosis", "the_end/endersurge"), "gems"));
        assertEquals("data\\custom_pack\\affixes\\nested\\deep\\bonus.json",
            com.apotheosis_artifice.jei.AffixDetailCategory.sourcePath(new net.minecraft.resources.ResourceLocation("custom_pack", "nested/deep/bonus"), "affixes"));
        Path dataRoot = Path.of("src", "main", "resources", "data");
        try (var files = Files.walk(dataRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                Path relative = dataRoot.relativize(file);
                String directory = relative.getName(1).toString();
                if (!directory.equals("affixes") && !directory.equals("gems")) continue;
                String entryPath = relative.subpath(2, relative.getNameCount()).toString().replace('\\', '/');
                var id = new net.minecraft.resources.ResourceLocation(relative.getName(0).toString(), entryPath.substring(0, entryPath.length() - 5));
                assertEquals("data\\" + relative.toString().replace('/', '\\'),
                    com.apotheosis_artifice.jei.AffixDetailCategory.sourcePath(id, directory));
            }
        }
    }

    @Test
    void jeiSourceTooltipOnlyAppearsInsideTheGear() {
        var id = new net.minecraft.resources.ResourceLocation("apotheosis_artifice", "attribute/armor");
        for (double[] point : List.of(new double[]{162, 1}, new double[]{173.99, 12.99}, new double[]{168, 7})) {
            var tooltip = com.apotheosis_artifice.jei.AffixDetailCategory.sourceTooltip(id, "affixes", point[0], point[1]);
            assertEquals(1, tooltip.size());
            var contents = (net.minecraft.network.chat.contents.TranslatableContents) tooltip.get(0).getContents();
            assertEquals("jei.apotheosis_artifice.data_source", contents.getKey());
            assertEquals("data\\apotheosis_artifice\\affixes\\attribute\\armor.json", contents.getArgs()[0]);
        }
        for (double[] point : List.of(new double[]{161.99, 1}, new double[]{174, 1}, new double[]{162, 0.99},
            new double[]{162, 13}, new double[]{22, 3}, new double[]{2, 28})) {
            assertTrue(com.apotheosis_artifice.jei.AffixDetailCategory.sourceTooltip(id, "affixes", point[0], point[1]).isEmpty());
        }
    }

    @Test
    void attributeButtonTranslationBeforeSuperUsesAStaticRedirect() throws IOException {
        var target = readClassNode("dev/shadowsoffire/attributeslib/client/AttributesGui$HideUnchangedButton");
        var constructor = target.methods.stream().filter(method -> method.name.equals("<init>")).findFirst().orElseThrow();
        int literalIndex = -1;
        int superIndex = -1;
        for (int i = 0; i < constructor.instructions.size(); i++) {
            if (!(constructor.instructions.get(i) instanceof org.objectweb.asm.tree.MethodInsnNode call)) continue;
            if (call.owner.equals("net/minecraft/network/chat/Component") && call.name.equals("literal")) literalIndex = i;
            if (call.owner.equals(target.superName) && call.name.equals("<init>")) superIndex = i;
        }
        assertTrue(literalIndex >= 0 && superIndex > literalIndex, "The translated label is an argument to the superclass constructor");
        var mixin = readClassNode("com/apotheosis_artifice/mixin/client/AttributesGuiLocalizeMixin$HideUnchangedButtonMixin");
        var handler = mixin.methods.stream().filter(method -> method.name.equals("localizeHideUnchangedAttr")).findFirst().orElseThrow();
        assertTrue((handler.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0,
            "Mixin redirects before super() must be static because this is not initialized");
    }

    @Test
    void gemCaseSearchConsumesTypingKeysBeforeContainerShortcuts() throws Exception {
        for (int key : new int[]{69, 81, 49, 73}) {
            var screen = gemCaseKeyboardHarness();
            screen.filter.focused = true;
            screen.inventoryKey = key;

            assertTrue(screen.keyPressed(key, 18, 0));
            assertFalse(screen.closed, "Typing must not close the gem case");
            assertEquals(0, screen.containerCalls, "Typing must not reach container shortcuts");
            assertEquals(1, screen.filter.keyCalls);
            org.junit.jupiter.api.Assertions.assertArrayEquals(new int[]{key, 18, 0}, screen.filter.lastKey);
        }
    }

    @Test
    void gemCaseSearchPassesEditingKeysAndModifiersOnlyOnce() throws Exception {
        for (int[] event : new int[][]{{259, 14, 0}, {65, 30, 2}, {263, 75, 1}}) {
            var screen = gemCaseKeyboardHarness();
            screen.filter.focused = true;
            screen.filter.handlesKey = true;

            assertTrue(screen.keyPressed(event[0], event[1], event[2]));
            assertEquals(1, screen.filter.keyCalls);
            org.junit.jupiter.api.Assertions.assertArrayEquals(event, screen.filter.lastKey);
            assertEquals(0, screen.containerCalls);
            assertFalse(screen.closed);
        }
    }

    @Test
    void gemCaseSearchPreservesEscapeTabAndUnfocusedInventoryShortcut() throws Exception {
        var escape = gemCaseKeyboardHarness();
        escape.filter.focused = true;
        assertTrue(escape.keyPressed(256, 1, 0));
        assertTrue(escape.closed);
        assertEquals(0, escape.filter.keyCalls);

        var tab = gemCaseKeyboardHarness();
        tab.filter.focused = true;
        tab.keyPressed(258, 15, 0);
        assertEquals(1, tab.containerCalls, "Tab must reach normal focus navigation");
        assertFalse(tab.closed);

        for (int state = 0; state < 4; state++) {
            var screen = gemCaseKeyboardHarness();
            if (state == 1) screen.filter = null;
            if (state == 2) {
                screen.filter.focused = true;
                screen.filter.editable = false;
            }
            if (state == 3) {
                screen.filter.focused = true;
                screen.filter.visible = false;
            }
            assertTrue(screen.keyPressed(69, 18, 0));
            assertTrue(screen.closed);
            assertEquals(1, screen.containerCalls);
        }
    }

    @Test
    void gemCaseOutputPagesStopAtTheSelectedGemHighestRarity() throws Exception {
        for (int[] boundary : new int[][]{{0, 0}, {5, 0}, {6, 1}, {10, 1}, {11, 2}, {15, 2}, {16, 3}}) {
            var menu = gemCasePagingHarness();
            menu.selectedGem = new GemCasePagingGem(menu.rarityOrder.get(boundary[0]));
            menu.setPage(Integer.MAX_VALUE);
            assertEquals(menu.rarityOrder.get(boundary[1] * 5), menu.slots.get(2).rarityId);
            assertEquals(boundary[1], menu.getMaxPage());
            menu.setPage(-1);
            assertEquals(menu.rarityOrder.get(0), menu.slots.get(2).rarityId);
        }
    }

    @Test
    void gemCaseOutputWithoutASelectedOrRegisteredGemStaysOnTheFirstPage() throws Exception {
        var menu = gemCasePagingHarness();
        menu.setPage(3);
        assertEquals(menu.rarityOrder.get(0), menu.slots.get(2).rarityId);
        menu.selectedGem = new GemCasePagingGem(new net.minecraft.resources.ResourceLocation("test", "missing"));
        menu.setPage(3);
        assertEquals(menu.rarityOrder.get(0), menu.slots.get(2).rarityId);
    }

    @Test
    void gemCaseOutputPageKeepsTheOverlapNeededToForgeHigherGems() throws Exception {
        var menu = gemCasePagingHarness();
        menu.selectedGem = new GemCasePagingGem(menu.rarityOrder.get(6));
        menu.setPage(1);
        assertEquals(menu.rarityOrder.get(5), menu.slots.get(2).rarityId);
        assertEquals(menu.rarityOrder.get(6), menu.slots.get(3).rarityId);
        menu.selectedGem = new GemCasePagingGem(menu.rarityOrder.get(5));
        menu.setPage(1);
        assertEquals(menu.rarityOrder.get(0), menu.slots.get(2).rarityId);
    }

    @Test
    void gemCaseOutputButtonsAreHiddenWithoutExtraSupportedTiers() throws Exception {
        var source = readClassNode("com/apotheosis_artifice/gemcase/GemCaseScreen$PageButton");
        assertTrue(source.methods.stream().anyMatch(method -> method.name.equals("updateState")),
            "Page button visibility must refresh when the selected gem changes");
        String name = "com/apotheosis_artifice/enchant/GemCasePagingButtonHarness";
        String parent = org.objectweb.asm.Type.getInternalName(GemCasePagingWidget.class);
        var remapper = new org.objectweb.asm.commons.SimpleRemapper(java.util.Map.of(
            source.name, name, source.superName, parent,
            "com/apotheosis_artifice/gemcase/GemCaseScreen", org.objectweb.asm.Type.getInternalName(GemCasePagingScreen.class)));
        var writer = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES | org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        writer.visit(org.objectweb.asm.Opcodes.V17, org.objectweb.asm.Opcodes.ACC_PUBLIC, name, null, parent, null);
        for (var field : source.fields) writer.visitField(field.access, field.name, remapper.mapDesc(field.desc), null, null).visitEnd();
        for (var method : source.methods) {
            if (!List.of("<init>", "canPress", "updateState").contains(method.name)) continue;
            method.accept(new org.objectweb.asm.commons.MethodRemapper(
                writer.visitMethod(method.access, method.name, remapper.mapMethodDesc(method.desc), null, null), remapper));
        }
        writer.visitEnd();
        var buttonClass = defineGemCasePagingHarness(name, writer.toByteArray());
        var update = buttonClass.getDeclaredMethod("updateState");
        update.setAccessible(true);
        var screen = new GemCasePagingScreen();
        for (boolean forward : new boolean[]{false, true}) {
            var button = (GemCasePagingWidget) buttonClass.getConstructor(int.class, int.class, GemCasePagingScreen.class, boolean.class)
                .newInstance(0, 0, screen, forward);
            screen.maxPage = 0;
            screen.page = 0;
            update.invoke(button);
            assertFalse(button.visible);
            assertFalse(button.active);
            screen.maxPage = 1;
            update.invoke(button);
            assertTrue(button.visible);
            assertEquals(forward, button.active);
            screen.page = 1;
            update.invoke(button);
            assertTrue(button.visible);
            assertEquals(!forward, button.active);
            screen.maxPage = 0;
            screen.page = 0;
            update.invoke(button);
            assertFalse(button.visible);
            assertFalse(button.active);
        }
    }

    private static GemCasePagingMenu gemCasePagingHarness() throws Exception {
        var source = readClassNode("com/apotheosis_artifice/gemcase/GemCaseMenu");
        String name = "com/apotheosis_artifice/enchant/GemCasePagingMenuHarness";
        String parent = org.objectweb.asm.Type.getInternalName(GemCasePagingMenu.class);
        var writer = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES | org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        writer.visit(org.objectweb.asm.Opcodes.V17, org.objectweb.asm.Opcodes.ACC_PUBLIC, name, null, parent, null);
        var constructor = writer.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, parent, "<init>", "()V", false);
        constructor.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        var remapper = new org.objectweb.asm.commons.SimpleRemapper(java.util.Map.of(
            source.name, name, source.superName, parent,
            "net/minecraft/world/inventory/AbstractContainerMenu", parent,
            "net/minecraft/world/inventory/Slot", org.objectweb.asm.Type.getInternalName(GemCasePagingSlot.class),
            "com/apotheosis_artifice/gemcase/GemCaseSlot", org.objectweb.asm.Type.getInternalName(GemCasePagingSlot.class),
            "dev/shadowsoffire/apotheosis/adventure/socket/gem/Gem", org.objectweb.asm.Type.getInternalName(GemCasePagingGem.class),
            "dev/shadowsoffire/apotheosis/adventure/loot/LootRarity", org.objectweb.asm.Type.getInternalName(GemCasePagingRarity.class),
            "dev/shadowsoffire/apotheosis/adventure/loot/RarityRegistry", org.objectweb.asm.Type.getInternalName(GemCasePagingRegistry.class),
            "dev/shadowsoffire/placebo/codec/CodecProvider", "java/lang/Object"));
        for (var method : source.methods) {
            if (!List.of("setPage", "getMaxPage").contains(method.name)) continue;
            method.accept(new org.objectweb.asm.commons.MethodRemapper(
                writer.visitMethod(method.access, method.name, remapper.mapMethodDesc(method.desc), null, null), remapper));
        }
        writer.visitEnd();
        return (GemCasePagingMenu) defineGemCasePagingHarness(name, writer.toByteArray()).getConstructor().newInstance();
    }

    private static Class<?> defineGemCasePagingHarness(String name, byte[] bytes) {
        return new ClassLoader(EnchanterPearlCompatibilityTest.class.getClassLoader()) {
            Class<?> defineHarness() { return defineClass(name.replace('/', '.'), bytes, 0, bytes.length); }
        }.defineHarness();
    }

    public static class GemCasePagingMenu {
        public static final net.minecraft.resources.ResourceLocation NONE_RARITY = new net.minecraft.resources.ResourceLocation("test", "none");
        public List<net.minecraft.resources.ResourceLocation> rarityOrder = new ArrayList<>();
        public net.minecraft.core.NonNullList<GemCasePagingSlot> slots = net.minecraft.core.NonNullList.create();
        public GemCasePagingGem selectedGem;
        public int page;
        public GemCasePagingMenu() {
            for (int i = 0; i < 22; i++) rarityOrder.add(new net.minecraft.resources.ResourceLocation("test", "tier_" + i));
            for (int i = 0; i < 8; i++) slots.add(new GemCasePagingSlot());
        }
        public void setPage(int page) { throw new AssertionError("Production paging method was not loaded"); }
        public int getMaxPage() { throw new AssertionError("Production page limit was not loaded"); }
    }

    public static class GemCasePagingSlot { public net.minecraft.resources.ResourceLocation rarityId; }
    public record GemCasePagingRarity(net.minecraft.resources.ResourceLocation id) {}
    public record GemCasePagingGem(net.minecraft.resources.ResourceLocation id) {
        public GemCasePagingRarity getMaxRarity() { return new GemCasePagingRarity(id); }
    }
    public static class GemCasePagingRegistry {
        public static final GemCasePagingRegistry INSTANCE = new GemCasePagingRegistry();
        public net.minecraft.resources.ResourceLocation getKey(Object rarity) { return ((GemCasePagingRarity) rarity).id(); }
    }
    public static class GemCasePagingScreen { public int page; public int maxPage; }
    public static class GemCasePagingWidget {
        public boolean visible = true;
        public boolean active = true;
        public GemCasePagingWidget(int x, int y, int width, int height, net.minecraft.network.chat.Component title) {}
    }

    private static GemCaseKeyboardContainer gemCaseKeyboardHarness() throws Exception {
        var source = readClassNode("com/apotheosis_artifice/gemcase/GemCaseScreen");
        String name = "com/apotheosis_artifice/enchant/GemCaseKeyboardHarness";
        String parent = org.objectweb.asm.Type.getInternalName(GemCaseKeyboardContainer.class);
        var writer = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES | org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        writer.visit(org.objectweb.asm.Opcodes.V17, org.objectweb.asm.Opcodes.ACC_PUBLIC, name, null, parent, null);
        var constructor = writer.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, parent, "<init>", "()V", false);
        constructor.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        var remapper = new org.objectweb.asm.commons.SimpleRemapper(java.util.Map.of(
            source.name, name,
            source.superName, parent,
            "net/minecraft/client/gui/components/EditBox", org.objectweb.asm.Type.getInternalName(GemCaseSearchInput.class)));
        source.methods.stream().filter(method -> method.name.equals("keyPressed") && method.desc.equals("(III)Z")).findFirst().ifPresent(method ->
            method.accept(new org.objectweb.asm.commons.MethodRemapper(
                writer.visitMethod(method.access, method.name, method.desc, null, null), remapper)));
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        var screenClass = new ClassLoader(EnchanterPearlCompatibilityTest.class.getClassLoader()) {
            Class<?> defineScreen() {
                return defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
            }
        }.defineScreen();
        return (GemCaseKeyboardContainer) screenClass.getConstructor().newInstance();
    }

    public static class GemCaseKeyboardContainer {
        public GemCaseSearchInput filter = new GemCaseSearchInput();
        public int inventoryKey = 69;
        public int containerCalls;
        public boolean closed;

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            this.containerCalls++;
            if (keyCode == 256) {
                this.closed = true;
                return true;
            }
            if (this.filter != null && this.filter.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (keyCode == this.inventoryKey) {
                this.closed = true;
                return true;
            }
            return false;
        }
    }

    public static class GemCaseSearchInput {
        public boolean focused;
        public boolean visible = true;
        public boolean editable = true;
        public boolean handlesKey;
        public int keyCalls;
        public int[] lastKey;

        public boolean canConsumeInput() {
            return this.focused && this.visible && this.editable;
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            this.keyCalls++;
            this.lastKey = new int[]{keyCode, scanCode, modifiers};
            return this.canConsumeInput() && this.handlesKey;
        }
    }

    private static org.objectweb.asm.tree.ClassNode readClassNode(String name) throws IOException {
        var node = new org.objectweb.asm.tree.ClassNode();
        try (var input = EnchanterPearlCompatibilityTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            org.junit.jupiter.api.Assertions.assertNotNull(input, name);
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    void cleansingHasAJeiExtensionBeforeApotheosisRegistersItsRecipes() throws Exception {
        net.minecraftforge.fml.loading.FMLPaths.loadAbsolutePaths(Path.of("build", "test-game"));
        bootstrapItems();
        new ApotheosisArtificeJEIPlugin();
        var field = dev.shadowsoffire.apotheosis.adventure.compat.ApothSmithingCategory.class.getDeclaredField("EXTENSIONS");
        field.setAccessible(true);
        var extensions = (java.util.Map<?, ?>) field.get(null);
        org.junit.jupiter.api.Assertions.assertNotNull(extensions.get(CleansingRecipe.class),
            "Apotheosis registers every reactive smithing recipe, so cleansing must already have a display extension");
    }


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
        int fuelRead = mechanicalMenu.indexOf("this.getSlot(1).getItem().getCount()", method);
        int decision = mechanicalMenu.indexOf("resolveGoldCount(pearlActive", method);
        assertTrue(method >= 0);
        assertTrue(pearlCheck > method);
        assertTrue(fuelRead > pearlCheck);
        assertTrue(decision > pearlCheck);
    }

    @Test
    void removingEasyMagicMigratesPersistentItemsAfterMenuConstruction() throws IOException {
        String menuMixin = read("mixin", "ApothEnchantmentMenuMixin.java");
        String migrator = read("compat", "EasyMagicInventoryMigrator.java");
        String migrationMixin = read("mixin", "AbstractContainerMenuEasyMagicMigrationMixin.java");
        String migrationInterface = read("compat", "EasyMagicInventoryMigration.java");
        String mechanicalMenu = read("enchant", "MechanicalRavenEnchantMenu.java");
        String mixinConfig = Files.readString(MIXIN_CONFIG);

        assertTrue(menuMixin.contains("artifice$pendingEasyMagicInventory"));
        assertTrue(menuMixin.contains("implements EasyMagicInventoryMigration"));
        assertTrue(menuMixin.contains("artifice$queueEasyMagicInventoryMigration"));
        assertFalse(menuMixin.contains("@Inject(method = \"broadcastChanges\""));
        assertTrue(migrationMixin.contains("@Mixin(AbstractContainerMenu.class)"));
        assertTrue(migrationMixin.contains("broadcastChanges"));
        assertTrue(migrationMixin.contains("instanceof EasyMagicInventoryMigration migration"));
        assertTrue(migrationMixin.contains("migration.artifice$migrateEasyMagicInventory()"));
        assertTrue(migrationInterface.contains("interface EasyMagicInventoryMigration"));
        assertTrue(migrationInterface.contains("artifice$migrateEasyMagicInventory"));
        assertTrue(mixinConfig.contains("AbstractContainerMenuEasyMagicMigrationMixin"));
        assertTrue(menuMixin.contains("if (EasyMagicCompat.isLoaded())"));
        assertTrue(menuMixin.contains("EasyMagicInventoryMigrator.migrate(new EasyMagicInventoryMigrator.Source<ItemStack>()"));
        assertTrue(migrator.contains("target.mayPlace(moved)"));
        assertTrue(migrator.contains("returnStack.accept(moved)"));
        assertTrue(migrator.contains("source.clear(sourceSlot)"));
        assertTrue(mechanicalMenu.contains("this.enchantSlots = te.getEnchantInventory()"));
        assertTrue(mechanicalMenu.contains("placeItemBackInInventory(fromSave)"));
    }

    @Test
    void easyMagicInventoryMigratorMovesStacksAndIsIdempotent() {
        MemorySource source = new MemorySource("input", "fuel", "catalyst");
        MemoryTarget input = new MemoryTarget();
        MemoryTarget fuel = new MemoryTarget();
        List<String> returned = new ArrayList<>();

        EasyMagicInventoryMigrator.migrate(source, input, fuel, returned::add, String::new, String::isEmpty);

        assertEquals("input", input.value);
        assertEquals("fuel", fuel.value);
        assertEquals(List.of("catalyst"), returned);
        assertTrue(source.isEmpty());

        EasyMagicInventoryMigrator.migrate(source, input, fuel, returned::add, String::new, String::isEmpty);
        assertEquals(List.of("catalyst"), returned);
    }

    @Test
    void easyMagicInventoryMigratorReturnsOccupiedOrInvalidTargets() {
        MemorySource source = new MemorySource("input", "fuel", "");
        MemoryTarget occupied = new MemoryTarget("existing");
        MemoryTarget invalid = new MemoryTarget();
        invalid.accepts = false;
        List<String> returned = new ArrayList<>();

        EasyMagicInventoryMigrator.migrate(source, occupied, invalid, returned::add, String::new, String::isEmpty);

        assertEquals(List.of("input", "fuel"), returned);
        assertEquals("existing", occupied.value);
        assertTrue(source.isEmpty());
    }

    @Test
    void inventoryMigrationClearsSourceBeforeAnotherMenuCanReenter() {
        MemorySource source = new MemorySource("input", "", "");
        MemoryTarget first = new MemoryTarget();
        MemoryTarget second = new MemoryTarget();
        List<String> returned = new ArrayList<>();
        EasyMagicInventoryMigrator.Target<String> reentrant = new EasyMagicInventoryMigrator.Target<>() {
            @Override public boolean hasItem() { return first.hasItem(); }
            @Override public boolean mayPlace(String stack) { return true; }
            @Override public void set(String stack) {
                first.set(stack);
                EasyMagicInventoryMigrator.migrate(source, second, new MemoryTarget(), returned::add, String::new, String::isEmpty);
            }
        };
        EasyMagicInventoryMigrator.migrate(source, reentrant, new MemoryTarget(), returned::add, String::new, String::isEmpty);
        assertEquals("input", first.value);
        assertTrue(second.value.isEmpty());
        assertTrue(returned.isEmpty());
    }

    @Test
    void mechanicalRavenGoldCountDecisionUsesPearlOrRealFuel() {
        assertEquals(64, MechanicalRavenEnchantMenu.resolveGoldCount(true, 0));
        assertEquals(7, MechanicalRavenEnchantMenu.resolveGoldCount(false, 7));
    }

    @Test
    void mechanicalRavenStopsAdvertisingFuelWhenTheLastLapisIsRemoved() {
        assertEquals(7, MechanicalRavenEnchantMenu.resolveGoldCount(false, 7));
        assertEquals(0, MechanicalRavenEnchantMenu.resolveGoldCount(false, 0));
    }

    @Test
    void blockedOutputSurvivesSaveAndRetriesWithoutEnchantingAgain() throws Exception {
        bootstrapItems();
        TestMechanicalTile tile = new TestMechanicalTile(0);
        tile.getIOInv().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 2));
        tile.getIOInv().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 63));
        invokeTile(tile, "tryAutoEnchant");
        invokeTile(tile, "tryAutoEnchant");
        assertEquals(1, tile.enchantCalls);
        assertEquals(1, tile.getIOInv().getStackInSlot(0).getCount());
        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        tile.saveAdditional(saved);
        assertEquals(8, net.minecraft.world.item.ItemStack.of(saved.getCompound("pending_output")).getCount());
        TestMechanicalTile restored = new TestMechanicalTile(0);
        restored.load(saved);
        restored.getIOInv().setStackInSlot(1, net.minecraft.world.item.ItemStack.EMPTY);
        invokeTile(restored, "flushPendingOutput");
        invokeTile(restored, "flushPendingOutput");
        assertEquals(8, restored.getIOInv().getStackInSlot(1).getCount());
        assertTrue(restored.getIOInv().getStackInSlot(1).is(net.minecraft.world.item.Items.DIAMOND));
        assertEquals(0, restored.enchantCalls);
    }

    @Test
    void partialBoundInsertionPreservesEveryRemainingItem() throws Exception {
        bootstrapItems();
        TestMechanicalTile tile = new TestMechanicalTile(3);
        tile.getIOInv().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 2));
        tile.getIOInv().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 63));
        invokeTile(tile, "tryAutoEnchant");
        assertEquals(3, tile.bound.getStackInSlot(0).getCount());
        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        tile.saveAdditional(saved);
        assertEquals(5, net.minecraft.world.item.ItemStack.of(saved.getCompound("pending_output")).getCount());
        tile.getIOInv().setStackInSlot(1, net.minecraft.world.item.ItemStack.EMPTY);
        invokeTile(tile, "flushPendingOutput");
        assertEquals(5, tile.getIOInv().getStackInSlot(1).getCount());
        assertEquals(8, tile.bound.getStackInSlot(0).getCount() + tile.getIOInv().getStackInSlot(1).getCount());
    }

    @Test
    void mechanicalInputHasOneOwnerAndSavesItsCurrentState() {
        bootstrapItems();
        TestMechanicalTile tile = new TestMechanicalTile(0);
        tile.setSavedEnchantSlot(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK));
        net.minecraft.world.Container first = tile.getEnchantInventory();
        net.minecraft.world.Container second = tile.getEnchantInventory();
        assertEquals(1, first.removeItem(0, 1).getCount());
        assertTrue(second.getItem(0).isEmpty());
        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        tile.saveAdditional(saved);
        assertFalse(saved.contains("ench_slot"));
    }

    @Test
    void oneAutomaticCycleProcessesOnlyOneSource() throws Exception {
        bootstrapItems();
        TestMechanicalTile tile = new TestMechanicalTile(0);
        tile.setSavedEnchantSlot(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK));
        tile.getIOInv().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 3));
        invokeTile(tile, "tryAutoEnchant");
        assertEquals(1, tile.enchantCalls);
        assertTrue(tile.getSavedEnchantSlot().isEmpty());
        assertEquals(3, tile.getIOInv().getStackInSlot(0).getCount());
        invokeTile(tile, "tryAutoEnchant");
        assertEquals(2, tile.enchantCalls);
        assertEquals(2, tile.getIOInv().getStackInSlot(0).getCount());
    }

    @Test
    void savedEnchantedBooksCanLeaveTheInputWithoutAnotherEnchant() {
        bootstrapItems();
        TestMechanicalTile tile = new TestMechanicalTile(0);
        tile.setSavedEnchantSlot(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK));
        tile.flushEnchantedInput();
        assertTrue(tile.getSavedEnchantSlot().isEmpty());
        assertTrue(tile.getIOInv().getStackInSlot(1).is(net.minecraft.world.item.Items.ENCHANTED_BOOK));
        assertEquals(0, tile.enchantCalls);
    }

    private static void bootstrapItems() {
        net.minecraft.SharedConstants.tryDetectVersion();
        new net.minecraftforge.network.NetworkEvent(() -> null).getListenerList();
        new net.minecraftforge.network.NetworkEvent.GatherLoginPayloadsEvent(new ArrayList<>(), false).getListenerList();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void ravenWorldBookUsesTheTablesCustomTexture() throws Exception {
        bootstrapItems();
        var table = registeredBookTable();
        var expected = table.getBookGuiTexture();
        var headlessType = net.minecraft.client.renderer.RenderType.entitySolid(expected);
        assertEquals(expected, bookRenderTexture(headlessType));
        var tile = new net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity(
            net.minecraft.core.BlockPos.ZERO, table.defaultBlockState());
        var original = bookVertexConsumer();
        var custom = bookVertexConsumer();
        var requested = new java.util.concurrent.atomic.AtomicReference<net.minecraft.client.renderer.RenderType>();
        net.minecraft.client.renderer.MultiBufferSource buffers = type -> {
            requested.set(type);
            return custom;
        };
        var route = worldBookTextureMethod();
        var mixin = route.getDeclaringClass().getDeclaredConstructor().newInstance();
        assertSame(custom, route.invoke(mixin, original, tile, 0.5F,
            new com.mojang.blaze3d.vertex.PoseStack(), buffers, 15728880, 0));
        assertNotNull(requested.get());
        assertEquals(expected, bookRenderTexture(requested.get()));
    }

    @Test
    void ravenWorldBookLeavesVanillaTablesUnchanged() throws Exception {
        bootstrapItems();
        var tile = new net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity(
            net.minecraft.core.BlockPos.ZERO, net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE.defaultBlockState());
        var original = bookVertexConsumer();
        var custom = bookVertexConsumer();
        var requested = new java.util.concurrent.atomic.AtomicReference<net.minecraft.client.renderer.RenderType>();
        net.minecraft.client.renderer.MultiBufferSource buffers = type -> {
            requested.set(type);
            return custom;
        };
        var route = worldBookTextureMethod();
        var mixin = route.getDeclaringClass().getDeclaredConstructor().newInstance();
        assertSame(original, route.invoke(mixin, original, tile, 0.5F,
            new com.mojang.blaze3d.vertex.PoseStack(), buffers, 15728880, 0));
        assertNull(requested.get());
    }

    @Test
    void ravenWorldBookMixinIsRegisteredForTheClient() throws IOException {
        var config = com.google.gson.JsonParser.parseString(Files.readString(MIXIN_CONFIG)).getAsJsonObject();
        assertTrue(java.util.stream.StreamSupport.stream(config.getAsJsonArray("client").spliterator(), false)
            .anyMatch(entry -> entry.getAsString().equals("client.EnchantmentBookMixin$WorldBook")));
    }

    private static java.lang.reflect.Method worldBookTextureMethod() throws Exception {
        Class<?> mixin = null;
        try {
            mixin = Class.forName("com.apotheosis_artifice.mixin.client.EnchantmentBookMixin$WorldBook");
        } catch (ClassNotFoundException missing) {
        }
        assertNotNull(mixin, "World-book texture routing is not implemented");
        var method = mixin.getDeclaredMethod("artifice$bookTexture",
            com.mojang.blaze3d.vertex.VertexConsumer.class,
            net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity.class,
            float.class, com.mojang.blaze3d.vertex.PoseStack.class,
            net.minecraft.client.renderer.MultiBufferSource.class, int.class, int.class);
        method.setAccessible(true);
        return method;
    }

    private static com.mojang.blaze3d.vertex.VertexConsumer bookVertexConsumer() {
        return (com.mojang.blaze3d.vertex.VertexConsumer) java.lang.reflect.Proxy.newProxyInstance(
            EnchanterPearlCompatibilityTest.class.getClassLoader(),
            new Class<?>[] { com.mojang.blaze3d.vertex.VertexConsumer.class },
            (proxy, method, args) -> { throw new AssertionError("Identity-only vertex consumer was invoked"); });
    }

    private static Object bookRenderTexture(net.minecraft.client.renderer.RenderType type) throws Exception {
        var stateField = type.getClass().getDeclaredField("state");
        stateField.setAccessible(true);
        var state = stateField.get(type);
        var textureField = state.getClass().getDeclaredField("textureState");
        textureField.setAccessible(true);
        var textureState = textureField.get(state);
        var texture = net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard.class
            .getDeclaredMethod("cutoutTexture");
        texture.setAccessible(true);
        return ((java.util.Optional<?>) texture.invoke(textureState)).orElseThrow();
    }

    private static TestBookTable registeredBookTable() throws Exception {
        var registry = (net.minecraftforge.registries.ForgeRegistry<net.minecraft.world.level.block.Block>)
            net.minecraftforge.registries.ForgeRegistries.BLOCKS;
        boolean locked = registry.isLocked();
        var wrapper = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        var unfreeze = wrapper.getClass().getMethod("unfreeze");
        unfreeze.setAccessible(true);
        registry.unfreeze();
        unfreeze.invoke(wrapper);
        try {
            var table = new TestBookTable(new net.minecraft.resources.ResourceLocation("apotheosis_artifice", "raven_book"));
            registry.register(new net.minecraft.resources.ResourceLocation("artifice_test", "world_book_table"), table);
            return table;
        } finally {
            wrapper.freeze();
            if (locked) registry.freeze();
        }
    }

    private static final class TestBookTable extends net.minecraft.world.level.block.Block implements BookTexturedTable {
        private final net.minecraft.resources.ResourceLocation texture;

        private TestBookTable(net.minecraft.resources.ResourceLocation texture) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
            this.texture = texture;
        }

        @Override
        public net.minecraft.resources.ResourceLocation getBookTextureId() {
            return this.texture;
        }
    }

    @Test
    void gemCaseMaterialChangesImmediatelyMarkTheTileDirty() {
        bootstrapItems();
        var tile = new TestGemCaseTile();
        tile.upgradeMatInv.setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 12));
        assertTrue(tile.dirty);
    }

    @Test
    void gemCaseMaterialLoadClearsSlotsMissingFromOlderSaves() {
        bootstrapItems();
        var tile = new TestGemCaseTile();
        tile.upgradeMatInv.setStackInSlot(5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 12));
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Size", 1);
        tag.put("Items", new net.minecraft.nbt.CompoundTag());
        tile.upgradeMatInv.deserializeNBT(tag);
        assertTrue(tile.upgradeMatInv.getStackInSlot(5).isEmpty());
    }

    @Test
    void gemCaseMaterialLoadIgnoresOversizedSlotMetadata() {
        bootstrapItems();
        var tile = new TestGemCaseTile();
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Size", 100);
        tag.put("Items", new net.minecraft.nbt.CompoundTag());
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> tile.upgradeMatInv.deserializeNBT(tag));
        assertEquals(6, tile.upgradeMatInv.getSlots());
    }

    @Test
    void gemCaseMaterialSnapshotsDoNotShareMutableItemTags() {
        bootstrapItems();
        var tile = new TestGemCaseTile();
        var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 500);
        stack.getOrCreateTag().putString("owner", "before");
        tile.upgradeMatInv.setStackInSlot(0, stack);
        var saved = tile.upgradeMatInv.serializeNBT();
        stack.getTag().putString("owner", "after");
        var restored = new TestGemCaseTile();
        restored.upgradeMatInv.deserializeNBT(saved);
        assertEquals(500, restored.upgradeMatInv.getStackInSlot(0).getCount());
        assertEquals("before", restored.upgradeMatInv.getStackInSlot(0).getTag().getString("owner"));
    }

    private static final class TestGemCaseTile extends com.apotheosis_artifice.gemcase.GemCaseTile {
        private boolean dirty;
        private TestGemCaseTile() {
            super(null, net.minecraft.core.BlockPos.ZERO, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        @Override public void setChanged() { this.dirty = true; }
    }

    @Test
    void cleansingOneStackedInputProducesOnlyOneOutputAndKeepsTheRest() {
        bootstrapItems();
        var inventory = new net.minecraft.world.SimpleContainer(3);
        var input = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 32);
        input.getOrCreateTagElement(dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper.AFFIX_DATA).putInt("sockets", 3);
        inventory.setItem(1, input);
        var result = new CleansingRecipe().assemble(inventory, net.minecraft.core.RegistryAccess.EMPTY);
        assertEquals(1, result.getCount());
        assertEquals(32, inventory.getItem(1).getCount());
        assertEquals(3, inventory.getItem(1).getTagElement(dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper.AFFIX_DATA).getInt("sockets"));
        assertFalse(result.getTagElement(dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper.AFFIX_DATA).contains("sockets"));
    }

    @Test
    void gemCaseNetworkRoundTripPreservesFullIntegerCountsAndItemTags() {
        bootstrapItems();
        List<net.minecraft.world.item.ItemStack> materials = new ArrayList<>();
        for (int count : new int[] {0, 64, 128, 256, 65536, Integer.MAX_VALUE}) {
            var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, count);
            if (!stack.isEmpty()) stack.getOrCreateTag().putString("owner", "player");
            materials.add(stack);
        }
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var packet = new com.apotheosis_artifice.ApotheosisNetwork.SyncGemCaseMaterialsPacket(17, materials);
            com.apotheosis_artifice.ApotheosisNetwork.SyncGemCaseMaterialsPacket.encode(packet, buffer);
            var decoded = com.apotheosis_artifice.ApotheosisNetwork.SyncGemCaseMaterialsPacket.decode(buffer);
            assertEquals(17, decoded.menuId());
            for (int i = 0; i < materials.size(); i++) {
                assertTrue(net.minecraft.world.item.ItemStack.matches(materials.get(i), decoded.materials().get(i)));
            }
        } finally { buffer.release(); }
    }

    @Test
    void baseHealthAffixPreservesExternalBaseAndDoesNotStackOnRefresh() {
        assertEquals(41D, com.apotheosis_artifice.affix.HeartBaseListener.resolveBaseValue(40, 0, 1));
        assertEquals(41D, com.apotheosis_artifice.affix.HeartBaseListener.resolveBaseValue(41, 1, 1));
        assertEquals(40D, com.apotheosis_artifice.affix.HeartBaseListener.resolveBaseValue(41, 1, 0));
        assertEquals(52D, com.apotheosis_artifice.affix.HeartBaseListener.resolveBaseValue(51, 1, 2));
    }

    @Test
    void damageResistanceAboveOneCannotBecomeDamageWhenStacked() {
        float once = com.apotheosis_artifice.affix.DamageResistanceAffix.applyReduction(10F, 1.1F);
        assertEquals(0F, once);
        assertEquals(0F, com.apotheosis_artifice.affix.DamageResistanceAffix.applyReduction(once, 1.1F));
        assertEquals(8F, com.apotheosis_artifice.affix.DamageResistanceAffix.applyReduction(10F, 0.2F));
    }

    @Test
    void uniqueSlotBonusSurvivesRemovingOneOfTwoMatchingItems() {
        var id = java.util.UUID.fromString("80000000-0000-0000-0000-000000000001");
        var first = new com.apotheosis_artifice.affix.HeartBaseListener.SlotBonus("necklace", id, 1);
        var second = new com.apotheosis_artifice.affix.HeartBaseListener.SlotBonus("necklace", id, 2);
        var combined = com.apotheosis_artifice.affix.HeartBaseListener.aggregateUniqueBonuses(List.of(first, second));
        assertEquals(2F, combined.get("necklace").get(id));
        var remaining = com.apotheosis_artifice.affix.HeartBaseListener.aggregateUniqueBonuses(List.of(first));
        assertEquals(1F, remaining.get("necklace").get(id));
        assertTrue(com.apotheosis_artifice.affix.HeartBaseListener.aggregateUniqueBonuses(List.of()).isEmpty());
    }

    @Test
    void uniqueSlotBonusesKeepDifferentTargetsAndDifferentIdsIndependent() {
        var id = java.util.UUID.fromString("80000000-0000-0000-0000-000000000001");
        var other = java.util.UUID.fromString("80000000-0000-0000-0000-000000000002");
        var bonuses = List.of(new com.apotheosis_artifice.affix.HeartBaseListener.SlotBonus("necklace", id, 1),
            new com.apotheosis_artifice.affix.HeartBaseListener.SlotBonus("necklace", other, 2),
            new com.apotheosis_artifice.affix.HeartBaseListener.SlotBonus("ring", id, 3));
        var combined = com.apotheosis_artifice.affix.HeartBaseListener.aggregateUniqueBonuses(bonuses);
        assertEquals(2, combined.get("necklace").size());
        assertEquals(3F, combined.get("ring").get(id));
    }

    @Test
    void synchronizedEnchantingSettingsRespectAllSupportedBounds() {
        var packet = new com.apotheosis_artifice.ApotheosisConfig.EnchantingConfigPacket(-1, 2000, 50, 0, 128, 10001);
        assertEquals(1, packet.maxEterna());
        assertEquals(1000, packet.maxQuanta());
        assertEquals(50, packet.maxArcana());
        assertEquals(1, packet.maxEnchantments());
        assertEquals(127, packet.extraLevelCap());
        assertEquals(10000, packet.extraLevelPowerPerLevel());
    }

    @Test
    void missingReplacementLeavesBothOldSlotsUntouched() {
        bootstrapItems();
        var input = new net.minecraft.world.SimpleContainer(2);
        input.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
        var buffer = new net.minecraftforge.items.ItemStackHandler(2);
        buffer.setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 12));
        var inventory = new net.minecraft.world.entity.player.Inventory(null);
        List<net.minecraft.world.item.ItemStack> returned = new ArrayList<>();
        assertFalse(SetRavenStatsPacket.replaceInput(input, buffer, inventory,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD), returned::add));
        assertTrue(input.getItem(0).is(net.minecraft.world.item.Items.DIAMOND));
        assertEquals(12, buffer.getStackInSlot(0).getCount());
        assertTrue(returned.isEmpty());
    }

    @Test
    void ordinaryReplacementReturnsOldItemOnlyAfterReplacingTheSlot() {
        bootstrapItems();
        var input = new net.minecraft.world.SimpleContainer(2);
        input.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
        var inventory = new net.minecraft.world.entity.player.Inventory(null);
        inventory.setItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 2));
        List<net.minecraft.world.item.ItemStack> returned = new ArrayList<>();
        assertTrue(SetRavenStatsPacket.replaceInput(input, null, inventory,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK), stack -> {
                assertTrue(input.getItem(0).is(net.minecraft.world.item.Items.BOOK));
                returned.add(stack);
            }));
        assertEquals(1, inventory.getItem(9).getCount());
        assertEquals(1, input.getItem(0).getCount());
        assertEquals(1, returned.size());
        assertTrue(returned.get(0).is(net.minecraft.world.item.Items.DIAMOND));
    }

    @Test
    void mechanicalReplacementKeepsOneInputAndReturnsOverflowExactlyOnce() {
        bootstrapItems();
        var input = new net.minecraft.world.SimpleContainer(2);
        input.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
        var buffer = new net.minecraftforge.items.ItemStackHandler(2) {
            @Override public int getSlotLimit(int slot) { return 3; }
        };
        buffer.setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 2));
        var inventory = new net.minecraft.world.entity.player.Inventory(null);
        inventory.setItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 8));
        List<net.minecraft.world.item.ItemStack> returned = new ArrayList<>();
        assertTrue(SetRavenStatsPacket.replaceInput(input, buffer, inventory,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 8), returned::add));
        assertTrue(inventory.getItem(9).isEmpty());
        assertEquals(1, input.getItem(0).getCount());
        assertEquals(3, buffer.getStackInSlot(0).getCount());
        assertEquals(3, returned.size());
        assertEquals(4, returned.stream().filter(stack -> stack.is(net.minecraft.world.item.Items.BOOK))
            .mapToInt(net.minecraft.world.item.ItemStack::getCount).sum());
    }

    private static void invokeTile(MechanicalRavenEnchantTile tile, String method) throws Exception {
        var action = MechanicalRavenEnchantTile.class.getDeclaredMethod(method);
        action.setAccessible(true);
        action.invoke(tile);
    }

    private static final class TestMechanicalTile extends MechanicalRavenEnchantTile {
        private final net.minecraftforge.items.ItemStackHandler bound;
        private int enchantCalls;

        private TestMechanicalTile(int capacity) {
            super(net.minecraft.core.BlockPos.ZERO, net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE.defaultBlockState());
            this.bound = new net.minecraftforge.items.ItemStackHandler(1) {
                @Override public int getSlotLimit(int slot) { return capacity; }
            };
        }

        @Override
        public net.minecraft.world.item.ItemStack doEnchant(net.minecraft.world.item.ItemStack input, RavenTableStats stats) {
            this.enchantCalls++;
            return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 8);
        }

        @Override
        public net.minecraft.world.item.ItemStack depositDirectToBound(net.minecraft.world.item.ItemStack stack) {
            return this.bound.insertItem(0, stack, false);
        }
    }

    private static final class MemorySource implements EasyMagicInventoryMigrator.Source<String> {
        private final String[] values;

        private MemorySource(String... values) { this.values = values; }
        @Override public String get(int slot) { return values[slot]; }
        @Override public void clear(int slot) { values[slot] = ""; }
        private boolean isEmpty() { return List.of(values).stream().allMatch(String::isEmpty); }
    }

    private static final class MemoryTarget implements EasyMagicInventoryMigrator.Target<String> {
        private String value = "";
        private boolean accepts = true;

        private MemoryTarget() {}
        private MemoryTarget(String value) { this.value = value; }
        @Override public boolean hasItem() { return !value.isEmpty(); }
        @Override public boolean mayPlace(String stack) { return accepts; }
        @Override public void set(String stack) { value = stack; }
    }

    @Test
    void socketingRecipeValidationKeepsTheMappedVanillaAssemblyAndEvents() throws IOException {
        String mixin = read("mixin", "SocketingRecipeMixin.java");

        assertTrue(mixin.contains("@Inject(method = \"assemble\""));
        assertTrue(mixin.contains("remap = true"));
        assertFalse(mixin.contains("@Overwrite"));
        assertTrue(mixin.contains("cir.setReturnValue(ItemStack.EMPTY)"));
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
        assertTrue(mechanicalMenu.contains("!broadcasting && !this.manualEnchantInProgress"));
        assertTrue(mechanicalMenu.contains("this.tile != null && !this.manualEnchantInProgress"));
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
