# Enchanter Pearl Treasure Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make an active Enigmatic Legacy Enchanter's Pearl enable only Apotheosis treasure enchantments for player-operated enchanting menus.

**Architecture:** Keep optional Enigmatic Legacy reflection in one compatibility helper and transform an already-collected `ApothEnchantmentMenu.TableStats` by changing only its `treasure` flag. Apply that transform to the ordinary Apotheosis menu before its corrected stats reach the client and directly in Raven menu stat gathering; leave mechanical automatic enchanting untouched.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, Apotheosis 7.4.8, Sponge Mixin, JUnit Jupiter 5.10.2, Gradle 8.8.

## Global Constraints

- Use Enchanter's Pearl's own `isPresent(player)` activation rule.
- Affect ordinary Apotheosis, Raven, and manually operated Mechanical Raven enchanting menus.
- Do not affect unattended Mechanical Raven automatic enchanting.
- Enable only `TableStats.treasure`; do not change Eterna, Quanta, Arcana, Rectification, Clues, or Blacklist.
- Remove free enchanting fuel, bonus enchantment generation, enchantment merging, and Eternal Binding handling from this compatibility.
- Preserve all concurrent Easy Magic compatibility changes.
- Keep Enigmatic Legacy optional and free of direct imports from its packages.

---

### Task 1: Replace result merging with a focused TableStats transformer

**Files:**
- Modify: `src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java`
- Modify: `src/main/java/com/apotheosis_artifice/compat/EnigmaticLegacyCompat.java`

**Interfaces:**
- Consumes: `EnchanterPearlItem#isPresent(Player)` through reflection.
- Produces: `public static ApothEnchantmentMenu.TableStats enableTreasure(ApothEnchantmentMenu.TableStats stats, Player player)`.

- [ ] **Step 1: Replace the old merge-oriented test with a failing transformer contract**

Replace `manualRavenEnchantingUsesOptionalEnchanterPearlCompatibility` with source-level assertions that require the new helper and reject the old behavior:

```java
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
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest.enchanterPearlCompatibilityOnlyEnablesTreasure
```

Expected: FAIL because `enableTreasure` does not exist and the old merge methods are still present.

- [ ] **Step 3: Implement the minimal optional compatibility helper**

Change `EnigmaticLegacyCompat` so its public behavior is:

```java
public static TableStats enableTreasure(TableStats stats, Player player) {
    if (stats == null || stats.treasure() || !isEnchanterPearlActive(player)) return stats;
    return new TableStats(
        stats.eterna(), stats.quanta(), stats.arcana(),
        stats.rectification(), stats.clues(), stats.blacklist(), true);
}
```

Retain `isEnchanterPearlActive(Player)`, but reduce initialization to the pearl method only:

```java
private static synchronized void initialize() {
    if (initialized || !ModList.get().isLoaded(MODID)) return;
    initialized = true;
    try {
        Item pearl = ForgeRegistries.ITEMS.getValue(ENCHANTER_PEARL);
        if (pearl == null) return;
        isPresent = pearl.getClass().getMethod("isPresent", Player.class);
        available = true;
    } catch (ReflectiveOperationException | LinkageError e) {
        ApotheosisArtificeMod.LOGGER.warn("Failed to initialize Enigmatic Legacy compatibility", e);
    }
}
```

Remove the `ItemStack` import, the merge method fields, the `mergePearlEnchantments` method, and all `SuperpositionHandler` reflection.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the Step 2 command again.

Expected: PASS with one test executed and no merge-oriented compatibility code remaining.

- [ ] **Step 5: Commit the focused helper change**

```powershell
git add -- src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java src/main/java/com/apotheosis_artifice/compat/EnigmaticLegacyCompat.java
git commit -m "refactor: model enchanter pearl as treasure access"
```

---

### Task 2: Apply treasure access to every player-operated menu

**Files:**
- Modify: `src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java`
- Modify: `src/main/java/com/apotheosis_artifice/mixin/ApothEnchantmentMenuMixin.java`
- Modify: `src/main/java/com/apotheosis_artifice/enchant/RavenEnchantMenu.java`
- Modify: `src/main/java/com/apotheosis_artifice/enchant/MechanicalRavenEnchantMenu.java`

**Interfaces:**
- Consumes: `EnigmaticLegacyCompat.enableTreasure(TableStats, Player)` from Task 1.
- Produces: corrected `TableStats` for ordinary and Raven-derived player menus, synchronized to the client.

- [ ] **Step 1: Add failing menu integration assertions**

Replace `ordinaryApotheosisEnchantingTableHandlesEnchanterPearlServerClick` and strengthen the mechanical test with these contracts:

```java
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
void mechanicalAutomaticEnchantingNeverUsesPlayerPearlCompatibility() throws IOException {
    String tile = read("enchant", "MechanicalRavenEnchantTile.java");

    assertFalse(tile.contains("EnigmaticLegacyCompat"));
    assertFalse(tile.contains("enchanter_pearl"));
    assertFalse(tile.substring(tile.indexOf("doEnchant")).contains("enableTreasure"));
}
```

- [ ] **Step 2: Run the integration tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest.playerOperatedMenusApplyTreasureStatsWithoutReplacingEnchanting --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest.mechanicalAutomaticEnchantingNeverUsesPlayerPearlCompatibility
```

Expected: the player-operated menu test FAILS because menus still replace click behavior and fuel count; the automatic test PASSES.

- [ ] **Step 3: Change the ordinary Apotheosis mixin to post-process gathered stats**

Preserve all Easy Magic fields, constructor hooks, inventory binding, `removed`, and reroll button handling. Add a unique player reference initialized in both constructor injections:

```java
@Unique private Player artifice$menuPlayer;

