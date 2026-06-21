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

    /** 从玩家背包真实移除最多 amount 个与 match 可堆叠的物品，返回实际取出的栈（数量 ≤ amount）。 */
    private static ItemStack extractFromInventory(ServerPlayer player, ItemStack match, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int need = amount;
        var inv = player.getInventory();
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
            if (!menu.stillValid(player)) return; // 复检：菜单仍打开且在有效距离/方块上
            if (!pkt.inputItem.isEmpty()) {
                if (menu instanceof MechanicalRavenEnchantMenu mech && mech.getTile() != null) {
                    var ioInv = mech.getTile().getIOInv();

                    // 1 清空缓冲（旧物回背包）
                    ItemStack bufItem = ioInv.getStackInSlot(0);
                    if (!bufItem.isEmpty()) {
                        ItemStack leftover = returnToPlayer(player, bufItem);
                        if (!leftover.isEmpty()) player.drop(leftover, true);
                        ioInv.extractItem(0, bufItem.getCount(), false);
                    }

                    // 2 旧附魔物回背包
                    ItemStack oldEnch = menu.enchantSlots.getItem(0);
                    if (!oldEnch.isEmpty()) {
                        ItemStack leftover = returnToPlayer(player, oldEnch);
                        if (!leftover.isEmpty()) player.drop(leftover, true);
                    }
                    mech.getTile().setSavedEnchantSlot(ItemStack.EMPTY);

                    // 3 从玩家背包「真实提取」要放入的物品（不信任 pkt.inputItem，避免凭空造物）
                    ItemStack taken = extractFromInventory(player, pkt.inputItem, pkt.inputItem.getCount());
                    if (taken.isEmpty()) return;

                    // 4 放入缓冲，再取 1 个到附魔槽；缓冲放不下的退回玩家
                    ItemStack leftover = ioInv.insertItem(0, taken, false);
                    if (!leftover.isEmpty()) {
                        leftover = returnToPlayer(player, leftover);
                        if (!leftover.isEmpty()) player.drop(leftover, true);
                    }
                    ItemStack enchItem = ioInv.extractItem(0, 1, false);
                    if (!enchItem.isEmpty()) {
                        menu.enchantSlots.setItem(0, enchItem);
                    }
                } else {
                    // 普通 RavenEnchantMenu: 旧物回背包，再从背包真实提取 1 个放入
                    ItemStack old = menu.getSlot(0).getItem();
                    if (!old.isEmpty()) {
                        ItemStack leftover = returnToPlayer(player, old);
                        if (!leftover.isEmpty()) player.drop(leftover, true);
                    }
                    ItemStack taken = extractFromInventory(player, pkt.inputItem, 1);
                    if (taken.isEmpty()) return;
                    menu.getSlot(0).set(taken);
                    menu.slotsChanged(menu.enchantSlots);
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
