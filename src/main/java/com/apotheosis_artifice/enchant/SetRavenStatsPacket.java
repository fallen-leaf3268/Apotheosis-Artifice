package com.apotheosis_artifice.enchant;

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

    public static void handle(SetRavenStatsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof RavenEnchantMenu menu) {
                if (!pkt.inputItem.isEmpty()) {
                    menu.getSlot(0).set(pkt.inputItem);
                }
                menu.setPlayerStats(pkt.eterna, pkt.quanta, pkt.arcana);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
