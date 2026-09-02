# Enchanter Pearl Lapis-Free Enchanting Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复未安装 Easy Magic 时附魔师珍珠不能在 Apotheosis 系列附魔台免青金石进行手动附魔的问题。

**Architecture:** 在现有 `ApothEnchantmentMenuMixin` 中用 `@ModifyVariable` 替换 `clickMenuButton` 捕获的燃料局部变量。珍珠启用时只向 Apotheosis 原流程提供数量为 64 的虚拟副本，真实燃料槽不参与检查或扣除；其他情况返回原物品栈。

**Tech Stack:** Java 17、Forge 1.20.1、Sponge Mixin、JUnit 5、Gradle 8.8

## Global Constraints

- Enigmatic Legacy 和 Easy Magic 均保持可选兼容，不新增编译或运行依赖。
- Easy Magic 安装状态不得参与手动附魔免青金石判定。
- 只影响按钮编号 `0..2` 的手动附魔；机械自动附魔保持原状。
- 保留 Apotheosis 原附魔流程、经验费用、配方、随机种子、统计、进度与事件。
- 输出 JAR 只放入 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 mods。

---

### Task 1: 用虚拟燃料副本绕过 Apotheosis 的青金石检查与扣除

**Files:**
- Modify: `src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java`
- Modify: `src/main/java/com/apotheosis_artifice/mixin/ApothEnchantmentMenuMixin.java`

**Interfaces:**
- Consumes: `EnigmaticLegacyCompat.isEnchanterPearlActive(Player)`、`ApothEnchantmentMenu.clickMenuButton(Player, int)`。
- Produces: Mixin 处理器 `private ItemStack artifice$provideVirtualPearlFuel(ItemStack fuel, Player player, int id)`。

- [ ] **Step 1: 写失败回归测试**

在 `EnchanterPearlCompatibilityTest` 添加：

```java
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
```

- [ ] **Step 2: 运行测试并确认缺少虚拟燃料注入而失败**

Run:

```powershell
.\gradlew.bat test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest.enchanterPearlWaivesManualEnchantingLapisWithoutEasyMagic
```

Expected: `FAILED`，首个断言指向缺失的 `@ModifyVariable`。

- [ ] **Step 3: 实现最小修复**

在 `ApothEnchantmentMenuMixin` 导入 `ModifyVariable` 和 `Items`，并添加：

```java
@ModifyVariable(method = "clickMenuButton", at = @At("STORE"), ordinal = 1)
private ItemStack artifice$provideVirtualPearlFuel(ItemStack fuel, Player player, int id) {
    if (id < 0 || id >= 3 || !EnigmaticLegacyCompat.isEnchanterPearlActive(player)) return fuel;
    if (fuel.isEmpty()) return new ItemStack(Items.LAPIS_LAZULI, 64);
    ItemStack virtualFuel = fuel.copy();
    virtualFuel.setCount(64);
    return virtualFuel;
}
```

空槽返回新的虚拟青金石；非空槽返回数量放大的副本。数量固定为 64，确保 Apotheosis 缩减副本后不会进入清空真实燃料槽的分支。

- [ ] **Step 4: 运行定向测试与完整兼容测试**

Run:

```powershell
.\gradlew.bat test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest.enchanterPearlWaivesManualEnchantingLapisWithoutEasyMagic
.\gradlew.bat test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest
```

Expected: 两次均为 `BUILD SUCCESSFUL`。

- [ ] **Step 5: 完整构建与差异检查**

Run:

```powershell
.\gradlew.bat clean build
git diff --check
```

Expected: `BUILD SUCCESSFUL`，`git diff --check` 无错误。

- [ ] **Step 6: 提交修复**

```powershell
git add -- src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java src/main/java/com/apotheosis_artifice/mixin/ApothEnchantmentMenuMixin.java
git commit -m "fix: waive lapis for enchanter pearl enchanting"
```

- [ ] **Step 7: 输出测试 JAR 并核对哈希**

将 `build/libs/apotheosis_artifice-1.0.7.jar` 复制为：

```text
C:\Users\Lenovo\Desktop\Ai_Run\output\apotheosis_artifice-1.0.7-enchanter-pearl-lapis-fix-test.jar
```

计算源文件和输出文件的 SHA-256，Expected: 两者哈希一致；不写入 mods。

- [ ] **Step 8: 更新 GitHub 功能分支**

```powershell
git push origin codex/enchanter-pearl-treasure
```

Expected: 远程 `codex/enchanter-pearl-treasure` 指向本次修复提交。
