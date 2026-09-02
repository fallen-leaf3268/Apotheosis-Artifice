# EasyMagic Refresh Button Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the EasyMagic reroll button a seamless left-side tab, persist mechanical-raven rerolls, and waive ordinary lapis reroll cost while Enchanter's Pearl is active.

**Architecture:** Keep the existing client mixin and EasyMagic texture dependency, but draw the button before the Apotheosis background so a 2-pixel overlap hides its right border. Centralize player-dependent catalyst cost in `EasyMagicCompat`, and let `MechanicalRavenEnchantMenu` persist a successful reroll seed to its tile before the next broadcast.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, Sponge Mixin, JUnit 5, Gradle 8.8

## Global Constraints

- The button is 38×27 pixels at `leftPos - 36`, `topPos + 16`, with its rightmost 2 pixels covered by the main GUI.
- Reuse `easymagic:textures/gui/container/enchanting_table_reroll.png`; add no texture files.
- Keep menu button id `4`, tooltip behavior, sound, translation key, server logic, and dedicated catalyst slot rendering unchanged.
- Do not change automatic enchanting, treasure-enchant activation, eterna, quanta, or arcana behavior.
- Enchanter's Pearl waives only ordinary lapis catalyst cost; experience and dedicated reroll catalysts remain configured costs.
- Put the test JAR in `C:\Users\Lenovo\Desktop\Ai_Run\output`; do not deploy it to `mods`.

---

### Task 1: Restore the Native Reroll Button Layers

**Files:**
- Modify: `src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java:179-191`
- Modify: `src/main/java/com/apotheosis_artifice/mixin/client/ApothEnchantScreenEasyMagicMixin.java:36-84`

**Interfaces:**
- Consumes: `EasyMagicCompat.canUseReroll(ApothEnchantmentMenu)`, `rerollExperienceCost()`, `rerollCatalystCost()`, `getTotalExperience(Player)`, and `getRerollCatalystCount(ApothEnchantmentMenu)`.
- Produces: private client-only helpers `artifice$renderRerollContents(...)`, `artifice$renderCostOrb(...)`, and `artifice$renderReadableText(...)`; no public API changes.

- [ ] **Step 1: Write the failing regression tests**

Change the existing coordinate assertion and add a separate native-layer test:

```java
assertTrue(screenMixin.contains("return this.leftPos - 36;"));
```

```java
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
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
$env:JAVA_HOME = 'D:\JAVA\JAVA_17'
$env:Path = 'D:\JAVA\JAVA_17\bin;' + $env:Path
.\gradlew.bat --no-daemon test --tests "com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest"
```

Expected: `BUILD FAILED`; the current implementation still uses `leftPos - 38` instead of the seamless-tab coordinate.

- [ ] **Step 3: Move the button flush against the GUI**

Change the shared coordinate helper so rendering, clicking, and tooltips move together:

```java
private int artifice$rerollButtonX() {
    return this.leftPos - 36;
}
```

- [ ] **Step 4: Invoke the native content renderer after the button background**

Keep the current background state calculation, then call the helper only when rerolling is usable:

```java
int experience = EasyMagicCompat.rerollExperienceCost();
int catalyst = EasyMagicCompat.rerollCatalystCost();
boolean missingResources = !this.minecraft.player.getAbilities().instabuild
    && (EasyMagicCompat.getTotalExperience(this.minecraft.player) < experience
        || EasyMagicCompat.getRerollCatalystCount(menu) < catalyst);
boolean hovered = mouseX > x && mouseX <= x + 38 && mouseY > y && mouseY <= y + 27;
graphics.blit(ARTIFICE_REROLL_TEXTURE, x, y, 0, !usable || missingResources ? 0 : hovered ? 54 : 27, 38, 27);
if (usable) this.artifice$renderRerollContents(graphics, x, y, missingResources, hovered, experience, catalyst);
```

- [ ] **Step 5: Add the EasyMagic-native icon and cost positioning**

Add the helper below the tooltip method:

```java
private void artifice$renderRerollContents(GuiGraphics graphics, int x, int y, boolean missingResources,
    boolean hovered, int experience, int catalyst) {
    int iconV = missingResources ? 0 : hovered ? 30 : 15;
    if (experience == 0 && catalyst == 0) {
        graphics.blit(ARTIFICE_REROLL_TEXTURE, x + 12, y + 6, 64, iconV, 15, 15);
        return;
    }
    graphics.blit(ARTIFICE_REROLL_TEXTURE, x + 3, y + 6, 64, iconV, 15, 15);
    int orbV = missingResources ? 39 : 0;
    if (experience > 0 && catalyst > 0) {
        this.artifice$renderCostOrb(graphics, x + (experience > 9 ? 17 : 20), y + 13, 38, orbV,
            experience, missingResources ? ChatFormatting.RED : ChatFormatting.GREEN);
        this.artifice$renderCostOrb(graphics, x + (catalyst > 9 ? 17 : 20), y + 1, 51, orbV,
            catalyst, missingResources ? ChatFormatting.RED : ChatFormatting.BLUE);
    } else if (experience > 0) {
        this.artifice$renderCostOrb(graphics, x + (experience > 9 ? 17 : 20), y + 7, 38, orbV,
            experience, missingResources ? ChatFormatting.RED : ChatFormatting.GREEN);
    } else if (catalyst > 0) {
        this.artifice$renderCostOrb(graphics, x + (catalyst > 9 ? 17 : 20), y + 7, 51, orbV,
            catalyst, missingResources ? ChatFormatting.RED : ChatFormatting.BLUE);
    }
}
```

