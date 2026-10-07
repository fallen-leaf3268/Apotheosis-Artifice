package com.apotheosis_artifice.spawner;

import java.text.DecimalFormat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apotheosis.spawn.modifiers.SpawnerStat;
import dev.shadowsoffire.apotheosis.spawn.modifiers.SpawnerStats;
import dev.shadowsoffire.apotheosis.spawn.modifiers.StatModifier;
import dev.shadowsoffire.apotheosis.spawn.spawner.ApothSpawnerBlock;
import dev.shadowsoffire.apotheosis.spawn.spawner.ApothSpawnerTile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ArtificeSpawnerStats {
    public static final SpawnerStat<Float> INITIAL_HEALTH = new HealthStat();
    public static final SpawnerStat<Boolean> BURNING = new BurningStat();
    public static final SpawnerStat<Integer> ECHOING = new EchoingStat();

    private ArtificeSpawnerStats() {}

    public static void register() {
        SpawnerStats.REGISTRY.put(INITIAL_HEALTH.getId(), INITIAL_HEALTH);
        SpawnerStats.REGISTRY.put(BURNING.getId(), BURNING);
        SpawnerStats.REGISTRY.put(ECHOING.getId(), ECHOING);
    }

    private static SpawnerStatData data(ApothSpawnerTile spawner) {
        return ((ArtificeSpawnerData) spawner).artifice$getSpawnerStats();
    }

    public static void applySpawnAttributes(ApothSpawnerTile spawner, Entity entity) {
        SpawnerStatData data = data(spawner);
        entity.getSelfAndPassengers().forEach(spawned -> {
            if (data.initialHealth() != 1F && spawned instanceof LivingEntity living) {
                living.setHealth(living.getHealth() * data.initialHealth());
            }
            if (data.burning() && !spawned.fireImmune()) {
                spawned.setRemainingFireTicks(Integer.MAX_VALUE);
                spawned.getPersistentData().putBoolean(SpawnerStatData.BURNING_ID, true);
            }
            if (data.echoes() > 0) spawned.getPersistentData().putInt(SpawnerStatData.ECHOING_ID, data.echoes());
        });
    }

    private static final class HealthStat implements SpawnerStat<Float> {
        private final Codec<StatModifier<Float>> codec = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(-1F, 1F).fieldOf("value").forGetter(StatModifier::value),
            Codec.floatRange(.2F, 1F).fieldOf("min").forGetter(StatModifier::min),
            Codec.floatRange(.2F, 1F).fieldOf("max").forGetter(StatModifier::max)
        ).apply(instance, (value, min, max) -> new StatModifier<>(this, value, min, max)));

        public String getId() { return SpawnerStatData.HEALTH_ID; }
        public Codec<StatModifier<Float>> getModifierCodec() { return codec; }
        public Float getValue(ApothSpawnerTile spawner) { return data(spawner).initialHealth(); }
        public Component getTooltip(ApothSpawnerTile spawner) {
            return ApothSpawnerBlock.concat(name(), new DecimalFormat("0.##").format(getValue(spawner) * 100F) + "%");
        }
        public boolean apply(Float value, Float min, Float max, ApothSpawnerTile spawner) {
            float old = getValue(spawner);
            float next = SpawnerStatRules.modifyHealth(old, value, min, max);
            if (!SpawnerStatRules.healthChanged(old, next)) return false;
            data(spawner).setInitialHealth(next);
            return true;
        }
    }

    private static final class BurningStat implements SpawnerStat<Boolean> {
        private final Codec<StatModifier<Boolean>> codec = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("value").forGetter(StatModifier::value)
        ).apply(instance, value -> new StatModifier<>(this, value, false, true)));

        public String getId() { return SpawnerStatData.BURNING_ID; }
        public Codec<StatModifier<Boolean>> getModifierCodec() { return codec; }
        public Boolean getValue(ApothSpawnerTile spawner) { return data(spawner).burning(); }
        public Component getTooltip(ApothSpawnerTile spawner) {
            return getValue(spawner) ? name().withStyle(ChatFormatting.DARK_GREEN) : CommonComponents.EMPTY;
        }
        public boolean apply(Boolean value, Boolean min, Boolean max, ApothSpawnerTile spawner) {
            if (getValue(spawner).equals(value)) return false;
            data(spawner).setBurning(value);
            return true;
        }
    }

    private static final class EchoingStat implements SpawnerStat<Integer> {
        private final Codec<StatModifier<Integer>> codec = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(-3, 3).fieldOf("value").forGetter(StatModifier::value),
            Codec.intRange(0, 3).fieldOf("min").forGetter(StatModifier::min),
            Codec.intRange(0, 3).fieldOf("max").forGetter(StatModifier::max)
        ).apply(instance, (value, min, max) -> new StatModifier<>(this, value, min, max)));

        public String getId() { return SpawnerStatData.ECHOING_ID; }
        public Codec<StatModifier<Integer>> getModifierCodec() { return codec; }
        public Integer getValue(ApothSpawnerTile spawner) { return data(spawner).echoes(); }
        public Component getTooltip(ApothSpawnerTile spawner) {
            return getValue(spawner) > 0 ? ApothSpawnerBlock.concat(name(), getValue(spawner)) : CommonComponents.EMPTY;
        }
        public boolean apply(Integer value, Integer min, Integer max, ApothSpawnerTile spawner) {
            int old = getValue(spawner);
            int next = SpawnerStatRules.normalizeEchoes(Math.max(min, Math.min(max, old + value)));
            if (old == next) return false;
            data(spawner).setEchoes(next);
            return true;
        }
    }
}
