# EasyMagic Reroll UI Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 EasyMagic 刷新按钮移到神化附魔界面外部并补齐中文提示，同时保持现有刷新协议和成本校验不变。

**Architecture:** 继续由 `ApothEnchantScreenEasyMagicMixin` 负责绘制、命中检测和 `id = 4` 点击发送，只把统一横坐标改为 `leftPos - 40`。继续使用 EasyMagic 原生键 `container.enchant.reroll`，在本模组 `zh_cn` 中补充该键，不重复定义英文。

**Tech Stack:** Java 17、Forge 1.20.1、Sponge Mixin、JUnit 5、Gradle 8.8。

## Global Constraints

- 按钮尺寸固定为 `38 × 27`，纵坐标固定为 `topPos + 16`。
- 普通神化、渡鸦和机械渡鸦界面统一使用 `leftPos - 40`。
- 保持 EasyMagic `id = 4` 菜单协议、经验成本和催化剂成本不变。
- 使用 `container.enchant.reroll`；仅在 `zh_cn` 补充“刷新附魔选项”。
- 不修改珍珠条件、神化属性计算或机械自动附魔。
- 不自动部署 JAR 到 `mods`。

---

### Task 1: 锁定按钮布局和翻译资源

**Files:**
- Modify: `src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java`
- Modify: `src/main/java/com/apotheosis_artifice/mixin/client/ApothEnchantScreenEasyMagicMixin.java`
- Modify: `src/main/resources/assets/apotheosis_artifice/lang/zh_cn.json`

**Interfaces:**
- Consumes: `EasyMagicCompat.rerollEnchantments()`、`ApothEnchantmentMenu.clickMenuButton(Player, int)`、EasyMagic 纹理和 `container.enchant.reroll`。
- Produces: `artifice$rerollButtonX(): int` 始终返回 `leftPos - 40`；中文资源提供原生刷新键。

- [ ] **Step 1: 写失败回归测试**

在 `EnchanterPearlCompatibilityTest` 中加入：

```java
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
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```powershell
gradle test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest.easyMagicRerollButtonUsesExternalApotheosisLayoutAndNativeTranslationKey
```

Expected: FAIL，因为当前普通神化和渡鸦界面仍返回内部坐标，且 `zh_cn` 缺少刷新键。

- [ ] **Step 3: 实现最小布局和翻译修改**

将坐标方法改为：

```java
private int artifice$rerollButtonX() {
    return this.leftPos - 40;
}
```

删除不再使用的 `MechanicalRavenEnchantScreen` import，并在 `zh_cn.json` 顶层对象加入：

```json
"container.enchant.reroll": "刷新附魔选项"
```

- [ ] **Step 4: 运行目标测试并确认通过**

Run:

```powershell
gradle test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest
```

Expected: PASS，所有兼容回归测试通过。

- [ ] **Step 5: 运行完整验证**

Run:

```powershell
gradle clean test build
```

Expected: `BUILD SUCCESSFUL`，生成完成重混淆的 `build/libs/apotheosis_artifice-1.0.7.jar`。

- [ ] **Step 6: 提交实现**

```powershell
git add src/main/java/com/apotheosis_artifice/mixin/client/ApothEnchantScreenEasyMagicMixin.java src/main/resources/assets/apotheosis_artifice/lang/zh_cn.json src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java
git commit -m "fix: adapt EasyMagic reroll UI to Apotheosis"
```

### Task 2: 输出游戏内测试 JAR

**Files:**
- Read: `build/libs/apotheosis_artifice-1.0.7.jar`
- Output: `C:/Users/Lenovo/Desktop/Ai_Run/output/apotheosis_artifice-1.0.7-enchanter-pearl-reroll-ui-fix-test.jar`

**Interfaces:**
- Consumes: Task 1 完整构建产生的重混淆 JAR。
- Produces: 不自动部署、可由用户手动替换的测试 JAR 和 SHA-256。

- [ ] **Step 1: 复制并校验测试 JAR**

```powershell
Copy-Item build/libs/apotheosis_artifice-1.0.7.jar C:/Users/Lenovo/Desktop/Ai_Run/output/apotheosis_artifice-1.0.7-enchanter-pearl-reroll-ui-fix-test.jar
Get-FileHash -Algorithm SHA256 C:/Users/Lenovo/Desktop/Ai_Run/output/apotheosis_artifice-1.0.7-enchanter-pearl-reroll-ui-fix-test.jar
```

Expected: 输出文件存在、大小非零，并打印 SHA-256。