- [ ] **Step 6: Add the cost-orb and outlined-number helpers**

```java
private void artifice$renderCostOrb(GuiGraphics graphics, int x, int y, int u, int v, int cost,
    ChatFormatting color) {
    graphics.blit(ARTIFICE_REROLL_TEXTURE, x, y, u, v + Math.min(2, cost / 5) * 13, 13, 13);
    this.artifice$renderReadableText(graphics, x + 8, y + 3, String.valueOf(cost), color.getColor());
}

private void artifice$renderReadableText(GuiGraphics graphics, int x, int y, String value, int color) {
    graphics.drawString(this.font, value, x - 1, y, 0, false);
    graphics.drawString(this.font, value, x + 1, y, 0, false);
    graphics.drawString(this.font, value, x, y - 1, 0, false);
    graphics.drawString(this.font, value, x, y + 1, 0, false);
    graphics.drawString(this.font, value, x, y, color, false);
}
```

- [ ] **Step 7: Run the focused test and verify GREEN**

Run the command from Step 2.

Expected: `BUILD SUCCESSFUL`; all `EnchanterPearlCompatibilityTest` methods pass.

- [ ] **Step 8: Commit the tested UI change**

```powershell
git add src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java src/main/java/com/apotheosis_artifice/mixin/client/ApothEnchantScreenEasyMagicMixin.java
git commit -m "fix: restore EasyMagic reroll button visuals"
```

### Task 2: Make the Button a Seamless Tab

**Files:**
- Modify: `src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java`
- Modify: `src/main/java/com/apotheosis_artifice/mixin/client/ApothEnchantScreenEasyMagicMixin.java`

**Interfaces:**
- Consumes: the existing `renderBg` mixin injection and dedicated catalyst slot texture.
- Produces: a HEAD injection for the tab and a TAIL injection for slot overlays.

- [ ] **Step 1: Add failing source-structure assertions**

```java
assertTrue(screenMixin.contains("@Inject(method = \"renderBg\", at = @At(\"HEAD\"))"));
assertTrue(screenMixin.contains("private void artifice$renderDedicatedCatalystSlot("));
assertTrue(screenMixin.contains("@Inject(method = \"renderBg\", at = @At(\"TAIL\"))"));
```

- [ ] **Step 2: Run the focused compatibility test and verify RED**

Run the Task 1 focused test command. Expected: FAIL because the button still renders at TAIL in the same method as the slot overlays.

- [ ] **Step 3: Split the render injections**

Change `artifice$renderRerollButton` to `@At("HEAD")`, remove the dedicated-slot block from it, and add:

```java
@Inject(method = "renderBg", at = @At("TAIL"))
private void artifice$renderDedicatedCatalystSlot(GuiGraphics graphics, float partialTick, int mouseX,
    int mouseY, CallbackInfo ci) {
    if (!EasyMagicCompat.isLoaded() || !EasyMagicCompat.rerollEnchantments()
        || !EasyMagicCompat.dedicatedRerollButton()) return;
    graphics.blit(ARTIFICE_ENCHANTING_TEXTURE, this.leftPos + 4, this.topPos + 46, 14, 46, 18, 18);
    graphics.blit(ARTIFICE_ENCHANTING_TEXTURE, this.leftPos + 22, this.topPos + 46, 34, 46, 18, 18);
    graphics.blit(ARTIFICE_REROLL_TEXTURE, this.leftPos + 40, this.topPos + 46, 0, 81, 18, 18);
}
```

- [ ] **Step 4: Run the focused compatibility test and verify GREEN**

Run the Task 1 focused test command. Expected: PASS.

### Task 3: Persist Mechanical Rerolls and Waive Pearl Lapis Cost

**Files:**
- Modify: `src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java`
- Modify: `src/main/java/com/apotheosis_artifice/compat/EasyMagicCompat.java`
- Modify: `src/main/java/com/apotheosis_artifice/mixin/client/ApothEnchantScreenEasyMagicMixin.java`
- Modify: `src/main/java/com/apotheosis_artifice/enchant/MechanicalRavenEnchantMenu.java`

