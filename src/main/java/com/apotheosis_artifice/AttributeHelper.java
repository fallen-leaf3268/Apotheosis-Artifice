package com.apotheosis_artifice;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public final class AttributeHelper {

    private AttributeHelper() {}

    public static void addToBase(LivingEntity entity, Attribute attribute, String name, double delta) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst != null) inst.setBaseValue(inst.getBaseValue() + delta);
    }
}