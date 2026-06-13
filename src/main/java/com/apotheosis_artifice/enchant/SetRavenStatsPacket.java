package com.apotheosis_artifice.enchant;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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

    public static void handle(SetRavenStatsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof RavenEnchantMenu menu)) return;
            if (!pkt.inputItem.isEmpty()) {
                if (menu instanceof MechanicalRavenEnchantMenu mech && mech.getTile() != null) {
                    var ioInv = mech.getTile().getIOInv();

                    // 1 清空缓冲（旧物回背包）
                    ItemStack bufItem = ioInv.getStackInSlot(0);
                    if (!bufItem.isEmpty()) {
                        returnToPlayer(player, bufItem);
                        ioInv.extractItem(0, bufItem.getCount(), false);
                    }

                    // 2 旧附魔物回背包
                    ItemStack oldEnch = menu.enchantSlots.getItem(0);
                    if (!oldEnch.isEmpty()) returnToPlayer(player, oldEnch);
                    mech.getTile().setSavedEnchantSlot(ItemStack.EMPTY);

                    // 3 插入 JEI 到缓冲
                    ItemStack leftover = ioInv.insertItem(0, pkt.inputItem.copy(), false);
                    int inserted = pkt.inputItem.getCount() - leftover.getCount();
                    if (inserted <= 0) return;

                    // 4 从缓冲取 1 个到附魔槽（不是额外复制，缓冲已包含 JEI 物品）
                    ItemStack enchItem = ioInv.extractItem(0, 1, false);
                    if (!enchItem.isEmpty()) {
                        menu.enchantSlots.setItem(0, enchItem);
                    }

                    // 5 扣背包
                    int toShrink = inserted;
                    for (int i = 0; i < player.getInventory().getContainerSize() && toShrink > 0; i++) {
                        var invStack = player.getInventory().getItem(i);
                        if (!invStack.isEmpty() && net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(invStack, pkt.inputItem)) {
                            int s = Math.min(toShrink, invStack.getCount());
                            invStack.shrink(s);
                            toShrink -= s;
                        }
                    }
                } else {
                    // 普通 RavenEnchantMenu: 先返回原物品到背包，再设置新物品
                    ItemStack old = menu.getSlot(0).getItem();
                    if (!old.isEmpty()) returnToPlayer(player, old);
                    menu.getSlot(0).set(pkt.inputItem);
                    menu.slotsChanged(menu.enchantSlots);
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        var invStack = player.getInventory().getItem(i);
                        if (!invStack.isEmpty() && net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(invStack, pkt.inputItem)) {
                            invStack.shrink(pkt.inputItem.getCount());
                            break;
                        }
                    }
                }
            }
            if (pkt.eterna != 0 || pkt.quanta != 0 || pkt.arcana != 0) {
                menu.setPlayerStats(pkt.eterna, pkt.quanta, pkt.arcana);
                if (menu instanceof MechanicalRavenEnchantMenu) {
                    menu.broadcastFullState();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
