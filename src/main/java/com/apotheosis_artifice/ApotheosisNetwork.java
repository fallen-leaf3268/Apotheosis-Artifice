package com.apotheosis_artifice;

import com.apotheosis_artifice.gemcase.GemCaseMenu;

import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import com.apotheosis_artifice.ISlotSelectMenu;
import com.apotheosis_artifice.gemcase.GemCaseMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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

        CHANNEL.registerMessage(id++, ForceSlot0Packet.class,
            ForceSlot0Packet::encode,
            ForceSlot0Packet::decode,
            ForceSlot0Packet::handle);

        CHANNEL.registerMessage(id++, SyncCluesPacket.class,
            SyncCluesPacket::encode,
            SyncCluesPacket::decode,
            SyncCluesPacket::handle);
    }

    public static void sendClues(ServerPlayer player, int slot, List<EnchantmentInstance> clues) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCluesPacket(slot, clues));
    }

    public static void sendForceSlot0(ServerPlayer player, ItemStack stack) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ForceSlot0Packet(stack));
    }

    public static record ForceSlot0Packet(ItemStack stack) {
        public static void encode(ForceSlot0Packet pkt, FriendlyByteBuf buf) {
            buf.writeItem(pkt.stack);
        }
        public static ForceSlot0Packet decode(FriendlyByteBuf buf) {
            return new ForceSlot0Packet(buf.readItem());
        }
        public static void handle(ForceSlot0Packet pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ApotheosisArtificeMod.PROXY.handleForceSlot0(pkt.stack);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static record SyncCluesPacket(int slot, List<EnchantmentInstance> clues) {
        public static void encode(SyncCluesPacket pkt, FriendlyByteBuf buf) {
            buf.writeByte(pkt.slot);
            buf.writeByte(pkt.clues.size());
            for (var e : pkt.clues) {
                buf.writeShort(BuiltInRegistries.ENCHANTMENT.getId(e.enchantment));
                buf.writeByte(e.level);
            }
        }
        public static SyncCluesPacket decode(FriendlyByteBuf buf) {
            int slot = buf.readByte();
            int size = buf.readByte();
            List<EnchantmentInstance> clues = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                var ench = BuiltInRegistries.ENCHANTMENT.byId(buf.readShort());
                clues.add(new EnchantmentInstance(ench, buf.readByte()));
            }
            return new SyncCluesPacket(slot, clues);
        }
        public static void handle(SyncCluesPacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ApotheosisArtificeMod.PROXY.handleSyncClues(pkt.slot, pkt.clues);
            });
            ctx.get().setPacketHandled(true);
        }
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
        public static void handle(ToggleBinderPacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                var player = ctx.get().getSender();
                if (player == null) return;

                ItemStack found = ItemStack.EMPTY;

                if (player.containerMenu != null && pkt.slotIndex >= 0 && pkt.slotIndex < player.containerMenu.slots.size()) {
                    found = player.containerMenu.getSlot(pkt.slotIndex).getItem();
                }

                if (!(found.getItem() instanceof com.apotheosis_artifice.enchant.GemBinderItem)) {
                    for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                        found = player.getItemBySlot(slot);
                        if (found.getItem() instanceof com.apotheosis_artifice.enchant.GemBinderItem) break;
                    }
                }

                if (!(found.getItem() instanceof com.apotheosis_artifice.enchant.GemBinderItem)) {
                    var cap = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY).resolve();
                    if (cap.isPresent()) {
                        var curios = cap.get();
                        outer:
                        for (var entry : curios.getCurios().entrySet()) {
                            var stacks = entry.getValue().getStacks();
                            for (int i = 0; i < stacks.getSlots(); i++) {
                                found = stacks.getStackInSlot(i);
                                if (found.getItem() instanceof com.apotheosis_artifice.enchant.GemBinderItem) break outer;
                            }
                        }
                    }
                }

                if (found.getItem() instanceof com.apotheosis_artifice.enchant.GemBinderItem) {
                    com.apotheosis_artifice.enchant.GemBinderItem.cycleMode(found, player);
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
