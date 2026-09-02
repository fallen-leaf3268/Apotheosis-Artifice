# EasyMagic Refresh Button Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the EasyMagic reroll button sit flush against the left side of the Apotheosis enchanting GUI and restore EasyMagic's native icon and cost layers.

**Architecture:** Keep the existing client mixin and EasyMagic texture dependency. Move only the shared button X coordinate, then add focused private rendering helpers that mirror EasyMagic 8.0.1's native layer order while continuing to read all costs and availability from `EasyMagicCompat`.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, Sponge Mixin, JUnit 5, Gradle 8.8

## Global Constraints

- The button is 38×27 pixels at `leftPos - 38`, `topPos + 16`.
- Reuse `easymagic:textures/gui/container/enchanting_table_reroll.png`; add no texture files.
- Keep menu button id `4`, tooltip behavior, sound, translation key, server logic, and dedicated catalyst slot rendering unchanged.
- Do not change automatic enchanting, treasure-enchant activation, eterna, quanta, or arcana behavior.
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
assertTrue(screenMixin.contains("return this.leftPos - 38;"));
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

Expected: `BUILD FAILED`; the coordinate assertion still finds `leftPos - 40`, and the native rendering helpers are absent.

- [ ] **Step 3: Move the button flush against the GUI**

Change the shared coordinate helper so rendering, clicking, and tooltips move together:

```java
private int artifice$rerollButtonX() {
    return this.leftPos - 38;
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

### Task 2: Verify and Export the Test JAR

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
