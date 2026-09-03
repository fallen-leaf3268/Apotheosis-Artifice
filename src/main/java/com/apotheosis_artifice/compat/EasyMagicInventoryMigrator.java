package com.apotheosis_artifice.compat;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class EasyMagicInventoryMigrator {
    public interface Source<S> {
        S get(int slot);
        void clear(int slot);
    }

    public interface Target<S> {
        boolean hasItem();
        boolean mayPlace(S stack);
        void set(S stack);
    }

    private EasyMagicInventoryMigrator() {}

    public static <S> void migrate(Source<S> source, Target<S> input, Target<S> fuel,
        Consumer<S> returnStack, UnaryOperator<S> copy, Predicate<S> empty) {
        moveOrReturn(source, 0, input, returnStack, copy, empty);
        moveOrReturn(source, 1, fuel, returnStack, copy, empty);
        returnOrClear(source, 2, returnStack, copy, empty);
    }

    private static <S> void moveOrReturn(Source<S> source, int sourceSlot, Target<S> target,
        Consumer<S> returnStack, UnaryOperator<S> copy, Predicate<S> empty) {
        S stack = source.get(sourceSlot);
        if (empty.test(stack)) return;
        S moved = copy.apply(stack);
        if (!target.hasItem() && target.mayPlace(moved)) target.set(moved);
        else returnStack.accept(moved);
        source.clear(sourceSlot);
    }

    private static <S> void returnOrClear(Source<S> source, int sourceSlot,
        Consumer<S> returnStack, UnaryOperator<S> copy, Predicate<S> empty) {
        S stack = source.get(sourceSlot);
        if (empty.test(stack)) return;
        returnStack.accept(copy.apply(stack));
        source.clear(sourceSlot);
    }
}
