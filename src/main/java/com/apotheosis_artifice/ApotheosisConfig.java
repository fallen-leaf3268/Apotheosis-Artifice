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

    static {
        BUILDER.push("Reforging");
        CLEAR_SOCKETS_ON_RARITY_CHANGE = BUILDER
            .comment("用不同品质材料重铸时，是否清空镶孔并重新生成（设为 false 则保持原版 Apotheosis 行为，总是保留最高品质的镶孔）")
            .define("clear_sockets_on_rarity_change", true);
        BUILDER.pop();

        BUILDER.push("Combat Formulas");
        USE_APOTH_ARMOR_FORMULA = BUILDER
            .comment("是否启用 Apothic Attributes 的护甲公式修改。")
            .define("use_apoth_armor_formula", true);
        USE_APOTH_PROT_FORMULA = BUILDER
            .comment("是否启用 Apothic Attributes 的保护公式修改。")
            .define("use_apoth_prot_formula", true);
        BUILDER.pop();

        BUILDER.push("Loot Rarity");
        ENABLE_CURIOS_LOOT_RARITY = BUILDER
            .comment("是否允许饰品（Curios）物品在战利品中生成重铸稀有度。")
            .define("enable_curios_loot_rarity", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