**Interfaces:**
- Produces: `EasyMagicCompat.rerollCatalystCost(Player)` returning the effective player cost.
- Produces: `MechanicalRavenEnchantMenu.clickMenuButton(Player, int)` persisting button `4` seed changes.

- [ ] **Step 1: Add failing compatibility tests**

```java
assertTrue(compat.contains("int rerollCatalystCost(Player player)"));
assertTrue(compat.contains("!dedicatedRerollButton() && EnigmaticLegacyCompat.isEnchanterPearlActive(player)"));
assertTrue(compat.contains("int catalystCost = rerollCatalystCost(player);"));
assertTrue(screenMixin.contains("EasyMagicCompat.rerollCatalystCost(this.minecraft.player)"));
assertTrue(mechanicalMenu.contains("this.tile.setEnchantmentSeed(this.enchantmentSeed.get());"));
assertTrue(mechanicalMenu.contains("if (id == 4 && rerolled && !player.level().isClientSide"));
```

- [ ] **Step 2: Run the focused compatibility test and verify RED**

Run the Task 1 focused test command. Expected: FAIL because the effective cost overload and mechanical seed write-back are absent.

- [ ] **Step 3: Add the effective catalyst cost**

```java
public static int rerollCatalystCost(Player player) {
    int configuredCost = rerollCatalystCost();
    if (!dedicatedRerollButton() && EnigmaticLegacyCompat.isEnchanterPearlActive(player)) return 0;
    return configuredCost;
}
```

Use this overload in `tryReroll`, button rendering, and tooltip rendering. Leave `rerollExperienceCost()` unchanged.

- [ ] **Step 4: Persist the mechanical-raven seed**

```java
@Override
public boolean clickMenuButton(Player player, int id) {
    boolean rerolled = super.clickMenuButton(player, id);
    if (id == 4 && rerolled && !player.level().isClientSide && this.tile != null) {
        this.tile.setEnchantmentSeed(this.enchantmentSeed.get());
        this.tile.setChanged();
    }
    return rerolled;
}
```

- [ ] **Step 5: Run the focused test and verify GREEN**

Run the Task 1 focused test command. Expected: PASS.

- [ ] **Step 6: Commit the functional fixes**

```powershell
git add src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java src/main/java/com/apotheosis_artifice/compat/EasyMagicCompat.java src/main/java/com/apotheosis_artifice/mixin/client/ApothEnchantScreenEasyMagicMixin.java src/main/java/com/apotheosis_artifice/enchant/MechanicalRavenEnchantMenu.java
git commit -m "fix: persist mechanical rerolls with pearl cost compatibility"
```

### Task 4: Verify and Export the Test JAR

**Files:**
- Verify: all project tests and generated `build/libs/apotheosis_artifice-1.0.7.jar`
- Output: `C:\Users\Lenovo\Desktop\Ai_Run\output\apotheosis_artifice-1.0.7-enchanter-pearl-reroll-native-ui-test.jar`

**Interfaces:**
- Consumes: the committed Task 1 client mixin and regression tests.
- Produces: a standalone test JAR for manual in-game validation; no deployment side effects.

- [ ] **Step 1: Run a clean full build**

```powershell
$env:JAVA_HOME = 'D:\JAVA\JAVA_17'
$env:Path = 'D:\JAVA\JAVA_17\bin;' + $env:Path
.\gradlew.bat --no-daemon clean test build
```

Expected: `BUILD SUCCESSFUL` and the reobfuscated JAR exists in `build/libs`.

- [ ] **Step 2: Copy the verified JAR to the output directory**

```powershell
New-Item -ItemType Directory -Force -Path 'C:\Users\Lenovo\Desktop\Ai_Run\output' | Out-Null
Copy-Item -LiteralPath 'build\libs\apotheosis_artifice-1.0.7.jar' -Destination 'C:\Users\Lenovo\Desktop\Ai_Run\output\apotheosis_artifice-1.0.7-enchanter-pearl-reroll-native-ui-test.jar' -Force
```

Expected: the destination file exists. Do not copy it into any `mods` directory.

- [ ] **Step 3: Record artifact size and SHA-256**

```powershell
Get-Item -LiteralPath 'C:\Users\Lenovo\Desktop\Ai_Run\output\apotheosis_artifice-1.0.7-enchanter-pearl-reroll-native-ui-test.jar' | Select-Object FullName,Length,LastWriteTime
Get-FileHash -Algorithm SHA256 -LiteralPath 'C:\Users\Lenovo\Desktop\Ai_Run\output\apotheosis_artifice-1.0.7-enchanter-pearl-reroll-native-ui-test.jar'
```

Expected: report the exact path, non-zero size, and hash to the user.

- [ ] **Step 4: Confirm repository cleanliness**

```powershell
git status --short
```

Expected: no uncommitted files.
