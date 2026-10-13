package com.apotheosis_artifice.jei;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

final class AffixPreviewText {
    private static final String BOUNDS_KEY = "misc.apotheosis.affix_bounds";
    private static final Object UNSUPPORTED = new Object();

    private AffixPreviewText() {}

    static boolean hasBounds(Component text) {
        if (text.getContents() instanceof TranslatableContents translated) {
            if (BOUNDS_KEY.equals(translated.getKey()) && translated.getArgs().length == 2) return true;
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Component component && hasBounds(component)) return true;
            }
        }
        return text.getSiblings().stream().anyMatch(AffixPreviewText::hasBounds);
    }

    static Component midpoint(Component example, Component lower, Component upper) {
        DecimalFormat format = (DecimalFormat) ItemStack.ATTRIBUTE_MODIFIER_FORMAT.clone();
        format.setParseBigDecimal(true);
        format.setMaximumFractionDigits(Math.max(8, format.getMaximumFractionDigits()));
        Component result = midpointNode(example, lower, upper, format);
        return result == null ? example.copy() : result;
    }

    private static Component midpointNode(Component example, Component lower, Component upper, DecimalFormat format) {
        if (!example.getStyle().equals(lower.getStyle()) || !example.getStyle().equals(upper.getStyle())
            || example.getSiblings().size() != lower.getSiblings().size()
            || example.getSiblings().size() != upper.getSiblings().size()) return null;
        MutableComponent result;
        if (example.getContents() instanceof TranslatableContents current
            && lower.getContents() instanceof TranslatableContents low
            && upper.getContents() instanceof TranslatableContents high) {
            if (!current.getKey().equals(low.getKey()) || !current.getKey().equals(high.getKey())
                || !Objects.equals(current.getFallback(), low.getFallback())
                || !Objects.equals(current.getFallback(), high.getFallback())
                || current.getArgs().length != low.getArgs().length
                || current.getArgs().length != high.getArgs().length) return null;
            if (BOUNDS_KEY.equals(current.getKey())) return example.copy();
            Object[] args = new Object[current.getArgs().length];
            for (int index = 0; index < args.length; index++) {
                args[index] = midpointArgument(current.getArgs()[index], low.getArgs()[index], high.getArgs()[index], format);
                if (args[index] == UNSUPPORTED) {
                    Object original = current.getArgs()[index];
                    args[index] = original instanceof Component component ? component.copy() : original;
                }
            }
            result = MutableComponent.create(new TranslatableContents(current.getKey(), current.getFallback(), args));
        } else if (example.getContents() instanceof LiteralContents current
            && lower.getContents() instanceof LiteralContents low
            && upper.getContents() instanceof LiteralContents high) {
            Object value = midpointArgument(current.text(), low.text(), high.text(), format);
            if (value == UNSUPPORTED) return null;
            result = Component.literal((String) value);
        } else {
            if (!example.getContents().equals(lower.getContents()) || !example.getContents().equals(upper.getContents())) return null;
            result = example.plainCopy();
        }
        result.setStyle(example.getStyle());
        for (int index = 0; index < example.getSiblings().size(); index++) {
            Component child = midpointNode(example.getSiblings().get(index), lower.getSiblings().get(index),
                upper.getSiblings().get(index), format);
            if (child == null) return null;
            result.append(child);
        }
        return result;
    }

    private static Object midpointArgument(Object example, Object lower, Object upper, DecimalFormat format) {
        if (example instanceof Component current && lower instanceof Component low && upper instanceof Component high) {
            Component result = midpointNode(current, low, high, format);
            return result == null ? UNSUPPORTED : result;
        }
        if (Objects.equals(example, lower) && Objects.equals(example, upper)) return example;
        if (!(example instanceof String && lower instanceof String && upper instanceof String)
            && !(example instanceof Number && lower instanceof Number && upper instanceof Number)) return UNSUPPORTED;
        BigDecimal current = number(example, format);
        BigDecimal low = number(lower, format);
        BigDecimal high = number(upper, format);
        if (current == null || low == null || high == null) return UNSUPPORTED;
        return format.format(low.add(high).divide(BigDecimal.valueOf(2)));
    }

    private static BigDecimal number(Object value, DecimalFormat format) {
        if (value instanceof Number) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        if (value instanceof String text) {
            ParsePosition position = new ParsePosition(0);
            Number parsed = format.parse(text, position);
            if (position.getIndex() == text.length() && parsed instanceof BigDecimal decimal) return decimal;
        }
        return null;
    }
}
