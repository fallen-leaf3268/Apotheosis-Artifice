# Apotheosis Artifice

Apotheosis Artifice 是 fallen-leaf3268 开发的非官方 Apotheosis 附属模组。它扩展饰品重铸、宝石存储、附魔台、便携回收和其他模组兼容功能。需要安装 Apotheosis；本项目不代表 Apotheosis 原作者。

## 版本与依赖

当前版本为 1.0.8，目标环境为 Minecraft 1.20.1 / Forge 47.x。

构建使用 Apotheosis 7.4.8、Placebo 8.6.2、Apothic Attributes 1.3.5 和 Curios 5.14.1+1.20.1。运行时还需要这些前置模组各自要求的依赖。前置模组通过各自的官方渠道安装。

## 代码许可

- 本项目原创代码采用 GPL-3.0-or-later，完整 GPLv3 条款见 `COPYING`。
- 移植或改编的 Apotheosis、Apothic Attributes 代码保留各自的 MIT 许可及版权声明，见 `LICENSE`。
- 代码许可不构成对第三方美术资源的授权，也不代表发布平台已批准项目。

## 更新内容（相对已发布的 1.0.8）

- 重绘宝石柜界面、普通宝石柜和法贝热宝石柜，优化栏位边框、按钮状态与物品辨识。
- 更新渡鸦附魔台、机械渡鸦附魔台及魔法书材质，修正世界中的书本材质显示。
- 修复宝石柜搜索文字重影，以及输入 `E` 键时误关闭界面的问题。
- 优化宝石输出栏翻页布局，按所选宝石支持的最高等级显示按钮，并修复隐藏后的按钮残留。
- JEI 词缀和宝石详情增加数据来源提示，改善多行说明显示。
- 补全许可声明、源码打包、Gradle 构建文件和回归测试。

## 源码与构建

源码仓库：[fallen-leaf3268/Apotheosis-Artifice](https://github.com/fallen-leaf3268/Apotheosis-Artifice)。

使用 Java 17。下列命令适用于解压完整源码 ZIP 后的工程；ZIP 包含 Gradle wrapper JAR。在 Windows 工程根目录执行：

```powershell
.\gradlew.bat build sourceDistribution
```

其他系统可执行 `bash gradlew build sourceDistribution`。

Git 仓库与完整源码 ZIP 均包含 Gradle 8.8 wrapper JAR，使用 Java 17 即可通过上述 wrapper 命令构建，无需预先安装 Gradle。

JAR 生成于 `build/libs/`，源码 ZIP 生成于 `build/distributions/`。源码 ZIP 包含当前工作副本的代码、资源、测试、构建脚本、Gradle wrapper 和许可文件；不包含 Git 历史、本地缓存或前置模组 JAR。

发布新的二进制包时，应同时提供本次构建对应的源码 ZIP，或提供包含全部变更的公开提交链接。仅有相同的版本号不能证明源代码与二进制包对应，尤其是重复使用 1.0.8 版本号时。当前本地修改未经推送前，不应将旧 `v1.0.8` 标签当成本次构建的完整源码。

资源授权整改完成后，再申请平台复审。构建成功不代表已经具备全部资源的分发授权。
