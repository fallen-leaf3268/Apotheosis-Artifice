package com.apotheosis_artifice.enchant;

import org.jetbrains.annotations.Nullable;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantTile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;

public class RavenEnchantTile extends ApothEnchantTile {

    public static BlockEntityType<RavenEnchantTile> TYPE;
    protected RavenTableStats ravenStats = new RavenTableStats();

    public RavenEnchantTile(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public RavenTableStats getRavenStats() { return this.ravenStats; }
    public IItemHandler getFuelInv() { return this.inv; }

    @Override
    public BlockEntityType<?> getType() { return TYPE; }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.ravenStats.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.ravenStats.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        this.ravenStats.save(tag);
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.ravenStats.load(tag);
        }
    }
}
