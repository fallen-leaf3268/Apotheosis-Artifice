package com.apotheosis_artifice.enchant;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ApotheosisConfig;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import dev.shadowsoffire.placebo.network.PacketDistro;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

public class RavenEnchantMenu extends ApothEnchantmentMenu {

    public static MenuType<RavenEnchantMenu> TYPE;
    private final RavenTableStats ravenStats;

    public static RavenEnchantMenu fromBuf(int id, Inventory inv, FriendlyByteBuf buf) {
        buf.readBlockPos();
        return new RavenEnchantMenu(id, inv, buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public RavenEnchantMenu(int id, Inventory inv, float e, float q, float a) {
        super(id, inv);
        this.ravenStats = new RavenTableStats(e, q, a);
    }

    public RavenEnchantMenu(int id, Inventory inv, ContainerLevelAccess access, RavenEnchantTile te, BlockPos pos, RavenTableStats stats) {
        super(id, inv, access, te);
        this.ravenStats = stats;
    }

    public RavenTableStats getRavenStats() { return ravenStats; }

    public void syncStatsToSliders(float e, float q, float a) {
        if (this.stats != null) {
            this.stats = new TableStats(e, q, a,
                this.stats.rectification(), this.stats.clues(), this.stats.blacklist(), this.stats.treasure());
        }
    }

    @Override
    public MenuType<?> getType() { return TYPE; }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            (level.getBlockState(pos).getBlock() instanceof RavenEnchantingTableBlock)
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
            true);
    }

    @Override
    public void gatherStats() {
        this.access.evaluate((world, bp) -> {
            int ench = this.enchantSlots.getItem(0).getEnchantmentValue();
            var blockStats = ApothEnchantmentMenu.gatherStats(world, bp, ench);
            this.stats = new TableStats(
                this.ravenStats.eterna(), this.ravenStats.quanta(), this.ravenStats.arcana(),
                blockStats.rectification(), blockStats.clues(), blockStats.blacklist(), blockStats.treasure());
            PacketDistro.sendTo(Apotheosis.CHANNEL, new dev.shadowsoffire.apotheosis.ench.table.StatsMessage(this.stats), this.player);
            return this;
        }).orElse(this);
    }

    private static volatile float[] JEI_SLIDERS = null;

    public static float[] consumeJEISliders() {
        float[] v = JEI_SLIDERS;
        JEI_SLIDERS = null;
        return v;
    }

    public void transferJEI(float e, float q, float a) {
        float maxE = Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
        JEI_SLIDERS = new float[]{e, q, a};
        this.stats = new TableStats(Mth.clamp(e, 0, maxE), Mth.clamp(q, 0, ApotheosisConfig.MAX_QUANTA.get()), Mth.clamp(a, 0, ApotheosisConfig.MAX_ARCANA.get()),
            this.stats.rectification(), this.stats.clues(), this.stats.blacklist(), this.stats.treasure());
    }

    public void setPlayerStats(float eterna, float quanta, float arcana) {
        // 设计上是"滑条自由选"：玩家可在 [0, 绝对上限] / [0,100] 范围内自由设定 e/q/a。
        // 钳到绝对上限即可——既保留自由选玩法，又防止伪造包发送超出滑条范围的离谱值。
        float maxEterna = Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get());
        this.ravenStats.set(
            Mth.clamp(eterna, 0, maxEterna),
            Mth.clamp(quanta, 0, ApotheosisConfig.MAX_QUANTA.get()),
            Mth.clamp(arcana, 0, ApotheosisConfig.MAX_ARCANA.get()));
        this.access.evaluate((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof RavenEnchantTile rt) {
                rt.setChanged();
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 2);
            }
            return 0;
        });
        this.slotsChanged(this.enchantSlots);
    }
}