private void artifice$rememberPlayer(Inventory inventory) {
    this.artifice$menuPlayer = inventory.player;
}
```

Call `artifice$rememberPlayer(inventory)` at the start of both existing constructor injections. Replace the pearl branch in `clickMenuButton` so only Easy Magic ID 4 remains:

```java
@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
private void artifice$handleEasyMagicReroll(Player player, int data, CallbackInfoReturnable<Boolean> cir) {
    if (data == 4) cir.setReturnValue(EasyMagicCompat.tryReroll((ApothEnchantmentMenu) (Object) this, player));
}
```

Inject after ordinary stat gathering, transform only on the server, and send the corrected stats because Apotheosis already sent its original message inside `gatherStats`:

```java
@Inject(method = "gatherStats", at = @At("TAIL"), remap = false)
private void artifice$enableEnchanterPearlTreasure(CallbackInfo ci) {
    if (this.artifice$menuPlayer == null || this.artifice$menuPlayer.level().isClientSide) return;
    ApothEnchantmentMenu.TableStats updated = EnigmaticLegacyCompat.enableTreasure(this.stats, this.artifice$menuPlayer);
    if (updated == this.stats) return;
    this.stats = updated;
    PacketDistro.sendTo(Apotheosis.CHANNEL, new StatsMessage(this.stats), this.artifice$menuPlayer);
}
```

Remove imports used only by the old copied enchanting flow. Add imports for `@Unique`, `Apotheosis`, `StatsMessage`, and `PacketDistro`.

- [ ] **Step 4: Restore Raven menus to the native enchanting flow**

In `RavenEnchantMenu`, remove the pearl-aware `getGoldCount()` override and the copied pearl branch in `clickMenuButton`. Retain Easy Magic reroll handling:

```java
@Override
public boolean clickMenuButton(Player player, int id) {
    if (id == 4) return EasyMagicCompat.tryReroll(this, player);
    return super.clickMenuButton(player, id);
}
```

Apply the helper before the existing stats packet in `gatherStats()`:

```java
this.stats = EnigmaticLegacyCompat.enableTreasure(new TableStats(
    this.ravenStats.eterna(), this.ravenStats.quanta(), this.ravenStats.arcana(),
    blockStats.rectification(), blockStats.clues(), blockStats.blacklist(), blockStats.treasure()), this.player);
PacketDistro.sendTo(Apotheosis.CHANNEL, new StatsMessage(this.stats), this.player);
```

Remove imports used only by the copied enchanting flow. Keep imports and behavior used by Easy Magic, slider synchronization, JEI transfer, and Raven statistics.

- [ ] **Step 5: Remove free fuel handling from Mechanical Raven manual menus**

In `MechanicalRavenEnchantMenu`, delete only the `EnigmaticLegacyCompat` import and this early return:

```java
if (EnigmaticLegacyCompat.isEnchanterPearlActive(this.player)) return 64;
```

Keep the rest of `getGoldCount()` because it reads the mechanical table's actual fuel inventory.

- [ ] **Step 6: Run focused tests and verify GREEN**

Run the Step 2 command again.

Expected: both tests PASS. The ordinary menu synchronizes corrected stats, Raven-derived manual menus use the shared helper, and the automatic tile remains unchanged.

- [ ] **Step 7: Run all compatibility tests**

Run:

```powershell
.\gradlew.bat test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest
```

Expected: all tests in `EnchanterPearlCompatibilityTest` PASS, including existing SocketingRecipe and Easy Magic isolation checks.

- [ ] **Step 8: Compile the mod**

Run:

```powershell
.\gradlew.bat compileJava
```

Expected: `BUILD SUCCESSFUL`; Mixin annotations, imports, and Apotheosis 7.4.8 method signatures compile.

- [ ] **Step 9: Commit the player-menu integration**

```powershell
git add -- src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java src/main/java/com/apotheosis_artifice/mixin/ApothEnchantmentMenuMixin.java src/main/java/com/apotheosis_artifice/enchant/RavenEnchantMenu.java src/main/java/com/apotheosis_artifice/enchant/MechanicalRavenEnchantMenu.java
git commit -m "feat: enable treasure enchants with enchanter pearl"
```

---

### Task 3: Final regression verification

**Files:**
- Verify only: all files modified in Tasks 1 and 2.

**Interfaces:**
- Consumes: completed Enchanter's Pearl treasure compatibility.
- Produces: verification evidence with no additional production API.

- [ ] **Step 1: Confirm forbidden legacy paths are absent**

Run:

```powershell
rg -n "mergePearlEnchantments|mergeEnchantments|maybeApplyEternalBinding|EnchantmentHelper\.enchantItem" src/main/java/com/apotheosis_artifice/compat/EnigmaticLegacyCompat.java src/main/java/com/apotheosis_artifice/mixin/ApothEnchantmentMenuMixin.java src/main/java/com/apotheosis_artifice/enchant/RavenEnchantMenu.java
```

Expected: no matches.

- [ ] **Step 2: Confirm automatic enchanting remains isolated**

Run:

```powershell
rg -n "EnigmaticLegacyCompat|enchanter_pearl|enableTreasure" src/main/java/com/apotheosis_artifice/enchant/MechanicalRavenEnchantTile.java
```

Expected: no matches.

- [ ] **Step 3: Run the complete test suite**

Run:

```powershell
.\gradlew.bat test
```

Expected: `BUILD SUCCESSFUL` with all JUnit tests passing.

- [ ] **Step 4: Review the final diff boundary**

Run:

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors. Only the intended compatibility files plus pre-existing user-owned Easy Magic and SocketingRecipe changes are present; unrelated files are not staged by this implementation.
