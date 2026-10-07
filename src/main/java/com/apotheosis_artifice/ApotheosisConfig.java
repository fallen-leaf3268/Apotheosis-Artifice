package com.apotheosis_artifice;

import java.util.List;
import java.util.Optional;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.EnchModule;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ApotheosisConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;
    private static final MigratingConfigSpec REGISTERED_SPEC;
    private static volatile EnchantingConfigPacket synchronizedEnchantingConfig;
    private static volatile ReforgingConfigPacket synchronizedReforgingConfig;
    private static volatile ReforgingConfigPacket activeServerReforgingConfig;
    private static final TagKey<Enchantment> ARTIFICE_EXTRA_LEVEL = TagKey.create(Registries.ENCHANTMENT,
        new ResourceLocation(ApotheosisArtificeMod.MODID, "extra_level"));
    private static final ThreadLocal<Boolean> nativeEnchantmentDefaults = new ThreadLocal<>();

    public static ForgeConfigSpec.BooleanValue ENABLE_CURIOS_REFORGING;
    public static ForgeConfigSpec.BooleanValue CLEAR_SOCKETS_ON_RARITY_CHANGE;
    public static ForgeConfigSpec.BooleanValue ENABLE_CURIOS_LOOT_RARITY;

    public static ForgeConfigSpec.IntValue MAX_ETERNA;
    public static ForgeConfigSpec.IntValue MAX_QUANTA;
    public static ForgeConfigSpec.IntValue MAX_ARCANA;
    public static ForgeConfigSpec.IntValue MAX_ENCHANTMENTS;
    public static ForgeConfigSpec.IntValue MECHANICAL_ENCHANT_INTERVAL;
    public static ForgeConfigSpec.IntValue EXTRA_LEVEL_CAP;
    public static ForgeConfigSpec.IntValue EXTRA_LEVEL_POWER_PER_LEVEL;

    public static ForgeConfigSpec.BooleanValue USE_APOTH_ARMOR_FORMULA;
    public static ForgeConfigSpec.BooleanValue USE_APOTH_PROT_FORMULA;
    public static ForgeConfigSpec.BooleanValue USE_BETTERCOMBAT_HEAVY_OVERRIDE;
    public static ForgeConfigSpec.EnumValue<LootPinataEffect> LOOT_PINATA_EFFECT;

    public enum LootPinataEffect { EXPLOSION, FIREWORKS, OFF }

    static {
        BUILDER.comment("重铸").push("Reforging");
        ENABLE_CURIOS_REFORGING = BUILDER
            .comment("启用饰品重铸；关闭时同时停用饰品重铸功能和词条战利品。")
            .worldRestart()
            .define("enable_curios_reforging", true);
        CLEAR_SOCKETS_ON_RARITY_CHANGE = BUILDER
            .comment("品质改变时重置镶孔数量；关闭则采用神化原有规则。")
            .define("clear_sockets_on_rarity_change", false);
        ENABLE_CURIOS_LOOT_RARITY = BUILDER
            .comment("生成带词条的饰品战利品，关闭本项不影响手动重铸。")
            .define("enable_curios_loot_rarity", true);
        BUILDER.pop();

        BUILDER.comment("附魔").push("Enchanting");
        MAX_ETERNA = BUILDER
            .comment("位阶")
            .defineInRange("max_eterna", 50, 1, 1000);
        MAX_QUANTA = BUILDER
            .comment("量子化。")
            .defineInRange("max_quanta", 100, 1, 1000);
        MAX_ARCANA = BUILDER
            .comment("阿卡那")
            .defineInRange("max_arcana", 100, 1, 1000);
        MAX_ENCHANTMENTS = BUILDER
            .comment("单次附魔数量上限。")
            .defineInRange("max_enchantments", 15, 1, 127);
        MECHANICAL_ENCHANT_INTERVAL = BUILDER
            .comment("机械渡鸦自动附魔间隔。")
            .defineInRange("mechanical_enchant_interval", 20, 1, Integer.MAX_VALUE);
        EXTRA_LEVEL_CAP = BUILDER
            .comment("apotheosis_artifice:extra_level 标签内附魔的等级上限。")
            .defineInRange("extra_level_cap", 127, 1, 127);
        EXTRA_LEVEL_POWER_PER_LEVEL = BUILDER
            .comment("提升的附魔等级成长曲线。")
            .defineInRange("extra_level_power_per_level", 15, 1, 10000);
        BUILDER.pop();

        BUILDER.comment("杂项").push("Combat_Formulas");
        USE_APOTH_ARMOR_FORMULA = BUILDER
            .comment("是否启用神化护甲公式；关闭时使用原版公式。")
            .define("use_apoth_armor_formula", true);
        USE_APOTH_PROT_FORMULA = BUILDER
            .comment("是否启用神化保护附魔公式；关闭时使用原版公式。")
            .define("use_apoth_prot_formula", true);
        USE_BETTERCOMBAT_HEAVY_OVERRIDE = BUILDER
            .comment("是否将 Better Combat 的双手剑武器归类为重型武器。")
            .define("use_bettercombat_heavy_override", false);
        LOOT_PINATA_EFFECT = BUILDER
            .comment("战利品大爆发特效修改：FIREWORKS 烟花、EXPLOSION 爆炸、OFF 关闭特效和音效。")
            .defineEnum("loot_pinata_effect", LootPinataEffect.EXPLOSION);
        BUILDER.pop();

        SPEC = BUILDER.build();
        REGISTERED_SPEC = new MigratingConfigSpec(SPEC);
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, REGISTERED_SPEC);
        MinecraftForge.EVENT_BUS.addListener(ApotheosisConfig::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(ApotheosisConfig::onServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(ApotheosisConfig::onServerStopped);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ApotheosisConfig::onConfigReload);
    }

    public static int getMaxEterna() {
        EnchantingConfigPacket remote = remoteEnchantingConfig();
        return remote == null ? MAX_ETERNA.get() : remote.maxEterna();
    }

    public static boolean isCuriosReforgingEnabled() {
        ReforgingConfigPacket snapshot = EffectiveSide.get().isClient()
            ? synchronizedReforgingConfig : activeServerReforgingConfig;
        return snapshot == null ? ENABLE_CURIOS_REFORGING.get() : snapshot.enableCuriosReforging();
    }

    public static boolean isCuriosLootRarityEnabled() {
        return isCuriosReforgingEnabled() && ENABLE_CURIOS_LOOT_RARITY.get();
    }

    public static LootPinataEffect getLootPinataEffect() {
        return Apotheosis.enableAdventure ? LOOT_PINATA_EFFECT.get() : LootPinataEffect.EXPLOSION;
    }

    private static void onServerAboutToStart(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
        activeServerReforgingConfig = new ReforgingConfigPacket(ENABLE_CURIOS_REFORGING.get());
    }

    private static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        activeServerReforgingConfig = null;
    }

    public static int getMaxQuanta() {
        EnchantingConfigPacket remote = remoteEnchantingConfig();
        return remote == null ? MAX_QUANTA.get() : remote.maxQuanta();
    }

    public static int getMaxArcana() {
        EnchantingConfigPacket remote = remoteEnchantingConfig();
        return remote == null ? MAX_ARCANA.get() : remote.maxArcana();
    }

    public static int getMaxEnchantments() {
        EnchantingConfigPacket remote = remoteEnchantingConfig();
        return remote == null ? MAX_ENCHANTMENTS.get() : remote.maxEnchantments();
    }

    public static int getMechanicalEnchantInterval() {
        return MECHANICAL_ENCHANT_INTERVAL.get();
    }

    public static int getExtraLevelCap() {
        EnchantingConfigPacket remote = remoteEnchantingConfig();
        return remote == null ? EXTRA_LEVEL_CAP.get() : remote.extraLevelCap();
    }

    public static int getExtraLevelPowerPerLevel() {
        EnchantingConfigPacket remote = remoteEnchantingConfig();
        return remote == null ? EXTRA_LEVEL_POWER_PER_LEVEL.get() : remote.extraLevelPowerPerLevel();
    }

    public static int limitTaggedEnchantmentLevel(Enchantment ench, int maximum) {
        if (!canExtendEnchantment(ench)) return maximum;
        int cap = Math.min(EnchModule.ENCH_HARD_CAPS.getOrDefault(ench, 127), getExtraLevelCap());
        return Math.max(maximum, Math.min(EnchModule.getDefaultMax(ench), cap));
    }

    public static boolean canExtendEnchantment(Enchantment ench) {
        if (!Apotheosis.enableEnch || isCalculatingNativeEnchantmentDefault() || ench.getMaxLevel() <= 1) return false;
        var tags = ForgeRegistries.ENCHANTMENTS.tags();
        return tags != null && tags.getTag(ARTIFICE_EXTRA_LEVEL).contains(ench)
            && getMaxEterna() > EnchantingStatRegistry.getAbsoluteMaxEterna();
    }

    public static float getEnchantmentPowerCap() {
        float nativeCap = EnchantingStatRegistry.getAbsoluteMaxEterna();
        if (!Apotheosis.enableEnch || isCalculatingNativeEnchantmentDefault()) return nativeCap;
        return Math.max(nativeCap, getMaxEterna());
    }

    public static int getNativeDefaultMax(Enchantment ench) {
        Boolean previous = nativeEnchantmentDefaults.get();
        nativeEnchantmentDefaults.set(Boolean.TRUE);
        try {
            return EnchModule.getDefaultMax(ench);
        } finally {
            if (previous == null) nativeEnchantmentDefaults.remove();
            else nativeEnchantmentDefaults.set(previous);
        }
    }

    public static boolean isCalculatingNativeEnchantmentDefault() {
        return Boolean.TRUE.equals(nativeEnchantmentDefaults.get());
    }

    private static EnchantingConfigPacket remoteEnchantingConfig() {
        return EffectiveSide.get().isClient() ? synchronizedEnchantingConfig : null;
    }

    public static void registerNetwork(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, EnchantingConfigPacket.class,
            (packet, buffer) -> {
                buffer.writeVarInt(packet.maxEterna());
                buffer.writeVarInt(packet.maxQuanta());
                buffer.writeVarInt(packet.maxArcana());
                buffer.writeVarInt(packet.maxEnchantments());
                buffer.writeVarInt(packet.extraLevelCap());
                buffer.writeVarInt(packet.extraLevelPowerPerLevel());
            },
            buffer -> new EnchantingConfigPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()),
            (packet, context) -> {
                context.get().enqueueWork(() -> synchronizedEnchantingConfig = packet);
                context.get().setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static EnchantingConfigPacket localEnchantingConfig() {
        return new EnchantingConfigPacket(MAX_ETERNA.get(), MAX_QUANTA.get(), MAX_ARCANA.get(),
            MAX_ENCHANTMENTS.get(), EXTRA_LEVEL_CAP.get(), EXTRA_LEVEL_POWER_PER_LEVEL.get());
    }

    public static void registerReforgingNetwork(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, ReforgingConfigPacket.class,
            (packet, buffer) -> buffer.writeBoolean(packet.enableCuriosReforging()),
            buffer -> new ReforgingConfigPacket(buffer.readBoolean()),
            (packet, context) -> {
                context.get().enqueueWork(() -> {
                    synchronizedReforgingConfig = packet;
                    ClientEvents.refreshReforgingViews();
                });
                context.get().setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static ReforgingConfigPacket effectiveServerReforgingConfig() {
        ReforgingConfigPacket snapshot = activeServerReforgingConfig;
        return snapshot == null ? new ReforgingConfigPacket(ENABLE_CURIOS_REFORGING.get()) : snapshot;
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ApotheosisNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), localEnchantingConfig());
            ApotheosisNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), effectiveServerReforgingConfig());
        }
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != REGISTERED_SPEC || ServerLifecycleHooks.getCurrentServer() == null) return;
        ApotheosisNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), localEnchantingConfig());
        ApotheosisNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), effectiveServerReforgingConfig());
    }

    private static final class MigratingConfigSpec extends UnmodifiableConfigWrapper<ForgeConfigSpec>
        implements IConfigSpec<MigratingConfigSpec> {

        private volatile boolean correcting;

        private MigratingConfigSpec(ForgeConfigSpec spec) {
            super(spec);
        }

        @Override
        public void acceptConfig(CommentedConfig values) {
            correcting = true;
            try {
                if (values != null) migrate(values);
                this.config.acceptConfig(values);
            } finally {
                correcting = false;
            }
        }

        @Override
        public boolean isCorrecting() {
            return correcting || this.config.isCorrecting();
        }

        @Override
        public boolean isCorrect(CommentedConfig values) {
            return this.config.isCorrect(values);
        }

        @Override
        public int correct(CommentedConfig values) {
            correcting = true;
            try {
                migrate(values);
                return this.config.correct(values);
            } finally {
                correcting = false;
            }
        }

        @Override
        public void afterReload() {
            this.config.afterReload();
        }

        private void migrate(CommentedConfig values) {
            migrateValue(values, "Loot_Rarity", "Reforging", "enable_curios_loot_rarity");
            migrateValue(values, "BetterCombat", "Combat_Formulas", "use_bettercombat_heavy_override");
        }

        private void migrateValue(CommentedConfig values, String previousGroup, String group, String key) {
            List<String> path = List.of(group, key);
            ForgeConfigSpec.ValueSpec definition = this.config.getSpec().get(path);
            Object previous = values.getRaw(previousGroup);
            if (!(previous instanceof UnmodifiableConfig previousSection)) return;
            Object previousValue = previousSection.getRaw(key);
            if (!definition.test(previousValue)) return;
            Object current = values.getRaw(group);
            if (current instanceof UnmodifiableConfig currentSection && definition.test(currentSection.getRaw(key))) return;
            if (!(current instanceof CommentedConfig)) values.set(group, values.createSubConfig());
            values.set(path, previousValue);
        }
    }

    public record ReforgingConfigPacket(boolean enableCuriosReforging) {}

    public record EnchantingConfigPacket(int maxEterna, int maxQuanta, int maxArcana,
        int maxEnchantments, int extraLevelCap, int extraLevelPowerPerLevel) {
        public EnchantingConfigPacket {
            maxEterna = Mth.clamp(maxEterna, 1, 1000);
            maxQuanta = Mth.clamp(maxQuanta, 1, 1000);
            maxArcana = Mth.clamp(maxArcana, 1, 1000);
            maxEnchantments = Mth.clamp(maxEnchantments, 1, 127);
            extraLevelCap = Mth.clamp(extraLevelCap, 1, 127);
            extraLevelPowerPerLevel = Mth.clamp(extraLevelPowerPerLevel, 1, 10000);
        }
    }

    @Mod.EventBusSubscriber(modid = ApotheosisArtificeMod.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            synchronizedEnchantingConfig = null;
            synchronizedReforgingConfig = null;
            refreshReforgingViews();
        }

        private static void refreshReforgingViews() {
            var mods = net.minecraftforge.fml.ModList.get();
            if (mods != null && mods.isLoaded("jei")) {
                com.apotheosis_artifice.enchant.ApotheosisArtificeJEIPlugin.refreshReforgingViews();
            }
        }
    }
}
