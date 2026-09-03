package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.compat.EasyMagicInventoryMigration;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuEasyMagicMigrationMixin {
    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void artifice$runEasyMagicMigration(CallbackInfo ci) {
        if (this instanceof EasyMagicInventoryMigration migration) {
            migration.artifice$migrateEasyMagicInventory();
        }
    }
}
