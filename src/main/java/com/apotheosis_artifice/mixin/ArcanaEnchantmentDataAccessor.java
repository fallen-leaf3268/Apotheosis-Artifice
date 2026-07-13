package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

@Mixin(RealEnchantmentHelper.ArcanaEnchantmentData.class)
public interface ArcanaEnchantmentDataAccessor {

    @Accessor(value = "data", remap = false)
    EnchantmentInstance getData();
}
