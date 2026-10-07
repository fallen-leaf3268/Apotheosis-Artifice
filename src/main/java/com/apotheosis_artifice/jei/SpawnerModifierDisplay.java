package com.apotheosis_artifice.jei;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SpawnerModifierDisplay {

    private SpawnerModifierDisplay() {}

    public static String formatChange(Number value, Number min, Number max, boolean percentage) {
        if (value.doubleValue() == 0 && Double.compare(min.doubleValue(), max.doubleValue()) == 0) {
            return "→ " + (percentage ? percentage(min) : min.toString());
        }
        if (percentage) {
            return (value.doubleValue() > 0 ? "+" : "") + percentage(value);
        }
        return value.toString();
    }

    public static Number bound(Number value, boolean percentage) {
        return percentage ? new DisplayNumber(value, percentage(value), Math.round(value.floatValue() * 100)) : value;
    }

    public static Number change(Number value, Number min, Number max, boolean percentage) {
        return new DisplayNumber(value, formatChange(value, min, max, percentage), 0);
    }

    private static String percentage(Number value) {
        return BigDecimal.valueOf(value.doubleValue()).multiply(BigDecimal.valueOf(100))
            .setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private static final class DisplayNumber extends Number {

        private final Number value;
        private final String text;
        private final int displayInt;

        private DisplayNumber(Number value, String text, int displayInt) {
            this.value = value;
            this.text = text;
            this.displayInt = displayInt;
        }

        @Override
        public int intValue() {
            return displayInt;
        }

        @Override
        public long longValue() {
            return displayInt;
        }

        @Override
        public float floatValue() {
            return value.floatValue();
        }

        @Override
        public double doubleValue() {
            return value.doubleValue();
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
