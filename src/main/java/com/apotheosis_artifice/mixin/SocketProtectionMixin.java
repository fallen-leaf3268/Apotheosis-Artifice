package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;

import dev.shadowsoffire.apotheosis.adventure.loot.LootController;

/**
 * Socket clearing on rarity change is disabled to prevent socket loss on reforging.
 * Originally cleared sockets when rarity changed, but this caused items to lose
 * their sockets during curio reforging and crash the smithing table.
 */
@Mixin(value = LootController.class, remap = false)
public class SocketProtectionMixin {
    // Socket clearing disabled - see Javadoc
}
