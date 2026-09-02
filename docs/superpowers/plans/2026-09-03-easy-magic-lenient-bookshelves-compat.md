# Easy Magic Lenient Bookshelves Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Apotheosis 的书架通路判断在 Easy Magic 可选安装且 `lenientBookshelves` 开启时允许非完整碰撞方块传递附魔增幅。

**Architecture:** 扩展现有 `EasyMagicCompat`，继续通过 `ModList` 和反射读取配置，不引用 Easy Magic 类型。在现有 `ApothEnchantmentMenuMixin` 的静态通路判断入口仅对非完整碰撞形状提前返回 `true`，其他情况继续执行 Apotheosis 原方法。

**Tech Stack:** Java 17、Forge 1.20.1、Sponge Mixin、JUnit 5、Gradle 8.8

## Global Constraints

- Easy Magic 只能作为可选兼容，不能成为编译或运行硬依赖。
- 不导入 Easy Magic 的类，不在方法签名、字段或 Mixin 目标中引用 Easy Magic 类型。
- Easy Magic 缺失、配置关闭或反射失败时保留 Apotheosis 原判定。
- 完整碰撞方块不由兼容强制判定，保留 Apotheosis 传导方块标签支持。
- 不修改附魔数值、珍珠能力、刷新费用或 Easy Magic 本体。
- 输出 JAR 只放入 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 mods。

---

### Task 1: 可选配置读取与书架通路注入

**Files:**
- Modify: `src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java`
- Modify: `src/main/java/com/apotheosis_artifice/compat/EasyMagicCompat.java`
- Modify: `src/main/java/com/apotheosis_artifice/mixin/ApothEnchantmentMenuMixin.java`

**Interfaces:**
- Consumes: `EasyMagicCompat.isLoaded()`、现有反射配置读取器 `getBoolean(String, boolean)`、`ApothEnchantmentMenu.canReadStatsFrom(Level, BlockPos, BlockPos)`。
- Produces: `public static boolean EasyMagicCompat.lenientBookshelves()`；Mixin 处理器 `artifice$allowLenientBookshelfPath(...)`。

- [ ] **Step 1: 写失败回归测试**

在 `EnchanterPearlCompatibilityTest` 加入：

```java
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
```

- [ ] **Step 2: 运行测试并确认因接口缺失而失败**

Run:

```powershell
.\gradlew.bat test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest.easyMagicLenientBookshelvesRemainOptionalForApotheosisTables
```

Expected: `FAILED`，首个失败断言指向缺失的 `lenientBookshelves()`。

- [ ] **Step 3: 实现无硬依赖的配置读取**

在 `EasyMagicCompat` 添加：

```java
public static boolean lenientBookshelves() {
    if (!isLoaded()) return false;
    boolean enabled = getBoolean("lenientBookshelves", true);
    return available && enabled;
}
```

读取后再次检查 `available`，保证反射在本次调用中失败时返回 `false`。

- [ ] **Step 4: 注入 Apotheosis 通路判断**

在 `ApothEnchantmentMenuMixin` 添加所需 Minecraft 导入，并加入：

```java
@Inject(method = "canReadStatsFrom", at = @At("HEAD"), cancellable = true, remap = false)
private static void artifice$allowLenientBookshelfPath(Level level, BlockPos tablePos,
    BlockPos shelfOffset, CallbackInfoReturnable<Boolean> cir) {
    if (!EasyMagicCompat.lenientBookshelves()) return;
    BlockPos between = tablePos.offset(
        shelfOffset.getX() / 2, shelfOffset.getY(), shelfOffset.getZ() / 2);
    if (level.getBlockState(between).getCollisionShape(level, between) != Shapes.block()) {
        cir.setReturnValue(true);
    }
}
```

完整碰撞方块不设置返回值，让 Apotheosis 原方法继续检查传导标签。

- [ ] **Step 5: 运行定向测试和完整兼容测试**

Run:

```powershell
.\gradlew.bat test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest.easyMagicLenientBookshelvesRemainOptionalForApotheosisTables
.\gradlew.bat test --tests com.apotheosis_artifice.enchant.EnchanterPearlCompatibilityTest
```

Expected: 两次均显示 `BUILD SUCCESSFUL`。

- [ ] **Step 6: 完整构建与边界检查**

Run:

```powershell
.\gradlew.bat clean build
git diff --check
```

Expected: `BUILD SUCCESSFUL`，`git diff --check` 无错误。

- [ ] **Step 7: 提交实现**

```powershell
git add -- src/test/java/com/apotheosis_artifice/enchant/EnchanterPearlCompatibilityTest.java src/main/java/com/apotheosis_artifice/compat/EasyMagicCompat.java src/main/java/com/apotheosis_artifice/mixin/ApothEnchantmentMenuMixin.java
git commit -m "feat: support Easy Magic lenient bookshelves"
```

- [ ] **Step 8: 输出测试 JAR 并核对哈希**

将 `build/libs/apotheosis_artifice-1.0.7.jar` 复制为：

```text
C:\Users\Lenovo\Desktop\Ai_Run\output\apotheosis_artifice-1.0.7-easy-magic-lenient-bookshelves-test.jar
```

对源 JAR 与输出 JAR 计算 SHA-256，Expected: 两者哈希相同；不写入 mods。
