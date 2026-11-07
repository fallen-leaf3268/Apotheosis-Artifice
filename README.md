# Apotheosis Artifice

Apotheosis Artifice 是 fallen-leaf3268 开发的非官方 Apotheosis 附属模组。它扩展饰品重铸、宝石存储、附魔台、便携回收和其他模组兼容功能。需要安装 Apotheosis；本项目不代表 Apotheosis 原作者。

## 版本与依赖

当前版本为 1.1.1，目标环境为 Minecraft 1.20.1 / Forge 47.x。

构建使用 Apotheosis 7.4.8、Placebo 8.6.2、Apothic Attributes 1.3.5 和 Curios 5.14.1+1.20.1。运行时还需要这些前置模组各自要求的依赖。前置模组通过各自的官方渠道安装。

## 代码许可

- 本项目原创代码采用 GPL-3.0-or-later，完整 GPLv3 条款见 `COPYING`。
- 移植或改编的 Apotheosis、Apothic Attributes、Apothic Spawners、Apothic Enchanting 代码保留各自的 MIT 许可及版权声明，见 `LICENSE`。
- 代码许可不构成对第三方美术资源的授权，也不代表发布平台已批准项目。

## 内容

- 饰品重铸：支持饰品重铸，并内置了一些词条和宝石。
- 神化重铸台：能根据当前最高稀有度自动调节重铸等级。
- 宝石柜：集中存储、搜索、筛选和批量升级宝石。
- 宝石数据包补充：扩展已有宝石的适用装备，或为未覆盖的装备类别追加能力。
- 附魔台：自由调节位阶、量子化和阿卡那；机械版支持红石控制的自动附魔与灌注。
- 书架：天青书架减少青金石消耗，知识满溢书架减少经验消耗，属性支持数据包调整。
- 便携工具：便携回收台随身分解装备；万能神化护符支持自动收纳宝石、附魔书和自动回收。
- 符石：至高、恶意符石强化词条；清除符石移除重铸属性；圣洁布匹清除诅咒附魔。
- 刷怪笼扩展：新增初始生命值、燃烧、回响属性，以及四种预设升级符文。
- 末影拴绳：捕获和释放生物，秘法末影拴绳可绑定刷怪笼生物种类。
- JEI 联动：提供 JEI 信息查询与配方填充。

## 1.1.1 更新内容（相对 1.1.0）

- 饰品神化支持整体停用，已有数据保留，重新开启后恢复。
- 修复 JEI 词条显示与重铸材料结算，改善 Easy Magic 兼容。

## 宝石数据包补充

在数据包的 `data/<命名空间>/apotheosis_artifice/gem_additions/<文件名>.json` 中编写补充，`target` 填已有宝石 ID。原有独立宝石的数据包格式保持不变。

例如，让守护者宝石的胸甲能力也适用于饰品：

```json
{
  "target": "apotheosis:core/guardian",
  "extend_bonuses": [
    { "match_category": "chestplate", "add_types": ["curio"] }
  ]
}
```

`match_category` 选中直接声明该类别的能力，新增类别沿用这项能力的数值；不会通过饰品类别回退查找。实际适用范围、宝石“可镶嵌于”列表和 JEI 会更新。此操作保留原有 `gem_class.key`，因此每条效果前的分组标题仍沿用原翻译。

也可以为守护者宝石尚未覆盖的靴子类别添加独立的生命值能力：

```json
{
  "target": "apotheosis:core/guardian",
  "add_bonuses": [
    {
      "type": "apotheosis:attribute",
      "gem_class": { "key": "boots", "types": ["boots"] },
      "attribute": "minecraft:generic.max_health",
      "operation": "ADDITION",
      "values": {
        "common": 1,
        "uncommon": 2,
        "rare": 3,
        "epic": 4,
        "mythic": 5,
        "ancient": 6
      }
    }
  ]
}
```

两种操作可写在同一个补充文件中。`add_bonuses` 沿用原生完整能力格式；目前不支持修改已有数值或补齐已有能力的品质数据。

- 同一路径采用高优先级数据包的文件；不同路径先处理高优先级数据包，同优先级按资源 ID 排序。
- 相同补充重复加载不会叠加。类别被其他能力占用或格式无效时，该补充文件整体不生效，日志会标出来源和原因。
- 支持 `conditions` 和 `forge:conditions`，目标宝石自身的加载条件也必须成立。仅在饰品神化开启时生效的补充，可添加 `"conditions": [{ "type": "apotheosis_artifice:curios_module" }]`。
- 使用 `/reload` 应用变更；删除补充文件后再次重载即可恢复。宝石原有数值与品质范围保持不变，JEI 随重载和服务端同步刷新。
