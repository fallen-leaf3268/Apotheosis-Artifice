# 附魔师珍珠宝藏附魔兼容设计

## 目标

让 Enigmatic Legacy 的附魔师珍珠在按原模组规则对玩家生效时，为玩家操作的 Apotheosis 附魔菜单启用宝藏附魔能力。兼容只启用 `treasure` 标志，不模拟宝藏书架的其他属性。

## 适用范围

- 普通 Apotheosis 附魔台。
- 渡鸦附魔台。
- 机械渡鸦附魔台的玩家手动操作界面。
- 机械渡鸦附魔台的无人自动附魔不受影响。

## 激活条件

兼容通过可选反射调用附魔师珍珠自身的 `isPresent(player)`，完全沿用 Enigmatic Legacy 对珍珠是否启用的判定。仅把珍珠放入背包不构成独立的新判定规则。

## 数据流

1. 附魔菜单按 Apotheosis 原生流程收集附近书架统计。
2. 统计完成后检查当前菜单玩家的附魔师珍珠是否启用。
3. 若未启用，保留原始 `TableStats`。
4. 若已启用，以原始数据重新构造 `TableStats`，仅将 `treasure` 设置为 `true`。
5. 后续预览、线索生成和正式附魔继续使用 Apotheosis 原生流程。

重新构造统计时必须原样保留：

- Eterna
- Quanta
- Arcana
- Rectification
- Clues
- Blacklist

珍珠不会应用宝藏书架自带的 `Quanta -10` 或 `Arcana +10`。如果附近书架已经启用了宝藏附魔，布尔结果仍为 `true`，不会产生重复叠加。

## 兼容结构

`EnigmaticLegacyCompat` 仅负责安全判断附魔师珍珠是否对玩家生效。它不再生成额外附魔、不再合并附魔结果、不修改青金石数量，也不调用 Enigmatic Legacy 的附魔合并或永恒绑定处理。

普通 Apotheosis 菜单通过统计收集完成后的注入应用兼容。渡鸦菜单复用同一统计后处理。机械渡鸦无人自动流程不经过玩家菜单后处理，因此保持不变。

## 故障处理

- 未安装 Enigmatic Legacy 时返回未启用，不直接加载其类。
- 找不到珍珠物品或反射方法时回退到 Apotheosis 原生行为。
- 反射初始化失败时记录兼容警告，之后不改变附魔统计。
- 珍珠状态查询失败时记录警告，并为本次查询返回未启用。

## 清理现有实现

移除当前兼容中下列行为：

- 把青金石数量强制视为 64。
- 覆写附魔按钮点击流程。
- 生成第二组原版附魔并合并结果。
- 调用 `mergeEnchantments`。
- 调用 `maybeApplyEternalBinding`。

与本设计无关的 SocketingRecipe 映射修复保持独立，不作为珍珠兼容逻辑的一部分。

## 验证标准

- 未安装 Enigmatic Legacy 时项目可加载，附魔行为不变。
- 珍珠未启用时所有 `TableStats` 字段不变。
- 珍珠启用且原统计 `treasure=false` 时，仅该字段变为 `true`。
- 珍珠启用且原统计 `treasure=true` 时统计保持等价。
- Quanta 和 Arcana 在兼容前后数值一致。
- 普通 Apotheosis、渡鸦和机械渡鸦手动菜单均应用兼容。
- 机械渡鸦无人自动附魔不查询或应用珍珠兼容。
- 不再存在手工合并附魔结果的代码路径。
