package com.apotheosis_artifice.spawner;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum SpawnerPreset {
    FRONTIER(new Stats(150, 600, 7, 12, 24)),
    ASCENT(new Stats(100, 400, 10, 18, 32)),
    SUMMIT(new Stats(50, 200, 13, 24, 40)),
    PINNACLE(new Stats(20, 20, 16, 32, 48));

    private final Stats stats;

    SpawnerPreset(Stats stats) {
        this.stats = stats;
    }

    public Stats stats() {
        return this.stats;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public List<Change> downgradesFrom(Stats current) {
        return downgrades(current, this.stats);
    }

    public static List<Change> downgrades(Stats current, Stats target) {
        return Arrays.stream(Stat.values())
            .filter(stat -> stat.lowerIsBetter ? target.value(stat) > current.value(stat)
                : target.value(stat) < current.value(stat))
            .map(stat -> new Change(stat, current.value(stat), target.value(stat)))
            .toList();
    }

    public record Stats(int minDelay, int maxDelay, int spawnCount, int maxNearby, int playerRange) {
        public int value(Stat stat) {
            return switch (stat) {
                case MIN_DELAY -> this.minDelay;
                case MAX_DELAY -> this.maxDelay;
                case SPAWN_COUNT -> this.spawnCount;
                case MAX_NEARBY_ENTITIES -> this.maxNearby;
                case REQ_PLAYER_RANGE -> this.playerRange;
            };
        }

        public int[] values() {
            return new int[]{this.minDelay, this.maxDelay, this.spawnCount, this.maxNearby, this.playerRange};
        }

        public Stats apply(Stat stat, int value, int min, int max) {
            int adjusted = (short) this.value(stat) + (short) value;
            int lower = (short) min;
            int upper = (short) max;
            int[] result = this.values();
            result[stat.ordinal()] = (short) (adjusted < lower ? lower : Math.min(adjusted, upper));
            return new Stats(result[0], result[1], result[2], result[3], result[4]);
        }
    }

    public record Change(Stat stat, int current, int target) {}

    public enum Stat {
        MIN_DELAY("min_delay", true),
        MAX_DELAY("max_delay", true),
        SPAWN_COUNT("spawn_count", false),
        MAX_NEARBY_ENTITIES("max_nearby_entities", false),
        REQ_PLAYER_RANGE("req_player_range", false);

        private final String id;
        private final boolean lowerIsBetter;

        Stat(String id, boolean lowerIsBetter) {
            this.id = id;
            this.lowerIsBetter = lowerIsBetter;
        }

        public String id() {
            return this.id;
        }
    }
}
