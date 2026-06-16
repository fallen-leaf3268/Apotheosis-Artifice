package com.apotheosis_artifice.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;

@Mixin(value = Gem.class, remap = false)
public interface GemAccessor {

    @Accessor("bonusMap")
    Map<LootCategory, GemBonus> getBonusMap();
}
