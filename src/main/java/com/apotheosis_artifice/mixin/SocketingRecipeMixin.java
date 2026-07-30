package com.apotheosis_artifice.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import dev.shadowsoffire.apotheosis.adventure.socket.SocketingRecipe;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketedGems;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Mixin(value = SocketingRecipe.class, priority = 500, remap = false)
public class SocketingRecipeMixin {

    @Overwrite
    public ItemStack m_5874_(Container inv, RegistryAccess regs) {
        ItemStack base = inv.getItem(1);
        ItemStack gemStack = inv.getItem(2);
        if (base.isEmpty() || gemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = base.copy();
        result.setCount(1);
        int socket = SocketHelper.getFirstEmptySocket(result);
        if (socket < 0) {
            return ItemStack.EMPTY;
        }
        List<GemInstance> gems = new ArrayList<>(SocketHelper.getGems(result).gems());
        if (socket >= gems.size()) {
            return ItemStack.EMPTY;
        }
        gems.set(socket, GemInstance.socketed(result, gemStack.copy()));
        SocketHelper.setGems(result, new SocketedGems(gems));
        return result;
    }
}
