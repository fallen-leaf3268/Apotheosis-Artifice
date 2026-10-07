package com.apotheosis_artifice.spawner;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class SpawnerStatData {
    public static final String HEALTH_ID = "artifice_initial_health";
    public static final String BURNING_ID = "artifice_burning";
    public static final String ECHOING_ID = "artifice_echoing";
    public static final String EXTENDED_FIRE_TAG = "ArtificeSpawnerFireTicks";

    private float initialHealth = 1F;
    private boolean burning;
    private int echoes;

    public float initialHealth() { return initialHealth; }
    public boolean burning() { return burning; }
    public int echoes() { return echoes; }
    public void setInitialHealth(float value) { initialHealth = SpawnerStatRules.normalizeHealth(value); }
    public void setBurning(boolean value) { burning = value; }
    public void setEchoes(int value) { echoes = SpawnerStatRules.normalizeEchoes(value); }

    public void save(CompoundTag tag) {
        tag.putFloat(HEALTH_ID, initialHealth);
        tag.putBoolean(BURNING_ID, burning);
        tag.putInt(ECHOING_ID, echoes);
    }

    public void load(CompoundTag tag) {
        setInitialHealth(tag.contains(HEALTH_ID, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(HEALTH_ID) : 1F);
        setBurning(tag.getBoolean(BURNING_ID));
        setEchoes(tag.getInt(ECHOING_ID));
    }
}
