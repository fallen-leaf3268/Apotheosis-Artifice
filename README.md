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

宝石柜 GUI 材质 `src/main/resources/assets/apotheosis_artifice/textures/gui/gem_case.png` 已使用 Blockbench 重绘。307×256 图集采用深棕主体、暖金边框和搜索纸面，物品格使用中性深灰底色；宝石输入栏与装备过滤栏采用带对称护角的方形边框。过滤栏仅在空槽显示小型胸甲图标，放入装备后隐藏占位图标，并按适用装备筛选宝石。无库存宝石以较高可见度显示，并标注灰色数量“0”。

搜索框与六列槽位对齐，文字只绘制一次且关闭阴影；滑块与搜索边框之间保留间距。五个锻造按钮保留宝石图案并采用竖直锤柄，锻造与翻页按钮均从图集读取正常、悬停、禁用三态。翻页按钮为 9×16 像素，与锻造按钮等高，分别位于输出栏两端；仅在所选宝石支持第一页以外的更高等级时显示并允许翻页，判断不依赖玩家已有的宝石库存。主背景不再预绘翻页箭头，隐藏控件后不会残留按钮图案。

搜索框处于可编辑且聚焦状态时，键盘输入优先交给输入框处理；输入 `E` 或绑定为背包快捷键的其他字符会继续编辑搜索文字，避免触发容器关闭、丢弃物品或数字键换位。保留 `Esc` 关闭、`Tab` 切换焦点、文字编辑快捷键，以及搜索框失去焦点后的正常容器键盘操作。

普通宝石柜与法贝热宝石柜分别使用 `textures/blocks/gem_case.png`、`textures/blocks/ender_gem_case.png`，已完成 Blockbench 柜体重绘与图集整理。普通柜采用下界合金风格的深灰紫金属与紫色宝石，法贝热柜采用紫菘、末地风格的淡紫柜体与青绿宝石；保留宝石、容器和锻造元素，底面使用独立的对称图案。玻璃下方展示托盘为均匀底色及细边框，不使用格栅纹。两张贴图均精简为 64×32，方块几何保留，模型 UV 与纹理引用同步更新；两柜各自引用本体贴图，原玻璃区域未重绘，仍需核查其来源与授权。

渡鸦附魔台与机械渡鸦附魔台已接入本机 Blockbench 绘制的 v9 本体和书本材质。台体贴图保持 16×16，以旧版的灰色四角承托、红布和金色布边分区为构图参考，重新绘制像素图案及配色；侧面垂布收窄，底座以深色对称羽毛表现渡鸦主题。机械款增加铁灰角件、竖护条与红石短线。六张贴图包括各自的顶面、侧面、共用底面和 64×32 魔法书，现有方块几何与 UV 保留。两种附魔台的世界书本使用自绘图片，保留原版开合、翻页、转向和悬浮动画；原版附魔台继续使用原版书纹理。

替换前的 GUI 图片与 Apotheosis 1.21 的对应图片像素完全一致；该上游分支以 `LICENSE_ASSETS` 单独声明资源保留所有权利。新 GUI 图依据界面代码的布局和用户提供的配色参考制作，未沿用旧图的美术像素。未重绘的材质区域、模型和其余 GUI 图片仍需逐项确认来源和适用许可，不能据本项目代码许可推定可再分发。

上游资源许可：[Apotheosis LICENSE_ASSETS](https://github.com/Shadows-of-Fire/Apotheosis/blob/1.21/LICENSE_ASSETS)。

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
