# Apotheosis Artifice

Apotheosis Artifice 是 fallen-leaf3268 开发的非官方 Apotheosis 附属模组。它扩展饰品重铸、宝石存储、附魔台、便携回收和其他模组兼容功能。需要安装 Apotheosis；本项目不代表 Apotheosis 原作者。

## 版本与依赖

当前版本为 1.0.8，目标环境为 Minecraft 1.20.1 / Forge 47.x。

构建使用 Apotheosis 7.4.8、Placebo 8.6.2、Apothic Attributes 1.3.5 和 Curios 5.14.1+1.20.1。运行时还需要这些前置模组各自要求的依赖。前置模组通过各自的官方渠道安装。

## 代码许可

- 本项目原创代码采用 GPL-3.0-or-later，完整 GPLv3 条款见 `COPYING`。
- 移植或改编的 Apotheosis、Apothic Attributes 代码保留各自的 MIT 许可及版权声明，见 `LICENSE`。
- 代码许可不构成对第三方美术资源的授权，也不代表发布平台已批准项目。

## 资源状态

本次仅补全许可声明与打包，材质、模型和 GUI 图片尚未完成授权整改。

已确认 `src/main/resources/assets/apotheosis_artifice/textures/gui/gem_case.png` 与 Apotheosis 1.21 的对应图片像素完全一致；该上游分支以 `LICENSE_ASSETS` 单独声明资源保留所有权利。此图片需要取得明确许可或替换。其余资源需要逐项确认来源和适用许可，不能据本项目代码许可推定可再分发。

上游资源许可：[Apotheosis LICENSE_ASSETS](https://github.com/Shadows-of-Fire/Apotheosis/blob/1.21/LICENSE_ASSETS)。

## 源码与构建

源码仓库：[fallen-leaf3268/Apotheosis-Artifice](https://github.com/fallen-leaf3268/Apotheosis-Artifice)。

使用 Java 17。下列命令适用于解压完整源码 ZIP 后的工程；ZIP 包含 Gradle wrapper JAR。在 Windows 工程根目录执行：

```powershell
.\gradlew.bat build sourceDistribution
```

其他系统可执行 `bash gradlew build sourceDistribution`。

如果使用 Git 克隆仓库，当前 `.gitignore` 排除了 wrapper JAR，需先在本机安装 Gradle 8.8，然后在工程根目录执行以下命令生成 wrapper，再运行上面的构建命令：

```powershell
gradle wrapper --gradle-version 8.8 --distribution-type bin
```

JAR 生成于 `build/libs/`，源码 ZIP 生成于 `build/distributions/`。源码 ZIP 包含当前工作副本的代码、资源、测试、构建脚本、Gradle wrapper 和许可文件；不包含 Git 历史、本地缓存或前置模组 JAR。

发布新的二进制包时，应同时提供本次构建对应的源码 ZIP，或提供包含全部变更的公开提交链接。仅有相同的版本号不能证明源代码与二进制包对应，尤其是重复使用 1.0.8 版本号时。当前本地修改未经推送前，不应将旧 `v1.0.8` 标签当成本次构建的完整源码。

资源授权整改完成后，再申请平台复审。构建成功不代表已经具备全部资源的分发授权。
