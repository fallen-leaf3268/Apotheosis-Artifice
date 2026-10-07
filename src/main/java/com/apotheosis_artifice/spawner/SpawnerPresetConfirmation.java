package com.apotheosis_artifice.spawner;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

public final class SpawnerPresetConfirmation<P> {

    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(30);
    private final Map<P, Pending> pending = new WeakHashMap<>();

    public Decision evaluate(P player, Target target, long nowNanos, boolean offhand, boolean mainhandRune) {
        if (offhand && mainhandRune) {
            return Decision.SUPPRESS;
        }
        if (SpawnerPreset.downgrades(target.current, target.target).isEmpty()) {
            this.clear(player);
            return Decision.APPLY;
        }
        Pending previous = this.pending.get(player);
        if (previous != null && previous.target.matches(target)
            && nowNanos - previous.createdNanos >= 0 && nowNanos - previous.createdNanos <= WINDOW_NANOS) {
            this.clear(player);
            return Decision.APPLY;
        }
        this.pending.put(player, new Pending(target, nowNanos));
        return Decision.PROMPT;
    }

    public void clear(P player) {
        this.pending.remove(player);
    }

    public record Target(String dimension, long position, Object tile, Object rune,
        SpawnerPreset preset, SpawnerPreset.Stats current, SpawnerPreset.Stats target, Object recipe) {
        public Target(String dimension, long position, Object tile, Object rune,
            SpawnerPreset preset, SpawnerPreset.Stats current) {
            this(dimension, position, tile, rune, preset, current, preset.stats(), rune);
        }

        public Target {
            Objects.requireNonNull(dimension);
            Objects.requireNonNull(tile);
            Objects.requireNonNull(rune);
            Objects.requireNonNull(preset);
            Objects.requireNonNull(current);
            Objects.requireNonNull(target);
            Objects.requireNonNull(recipe);
        }

        private boolean matches(Target other) {
            return this.position == other.position && this.tile == other.tile && this.rune == other.rune
                && this.preset == other.preset && this.recipe == other.recipe && this.dimension.equals(other.dimension)
                && this.current.equals(other.current) && this.target.equals(other.target);
        }
    }

    public enum Decision {
        APPLY, PROMPT, SUPPRESS
    }

    private record Pending(Target target, long createdNanos) {}
}
