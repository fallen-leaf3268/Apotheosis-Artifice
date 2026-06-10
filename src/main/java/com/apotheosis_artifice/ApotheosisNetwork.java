package com.apotheosis_artifice;

import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import com.apotheosis_artifice.ISlotSelectMenu;
import com.apotheosis_artifice.gemcase.GemCaseMenu;

public class ApotheosisNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ApotheosisArtificeMod.MODID + ":main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals);

    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(id++, SlotSelectPacket.class,
            (pkt, buf) -> buf.writeByte(pkt.slotIndex),
            buf -> new SlotSelectPacket(buf.readByte()),
            (pkt, ctx) -> {
                ctx.get().enqueueWork(() -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player == null) return;
                    if (!(player.containerMenu instanceof ReforgingMenu menu)) {
                        ApotheosisArtificeMod.LOGGER.warn("[APOTH] Server packet: menu is not ReforgingMenu, it's {}",
                            player.containerMenu.getClass().getName());
                        return;
                    }
                    ApotheosisArtificeMod.LOGGER.info("[APOTH] Server packet received: slotIdx={}", pkt.slotIndex);
                    ((ISlotSelectMenu) menu).curiosforge_selectSlot(pkt.slotIndex);
                    menu.slotsChanged(null);
                    menu.broadcastChanges();
                    ApotheosisArtificeMod.LOGGER.info("[APOTH] Server: slotsChanged + broadcast done");
                });
                ctx.get().setPacketHandled(true);
            });

        CHANNEL.registerMessage(id++, GemCaseSelectPacket.class,
            (pkt, buf) -> buf.writeUtf(pkt.gemId),
            buf -> new GemCaseSelectPacket(buf.readUtf()),
            (pkt, ctx) -> {
                ctx.get().enqueueWork(() -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player == null) return;
                    if (player.containerMenu instanceof GemCaseMenu menu) {
                        menu.setSelectedGemFromServer(pkt.gemId);
                        menu.broadcastChanges();
                    }
                });
                ctx.get().setPacketHandled(true);
            });

        CHANNEL.registerMessage(id++, GemCaseUpgradePacket.class,
            (pkt, buf) -> {
                buf.writeVarInt(pkt.rarityOrdinal);
                buf.writeVarInt(pkt.page);
                buf.writeBoolean(pkt.shift);
            },
            buf -> new GemCaseUpgradePacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean()),
            (pkt, ctx) -> {
                ctx.get().enqueueWork(() -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player == null) return;
                    if (player.containerMenu instanceof GemCaseMenu menu) {
                        menu.handleUpgradeClick(pkt.rarityOrdinal, pkt.shift, pkt.page);
                        menu.broadcastChanges();
                    }
                });
                ctx.get().setPacketHandled(true);
            });

        CHANNEL.registerMessage(id++, GemCasePagePacket.class,
            (pkt, buf) -> buf.writeVarInt(pkt.page),
            buf -> new GemCasePagePacket(buf.readVarInt()),
            (pkt, ctx) -> {
                ctx.get().enqueueWork(() -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player == null) return;
                    if (player.containerMenu instanceof GemCaseMenu menu) {
                        menu.setPage(pkt.page);
                        menu.broadcastChanges();
                    }
                });
                ctx.get().setPacketHandled(true);
            });

        CHANNEL.registerMessage(id++, com.apotheosis_artifice.enchant.SetRavenStatsPacket.class,
            com.apotheosis_artifice.enchant.SetRavenStatsPacket::encode,
            com.apotheosis_artifice.enchant.SetRavenStatsPacket::decode,
            com.apotheosis_artifice.enchant.SetRavenStatsPacket::handle);

        CHANNEL.registerMessage(id++, ToggleBinderPacket.class,
            ToggleBinderPacket::encode,
            ToggleBinderPacket::decode,
            ToggleBinderPacket::handle);
    }

    public static void sendToggleBinder(int slotIndex) {
        CHANNEL.sendToServer(new ToggleBinderPacket(slotIndex));
    }

    public static record ToggleBinderPacket(int slotIndex) {
        public static void encode(ToggleBinderPacket pkt, FriendlyByteBuf buf) {
            buf.writeInt(pkt.slotIndex);
        }
        public static ToggleBinderPacket decode(FriendlyByteBuf buf) {
            return new ToggleBinderPacket(buf.readInt());
        }
        public static void handle(ToggleBinderPacket pkt, java.util.function.Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                var player = ctx.get().getSender();
                if (player == null) return;
                if (player.containerMenu != null && pkt.slotIndex >= 0 && pkt.slotIndex < player.containerMenu.slots.size()) {
                    var stack = player.containerMenu.getSlot(pkt.slotIndex).getItem();
                    if (stack.getItem() instanceof com.apotheosis_artifice.enchant.GemBinderItem) {
                        var tag = stack.getOrCreateTag();
                        tag.putBoolean("salvage_mode", !tag.getBoolean("salvage_mode"));
                        return;
                    }
                }
                for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                    var stack = player.getItemBySlot(slot);
                    if (stack.getItem() instanceof com.apotheosis_artifice.enchant.GemBinderItem) {
                        var tag = stack.getOrCreateTag();
                        tag.putBoolean("salvage_mode", !tag.getBoolean("salvage_mode"));
                        return;
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static record SlotSelectPacket(int slotIndex) {}
    public static record GemCaseUpgradePacket(int rarityOrdinal, int page, boolean shift) {}
    public static record GemCaseSelectPacket(String gemId) {}
    public static record GemCasePagePacket(int page) {}
}
