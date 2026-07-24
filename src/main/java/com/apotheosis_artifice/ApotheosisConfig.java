package com.apotheosis_artifice;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ApotheosisConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.BooleanValue CLEAR_SOCKETS_ON_RARITY_CHANGE;
    public static ForgeConfigSpec.BooleanValue USE_APOTH_ARMOR_FORMULA;
    public static ForgeConfigSpec.BooleanValue USE_APOTH_PROT_FORMULA;
    public static ForgeConfigSpec.BooleanValue ENABLE_CURIOS_LOOT_RARITY;
    public static ForgeConfigSpec.BooleanValue USE_BETTERCOMBAT_HEAVY_OVERRIDE;
    public static ForgeConfigSpec.IntValue MAX_ETERNA;
    public static ForgeConfigSpec.IntValue MAX_QUANTA;
    public static ForgeConfigSpec.IntValue MAX_ARCANA;
    public static ForgeConfigSpec.IntValue MAX_ENCHANTMENTS;
    public static ForgeConfigSpec.IntValue EXTRA_LEVEL_CAP;
    public static ForgeConfigSpec.IntValue EXTRA_LEVEL_POWER_PER_LEVEL;

    static {
        BUILDER.push("Reforging");
        CLEAR_SOCKETS_ON_RARITY_CHANGE = BUILDER
            .comment("用不同品质材料重铸时，是否清空镶孔并重新生成（设为 true 则清空原镶孔并按新品质重新生成；设为 false 则保持原版 Apotheosis 行为，总是保留最高品质的镶孔）")
            .define("clear_sockets_on_rarity_change", false);
        BUILDER.pop();

        BUILDER.push("Combat_Formulas");
        USE_APOTH_ARMOR_FORMULA = BUILDER
            .comment("是否启用 Apothic Attributes 的护甲公式修改。")
            .define("use_apoth_armor_formula", true);
        USE_APOTH_PROT_FORMULA = BUILDER
            .comment("是否启用 Apothic Attributes 的保护公式修改。")
            .define("use_apoth_prot_formula", true);
        BUILDER.pop();

        BUILDER.push("Loot_Rarity");
        ENABLE_CURIOS_LOOT_RARITY = BUILDER
            .comment("是否允许饰品（Curios）物品在战利品中生成重铸稀有度。")
            .define("enable_curios_loot_rarity", true);
        BUILDER.pop();

        BUILDER.push("BetterCombat");
        USE_BETTERCOMBAT_HEAVY_OVERRIDE = BUILDER
            .comment("是否启用 Better Combat 联动功能，开启后，双手武器将被判定为重型武器。")
            .define("use_bettercombat_heavy_override", false);
        BUILDER.pop();

        BUILDER.push("Enchanting");
        MAX_ETERNA = BUILDER
            .comment("附魔台 Eterna（位阶）的最大值。")
            .defineInRange("max_eterna", 50, 1, 1000);
        MAX_QUANTA = BUILDER
            .comment("附魔台 Quanta（量子化）的最大值。")
            .defineInRange("max_quanta", 100, 1, 1000);
        MAX_ARCANA = BUILDER
            .comment("附魔台 Arcana（阿卡那）的最大值。")
            .defineInRange("max_arcana", 100, 1, 1000);
        MAX_ENCHANTMENTS = BUILDER
            .comment("单次附魔产出的魔咒数量上限（含保底与随机追加）。")
            .defineInRange("max_enchantments", 15, 1, 127);
        EXTRA_LEVEL_CAP = BUILDER
            .comment("加入 extra_level 标签的附魔的等级硬上限（原始最高等级为 1 的附魔不受影响）。")
            .defineInRange("extra_level_cap", 127, 1, 127);
        EXTRA_LEVEL_POWER_PER_LEVEL = BUILDER
            .comment("加入 extra_level 标签的附魔，超过原始最高等级后每提升 1 级所需的额外威力（线性）。数值越小等级涨得越快。")
            .defineInRange("extra_level_power_per_level", 50, 1, 10000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
        org.slf4j.LoggerFactory.getLogger(ApotheosisConfig.class).info("ApotheosisConfig initialized: max_eterna={}, max_quanta={}, max_arcana={}", MAX_ETERNA.get(), MAX_QUANTA.get(), MAX_ARCANA.get());
    }
}
