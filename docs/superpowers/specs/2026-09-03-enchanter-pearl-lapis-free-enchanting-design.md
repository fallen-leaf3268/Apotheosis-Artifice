# 附魔师珍珠免青金石附魔修复设计

## 目标

佩戴 Enigmatic Legacy 附魔师珍珠时，Apotheosis 系列附魔台的手动附魔不需要真实青金石，并且该能力不依赖 Easy Magic 是否安装。

## 根因

Enigmatic Legacy 通过 Mixin 修改原版 `EnchantmentMenu`。Apotheosis 重写了 `clickMenuButton`，在自己的实现中直接读取燃料槽数量，并在附魔成功后直接缩减该物品栈。因此，原模组对原版菜单的免青金石处理不能覆盖 Apotheosis 的实现。

当前项目只为珍珠补充了宝藏附魔权限，并在 Easy Magic 的刷新流程中免除刷新催化剂费用，没有处理 Apotheosis 手动附魔中的燃料检查和扣除。

## 方案

在现有 `ApothEnchantmentMenuMixin` 中使用 `@ModifyVariable`，捕获 `clickMenuButton` 中保存到局部变量的燃料物品栈。

- 仅当按钮编号为 `0..2` 且玩家佩戴附魔师珍珠时生效。
- 返回一个数量充足的燃料副本；燃料槽为空时返回新的虚拟青金石栈。
- Apotheosis 后续的数量检查与 `shrink` 只作用于该副本，真实槽位不被写入或消耗。
- 不取消或复制 Apotheosis 的附魔流程，保留其配方、经验、随机种子、统计、进度和事件行为。
- 创造模式继续由 Apotheosis 原逻辑处理。

`RavenEnchantMenu` 和 `MechanicalRavenEnchantMenu` 继承 Apotheosis 的按钮实现，因此同一注入覆盖普通神化附魔台、渡鸦附魔台和机械渡鸦的手动附魔。机械自动附魔不经过该按钮流程，保持原状。

## 可选兼容边界

- 只通过现有 `EnigmaticLegacyCompat.isEnchanterPearlActive(player)` 判断珍珠状态。
- 不引入 Enigmatic Legacy 或 Easy Magic 的编译、运行依赖。
- 未安装 Enigmatic Legacy、未佩戴珍珠或反射检查失败时，返回原燃料栈并保留 Apotheosis 原行为。
- Easy Magic 安装与否不参与免青金石判定。

## 验证

- 先添加回归测试，要求 Mixin 存在独立于 Easy Magic 的手动附魔燃料替换逻辑，并确认测试在实现前失败。
- 实现后运行定向回归测试和完整兼容测试类。
- 执行 `gradlew clean build` 和 `git diff --check`。
- 输出新的测试 JAR 到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 mods。
- 游戏内验证：移除 Easy Magic，佩戴附魔师珍珠，在三个附魔台中以零青金石进行手动附魔，确认附魔成功且没有生成或扣除真实青金石。
