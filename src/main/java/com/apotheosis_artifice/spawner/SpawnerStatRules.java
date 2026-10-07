package com.apotheosis_artifice.spawner;

public final class SpawnerStatRules {
    private SpawnerStatRules() {}

    public static float normalizeHealth(float value) {
        if (!Float.isFinite(value)) return 1F;
        return Math.max(.2F, Math.min(1F, value));
    }

    public static float modifyHealth(float current, float value, float min, float max) {
        float next = Math.max(min, Math.min(max, current + value));
        return normalizeHealth(Math.round(next * 10000F) / 10000F);
    }

    public static boolean healthChanged(float current, float next) {
        return Math.abs(current - next) > .0001F;
    }

    public static int normalizeEchoes(int value) {
        return Math.max(0, Math.min(3, value));
    }

    public static int echoExperience(int experience, int level) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, experience) * (1 + normalizeEchoes(level)));
    }

    public static boolean needsExtendedFireSave(boolean burning, int ticks) {
        return burning && ticks > Short.MAX_VALUE;
    }
}
