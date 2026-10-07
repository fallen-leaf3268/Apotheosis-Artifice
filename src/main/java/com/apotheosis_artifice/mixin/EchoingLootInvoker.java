package com.apotheosis_artifice.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface EchoingLootInvoker {
    @Invoker("shouldDropLoot")
    boolean artifice$shouldDropLoot();

    @Invoker("dropFromLootTable")
    void artifice$dropFromLootTable(DamageSource source, boolean recentlyHit);
}
