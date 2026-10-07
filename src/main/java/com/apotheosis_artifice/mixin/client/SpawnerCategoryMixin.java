package com.apotheosis_artifice.mixin.client;

import com.apotheosis_artifice.jei.SpawnerModifierDisplay;

import dev.shadowsoffire.apotheosis.spawn.modifiers.StatModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.spawn.compat.SpawnerCategory", remap = false)
public class SpawnerCategoryMixin {

    @Redirect(method = "draw", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/apotheosis/spawn/modifiers/StatModifier;value()Ljava/lang/Object;"))
    private Object artifice$displayValue(StatModifier<?> modifier) {
        if (modifier.value() instanceof Number value && modifier.min() instanceof Number min && modifier.max() instanceof Number max) {
            boolean percentage = modifier.stat().getId().equals("artifice_initial_health");
            if (percentage || value.doubleValue() == 0 && Double.compare(min.doubleValue(), max.doubleValue()) == 0) {
                return SpawnerModifierDisplay.change(value, min, max, percentage);
            }
        }
        return modifier.value();
    }

    @Redirect(method = "draw", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/apotheosis/spawn/modifiers/StatModifier;min()Ljava/lang/Object;"))
    private Object artifice$displayMinimum(StatModifier<?> modifier) {
        return modifier.min() instanceof Number value
            ? SpawnerModifierDisplay.bound(value, modifier.stat().getId().equals("artifice_initial_health")) : modifier.min();
    }

    @Redirect(method = "draw", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/apotheosis/spawn/modifiers/StatModifier;max()Ljava/lang/Object;"))
    private Object artifice$displayMaximum(StatModifier<?> modifier) {
        return modifier.max() instanceof Number value
            ? SpawnerModifierDisplay.bound(value, modifier.stat().getId().equals("artifice_initial_health")) : modifier.max();
    }
}
