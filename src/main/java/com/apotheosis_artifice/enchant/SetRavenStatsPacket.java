package com.apotheosis_artifice.enchant;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.function.Consumer;

public record SetRavenStatsPacket(float eterna, float quanta, float arcana, ItemStack inputItem) {

    public SetRavenStatsPacket(float eterna, float quanta, float arcana) {
        this(eterna, quanta, arcana, ItemStack.EMPTY);
    }

    public static void encode(SetRavenStatsPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.eterna);
        buf.writeFloat(pkt.quanta);
        buf.writeFloat(pkt.arcana);
        buf.writeItem(pkt.inputItem);
    }

    public static SetRavenStatsPacket decode(FriendlyByteBuf buf) {
        return new SetRavenStatsPacket(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readItem());
    }

    private static ItemStack returnToPlayer(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return stack;
        var rawInv = new net.minecraftforge.items.wrapper.InvWrapper(player.getInventory());
        // 优先背包栏 9-35，再快捷栏 0-8
        var backpack = new net.minecraftforge.items.wrapper.RangedWrapper(rawInv, 9, 36);
        var hotbar = new net.minecraftforge.items.wrapper.RangedWrapper(rawInv, 0, 9);
        stack = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(backpack, stack, false);
        if (!stack.isEmpty()) {
            stack = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(hotbar, stack, false);
        }
        return stack;
    }

    /** 从玩家背包真实移除最多 amount 个与 match 可堆叠的物品，返回实际取出的栈（数量 ≤ amount）。 */
    private static ItemStack extractFromInventory(Inventory inv, ItemStack match, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int need = amount;
        for (int i = 0; i < inv.getContainerSize() && need > 0; i++) {
            ItemStack invStack = inv.getItem(i);
            if (invStack.isEmpty()
                || !net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(invStack, match)) continue;
            int take = Math.min(need, invStack.getCount());
            if (result.isEmpty()) {
                result = invStack.copy();
                result.setCount(take);
            } else {
                result.grow(take);
            }
            invStack.shrink(take);
            need -= take;
        }
        return result;
    }

    public static void handle(SetRavenStatsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof RavenEnchantMenu menu)) return;
            if (!menu.stillValid(player)) return;
            if (!Float.isFinite(pkt.eterna) || !Float.isFinite(pkt.quanta) || !Float.isFinite(pkt.arcana)) return;
            if (!pkt.inputItem.isEmpty()) {
                if (!menu.getSlot(0).mayPlace(pkt.inputItem)) return;
                ItemStackHandler buffer = null;
                if (menu instanceof MechanicalRavenEnchantMenu mech && mech.getTile() != null) {
                    buffer = mech.getTile().getIOInv();
                    if (!buffer.isItemValid(0, pkt.inputItem)) return;
                }
                if (!replaceInput(menu.enchantSlots, buffer, player.getInventory(), pkt.inputItem,
                    stack -> returnOrDrop(player, stack))) return;
            }
            menu.setPlayerStats(pkt.eterna, pkt.quanta, pkt.arcana);
            if (menu instanceof MechanicalRavenEnchantMenu) {
                menu.broadcastFullState();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    static boolean replaceInput(Container input, ItemStackHandler buffer, Inventory inventory,
        ItemStack requested, Consumer<ItemStack> returnStack) {
        int amount = buffer == null ? 1 : Math.min(requested.getCount(), requested.getMaxStackSize());
        ItemStack taken = extractFromInventory(inventory, requested, amount);
        if (taken.isEmpty()) return false;
        ItemStack old = input.removeItemNoUpdate(0);
        ItemStack previousBuffer = buffer == null ? ItemStack.EMPTY
            : buffer.extractItem(0, buffer.getStackInSlot(0).getCount(), false);
        ItemStack replacement = taken.split(1);
        ItemStack leftover = buffer == null ? taken : buffer.insertItem(0, taken, false);
        input.setItem(0, replacement);
        if (!old.isEmpty()) returnStack.accept(old);
        if (!previousBuffer.isEmpty()) returnStack.accept(previousBuffer);
        if (!leftover.isEmpty()) returnStack.accept(leftover);
        return true;
    }

    private static void returnOrDrop(ServerPlayer player, ItemStack stack) {
        ItemStack leftover = returnToPlayer(player, stack);
        if (!leftover.isEmpty()) player.drop(leftover, true);
    }
}
