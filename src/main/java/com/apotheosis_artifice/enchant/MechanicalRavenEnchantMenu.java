package com.apotheosis_artifice.enchant;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class MechanicalRavenEnchantMenu extends RavenEnchantMenu {

    public static MenuType<MechanicalRavenEnchantMenu> TYPE;
    private MechanicalRavenEnchantTile tile;
    public MechanicalRavenEnchantTile getTile() { return tile; }
    private int inputIdx = -1, outputIdx = -1;
    private volatile boolean broadcasting = false;
    private int lastGoldCount = 0;

    public MechanicalRavenEnchantMenu(int id, Inventory inv, ContainerLevelAccess access, MechanicalRavenEnchantTile te, BlockPos pos, RavenTableStats stats) {
        super(id, inv, access, te, pos, stats);
        this.tile = te;
        addIOSlots(te.getIOInv());
        // 优先从已保存的附魔槽加载，没有则从缓冲提取
        ItemStack fromSave = te.getSavedEnchantSlot();
        if (!fromSave.isEmpty()) {
            this.enchantSlots.setItem(0, fromSave.copy());
        } else {
            ItemStack fromBuf = te.getIOInv().extractItem(0, 1, false);
            if (!fromBuf.isEmpty()) {
                this.enchantSlots.setItem(0, fromBuf);
            }
        }
    }

    public MechanicalRavenEnchantMenu(int id, Inventory inv, float eterna, float quanta, float arcana, BlockPos pos) {
        super(id, inv, eterna, quanta, arcana);
        addIOSlots(new ItemStackHandler(2));
        if (inv.player.level().isClientSide) {
            try {
                var rs = this.getRavenStats();
                var bs = dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.gatherStats(
                    inv.player.level(), pos, this.enchantSlots.getItem(0).getEnchantmentValue());
                this.stats = new dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats(
                    rs.eterna(), rs.quanta(), rs.arcana(),
                    bs.rectification(), bs.clues(), bs.blacklist(), bs.treasure());
            } catch (Exception e) {
                ApotheosisArtificeMod.LOGGER.warn("[fromBuf] client stats failed", e);
            }
        }
    }

    private void addIOSlots(ItemStackHandler ioInv) {
        inputIdx = this.slots.size();
        this.addSlot(new SlotItemHandler(ioInv, 0, 15, 17) { @Override public int getMaxStackSize() { return 64; } });
        outputIdx = this.slots.size();
        this.addSlot(new SlotItemHandler(ioInv, 1, 35, 17) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return true; }
        });
    }

    @Override
    public int getGoldCount() {
        if (this.tile != null) { lastGoldCount = this.tile.getFuelInv().getStackInSlot(0).getCount(); return lastGoldCount; }
        int v = this.getSlot(1).getItem().getCount();
        if (v > 0) lastGoldCount = v;
        return lastGoldCount;
    }

    private ItemStack lastS0 = ItemStack.EMPTY;

    @Override
    public void broadcastChanges() {
        if (this.tile != null && inputIdx >= 0 && !broadcasting) {
            broadcasting = true;
            try {
                this.enchantmentSeed.set((int) this.tile.getEnchantmentSeed());
                var io = this.tile.getIOInv();
                // 附魔槽空且缓冲有可附魔物品 → 提取到附魔槽
                if (this.enchantSlots.getItem(0).isEmpty()) {
                    ItemStack buf = io.getStackInSlot(0);
                    if (!buf.isEmpty() && !buf.isEnchanted() && buf.getItem().getEnchantmentValue() > 0) {
                        ItemStack n = io.extractItem(0, 1, false);
                        if (!n.isEmpty()) {
                            this.enchantSlots.setItem(0, n);
                            this.enchantSlots.setChanged();
                        }
                    }
                }
            } finally { broadcasting = false; }
        }
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        if (this.tile != null) {
            ItemStack e = this.enchantSlots.getItem(0);
            this.tile.setSavedEnchantSlot(e);
        }
        this.enchantSlots.setItem(0, ItemStack.EMPTY);
        super.removed(player);
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container inventoryIn) { super.slotsChanged(inventoryIn); }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(idx);
        if (slot != null && slot.hasItem()) {
            ItemStack raw = slot.getItem(); stack = raw.copy();
            if (idx <= 1 || idx >= 38) { if (!this.moveItemStackTo(raw, 2, 38, false)) return ItemStack.EMPTY; }
            else if (idx >= 2 && idx <= 37) {
                if (raw.is(net.minecraftforge.common.Tags.Items.ENCHANTING_FUELS)) {
                    if (!this.moveItemStackTo(raw, 1, 2, false)) { if (!this.moveItemStackTo(raw, 38, 39, false)) return ItemStack.EMPTY; }
                } else if (!this.moveItemStackTo(raw, 38, 39, false)) { if (!this.moveItemStackTo(raw, 0, 1, false)) return ItemStack.EMPTY; }
            }
            if (raw.isEmpty()) slot.set(ItemStack.EMPTY);
            slot.setChanged();
        }
        return stack;
    }

    @Override public MenuType<?> getType() { return TYPE; }

    public static MechanicalRavenEnchantMenu fromBuf(int id, Inventory inv, net.minecraft.network.FriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        float e = buf.readFloat(), q = buf.readFloat(), a = buf.readFloat();
        var item = buf.readItem();
        var menu = new MechanicalRavenEnchantMenu(id, inv, e, q, a, pos);
        if (!item.isEmpty()) {
            menu.enchantSlots.setItem(0, item.copy());
            try {
                var rs = menu.getRavenStats();
                var bs = dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.gatherStats(inv.player.level(), pos, item.getEnchantmentValue());
                menu.stats = new dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats(rs.eterna(), rs.quanta(), rs.arcana(), bs.rectification(), bs.clues(), bs.blacklist(), bs.treasure());
            } catch (Exception ex) { ApotheosisArtificeMod.LOGGER.warn("[fromBuf] stats failed", ex); }
        }
        return menu;
    }
}
