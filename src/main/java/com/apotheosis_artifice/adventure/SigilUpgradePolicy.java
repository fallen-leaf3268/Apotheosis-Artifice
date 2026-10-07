package com.apotheosis_artifice.adventure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import com.google.gson.JsonElement;

public final class SigilUpgradePolicy {

    public static final float MALICE_LEVEL = 2.0F;
    public static final float FINAL_MALICE_CHANCE = 0.033F;

    public static boolean canApplySupremacy(List<Affix> input, boolean alreadyUsed) {
        return input.stream().anyMatch(affix -> affix.valid && Float.isFinite(affix.level) && affix.level < 1.5F);
    }

    public static List<Affix> supremacy(List<Affix> input) {
        return input.stream().map(affix -> affix.valid
            ? affix.withPower(Math.max(affix.level, 1.5F), Math.max(affix.level, 1.5F), false) : affix).toList();
    }

    public static boolean canUpgrade(float level, float maximum) {
        return Float.isFinite(level) && Float.isFinite(maximum) && maximum > 0 && level >= 0 && level < maximum;
    }

    public static float upgrade(float level, float maximum) {
        return canUpgrade(level, maximum) ? Math.min(maximum, level + 0.25F) : level;
    }

    public static boolean canApplyMalice(List<Affix> input) {
        List<Integer> targets = targets(input);
        return targets.size() >= 2 && !fullyMalicious(input, targets);
    }

    public static boolean isExtremelyMalicious(List<Affix> input) {
        return fullyMalicious(input, targets(input));
    }

    public static MaliceResult malice(List<Affix> input, int uses, Rng random) {
        List<Integer> targets = targets(input);
        if (!canApplyMalice(input)) {
            return new MaliceResult(List.copyOf(input), uses, -1, -1, false, fullyMalicious(input, targets));
        }
        List<Integer> upgradable = targets.stream().filter(index -> input.get(index).level < MALICE_LEVEL).toList();
        int boosted;
        int reset = -1;
        if (upgradable.size() == 1) {
            boosted = upgradable.get(0);
            if (random.nextFloat() >= FINAL_MALICE_CHANCE) {
                var others = new ArrayList<>(targets);
                others.remove(Integer.valueOf(boosted));
                reset = others.get(random.nextInt(others.size()));
            }
        } else {
            boosted = targets.get(random.nextInt(targets.size()));
            int second;
            do {
                second = random.nextInt(targets.size());
            } while (targets.get(second) == boosted);
            reset = targets.get(second);
        }
        var output = new ArrayList<>(input);
        float boostedLevel = Math.max(input.get(boosted).level, MALICE_LEVEL);
        output.set(boosted, input.get(boosted).withPower(boostedLevel, boostedLevel, false));
        if (reset >= 0) {
            output.set(reset, input.get(reset).withPower(0, 1F, false));
        }
        return new MaliceResult(List.copyOf(output), uses + 1, boosted, reset, true, fullyMalicious(output, targets));
    }

    public static boolean isLevelIndependent(ValueKind kind, JsonElement value) {
        return switch (kind) {
            case FIXED -> true;
            case UNKNOWN -> true;
            case SCALAR -> isConstant(value);
            case CLEAVING -> value != null && value.isJsonObject()
                && isConstant(value.getAsJsonObject().get("chance")) && isConstant(value.getAsJsonObject().get("targets"));
            case MOB_EFFECT -> value != null && value.isJsonObject()
                && isConstant(value.getAsJsonObject().get("duration")) && isConstant(value.getAsJsonObject().get("amplifier"));
            case RADIAL -> value != null && value.isJsonArray() && value.getAsJsonArray().size() == 1;
        };
    }

    public static boolean consumePending(boolean server, boolean enabled, BooleanSupplier pending, Runnable clear, Runnable apply) {
        if (!server || !enabled || !pending.getAsBoolean()) {
            return false;
        }
        clear.run();
        apply.run();
        return true;
    }

    private static boolean isConstant(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return false;
        }
        if (value.isJsonPrimitive()) {
            return value.getAsJsonPrimitive().isNumber();
        }
        if (!value.isJsonObject()) {
            return false;
        }
        JsonElement step = value.getAsJsonObject().get("step");
        return step != null && step.isJsonPrimitive() && step.getAsJsonPrimitive().isNumber() && step.getAsFloat() == 0;
    }

    private static List<Integer> targets(List<Affix> input) {
        return IntStream.range(0, input.size()).filter(index -> {
            Affix affix = input.get(index);
            return affix.valid && !affix.levelIndependent;
        }).boxed().toList();
    }

    private static boolean fullyMalicious(List<Affix> input, List<Integer> targets) {
        return !targets.isEmpty() && targets.stream().allMatch(index -> input.get(index).level >= MALICE_LEVEL);
    }

    public record Affix(String id, float level, boolean valid, boolean levelIndependent, boolean durability, float maximum, boolean maliceUsed) {
        public Affix(String id, float level, boolean valid, boolean levelIndependent, boolean durability) {
            this(id, level, valid, levelIndependent, durability, Math.max(1F, level));
        }

        public Affix(String id, float level, boolean valid, boolean levelIndependent, boolean durability, float maximum) {
            this(id, level, valid, levelIndependent, durability, maximum, maximum >= MALICE_LEVEL);
        }

        private Affix withPower(float level, float maximum) {
            return withPower(level, maximum, this.maliceUsed);
        }

        private Affix withPower(float level, float maximum, boolean maliceUsed) {
            return new Affix(this.id, level, this.valid, this.levelIndependent, this.durability, maximum, maliceUsed);
        }
    }

    public record MaliceResult(List<Affix> affixes, int uses, int boosted, int reset, boolean applied, boolean fullyMalicious) {}

    public interface Rng {
        int nextInt(int bound);

        float nextFloat();
    }

    public enum ValueKind {
        SCALAR, CLEAVING, MOB_EFFECT, RADIAL, FIXED, UNKNOWN
    }
}
