# Easy Magic 宽松书架通路兼容设计

## 目标

让 Apotheosis 的附魔统计收集遵循 Easy Magic 8.0.1 的 `lenientBookshelves` 配置：配置开启时，附魔台与书架之间的非完整碰撞方块不再阻断增幅；完整碰撞方块仍然阻断。

该行为覆盖普通 Apotheosis 附魔台、渡鸦附魔台和机械渡鸦附魔台。机械渡鸦的自动统计收集使用同一规则。

## 可选依赖约束

Easy Magic 只能作为可选兼容，不能成为编译或运行硬依赖：

- 不导入 Easy Magic 的类。
- 不在方法签名、字段或 Mixin 目标中引用 Easy Magic 类型。
- 先通过 `ModList` 判断模组是否存在。
- 通过现有 `EasyMagicCompat` 反射读取 `ServerConfig.lenientBookshelves`。
- Easy Magic 缺失、字段不可用或反射失败时，放弃兼容并保留 Apotheosis 原判定。

## 实现方案

在 `ApothEnchantmentMenu.canReadStatsFrom(Level, BlockPos, BlockPos)` 开头加入可取消的 Mixin 注入。

处理顺序：

1. Easy Magic 未加载或 `lenientBookshelves` 关闭时不返回结果，让 Apotheosis 原方法继续执行。
2. 按 Easy Magic 的算法计算附魔台和书架之间的中间位置。
3. 获取中间方块的碰撞形状。
4. 碰撞形状不是完整方块时返回 `true`。
5. 碰撞形状是完整方块时不强制返回 `false`，继续执行 Apotheosis 原方法，以保留其传导方块标签支持。

这样既复现 Easy Magic 的宽松规则，也不会破坏 Apotheosis 原有的特殊增幅传导方块。

## 配置接口

在 `EasyMagicCompat` 增加 `lenientBookshelves()`，复用现有反射配置读取器与失败降级逻辑。Easy Magic 的字段默认值为 `true`；读取后再次确认兼容仍可用，确保反射失败不会误启用宽松规则。

## 测试

回归测试验证：

- 配置通过反射读取，不出现 Easy Magic 类型导入。
- Mixin 注入 Apotheosis 的静态通路判断方法。
- 非完整碰撞形状返回可通路。
- 完整碰撞形状继续交给 Apotheosis 原判定。
- 现有珍珠、刷新、持久化预览与机械种子测试继续通过。

## 非目标

- 不修改书架提供的 Eterna、Quanta 或 Arcana 数值。
- 不改变完整方块的遮挡规则。
- 不新增配置项。
- 不修改 Easy Magic 本体或其 JAR。
