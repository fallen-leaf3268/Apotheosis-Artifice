package com.apotheosis_artifice.enchant;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.compat.EnigmaticLegacyCompat;
import com.apotheosis_artifice.compat.EasyMagicCompat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class MechanicalRavenEnchantMenu extends RavenEnchantMenu {

    public static MenuType<MechanicalRavenEnchantMenu> TYPE;
    private MechanicalRavenEnchantTile tile;
    public MechanicalRavenEnchantTile getTile() { return tile; }
    private int inputIdx = -1, outputIdx = -1, dedicatedCatalystIdx = -1;
    private volatile boolean broadcasting = false;
    private boolean manualEnchantInProgress;
    private final DataSlot outputPending = DataSlot.standalone();

    public boolean isOutputPending() {
        return this.tile != null ? this.tile.isOutputPending() : this.outputPending != null && this.outputPending.get() != 0;
    }

    public MechanicalRavenEnchantMenu(int id, Inventory inv, ContainerLevelAccess access, MechanicalRavenEnchantTile te, BlockPos pos, RavenTableStats stats) {
        super(id, inv, access, te, pos, stats);
        this.tile = te;
        if (!EasyMagicCompat.isLoaded()) {
            this.enchantSlots = te.getEnchantInventory();
        }
        this.replaceEnchantSlot();
        addIOSlots(te.getIOInv());
        this.addDataSlot(this.outputPending);
        ItemStack fromSave = te.getSavedEnchantSlot();
        if (EasyMagicCompat.isLoaded() && !fromSave.isEmpty()) {
            te.setSavedEnchantSlot(ItemStack.EMPTY);
            if (this.enchantSlots.getItem(0).isEmpty()) this.enchantSlots.setItem(0, fromSave);
            else inv.placeItemBackInInventory(fromSave);
        }
        this.enchantmentSeed.set((int) te.getEnchantmentSeed());
        te.refillEnchantSlot();
        this.slotsChanged(this.enchantSlots);
    }

    public MechanicalRavenEnchantMenu(int id, Inventory inv, float eterna, float quanta, float arcana, BlockPos pos) {
        super(id, inv, eterna, quanta, arcana);
        this.replaceEnchantSlot();
        addIOSlots(new ItemStackHandler(2));
        this.addDataSlot(this.outputPending);
        if (inv.player.level().isClientSide) {
            try {
                var rs = this.getRavenStats();
                var bs = dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.gatherStats(inv.player.level(), pos, this.enchantSlots.getItem(0).getEnchantmentValue());
                this.stats = new dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats(rs.eterna(), rs.quanta(), rs.arcana(), bs.rectification(), bs.clues(), bs.blacklist(), bs.treasure());
            } catch (Exception e) {
                ApotheosisArtificeMod.LOGGER.warn("[fromBuf] client stats failed", e);
            }
        }
    }

    private void replaceEnchantSlot() {
        Slot original = this.slots.get(0);
        Slot input = new Slot(this.enchantSlots, 0, original.x, original.y) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) {
                return !MechanicalRavenEnchantMenu.this.isOutputPending() && original.mayPlace(stack);
            }
        };
        input.index = original.index;
        this.slots.set(0, input);
    }

    private void addIOSlots(ItemStackHandler ioInv) {
        dedicatedCatalystIdx = -1;
        for (int i = 0; i < this.slots.size(); i++) {
            Slot slot = this.slots.get(i);
            if (slot.container == this.enchantSlots && slot.getContainerSlot() == 2) {
                dedicatedCatalystIdx = i;
                break;
            }
        }
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
        boolean pearlActive = EnigmaticLegacyCompat.isEnchanterPearlActive(this.player);
        return resolveGoldCount(pearlActive, this.getSlot(1).getItem().getCount());
    }

    static int resolveGoldCount(boolean pearlActive, int fuelCount) {
        if (pearlActive) return 64;
        return fuelCount;
    }

    public void persistEnchantmentSeed(int seed) {
        if (this.tile != null) {
            this.tile.setEnchantmentSeed(seed);
            this.tile.setChanged();
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if ((id >= 0 && id < 3 || id == 4) && this.isOutputPending()) return false;
        boolean manualEnchant = id >= 0 && id < 3 && !player.level().isClientSide && this.tile != null;
        if (!manualEnchant) return super.clickMenuButton(player, id);
        if (!this.tile.canAcceptManualOutput()) return false;
        ItemStack input = this.enchantSlots.getItem(0);
        ItemStack before = input.copy();
        int seed = this.enchantmentSeed.get();
        this.manualEnchantInProgress = true;
        boolean enchanted = false;
        try {
            enchanted = super.clickMenuButton(player, id);
            if (enchanted) this.persistEnchantmentSeed(this.enchantmentSeed.get());
            if (hasCompletedManualEnchant(enchanted, input, before, this.enchantSlots.getItem(0), seed, this.enchantmentSeed.get())) {
                this.tile.submitManualOutput();
            }
            return enchanted;
        } finally {
            this.manualEnchantInProgress = false;
            if (enchanted) this.enchantSlots.setChanged();
        }
    }

    static boolean hasCompletedManualEnchant(boolean accepted, ItemStack input, ItemStack before, ItemStack after, int oldSeed, int newSeed) {
        return accepted && !after.isEmpty() && (input != after || !ItemStack.matches(before, after) || oldSeed != newSeed);
    }

    @Override
    public void broadcastChanges() {
        if (this.tile != null && inputIdx >= 0 && !broadcasting && !this.manualEnchantInProgress) {
            broadcasting = true;
            try {
                this.enchantmentSeed.set((int) this.tile.getEnchantmentSeed());
                this.tile.flushEnchantedInput();
                this.tile.refillEnchantSlot();
                this.outputPending.set(this.tile.isOutputPending() ? 1 : 0);
                if (this.isOutputPending()) java.util.Arrays.fill(this.costs, 0);
            } finally { broadcasting = false; }
        }
        super.broadcastChanges();
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container inventoryIn) {
        if (this.tile != null && !this.manualEnchantInProgress) {
            this.enchantmentSeed.set((int) this.tile.getEnchantmentSeed());
        }
        super.slotsChanged(inventoryIn);
        if (this.isOutputPending()) java.util.Arrays.fill(this.costs, 0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        if (idx < 0 || idx >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack raw = slot.getItem();
        ItemStack stack = raw.copy();
        int playerStart = 2;
        int playerEnd = 38;
        boolean tableSlot = idx == 0 || idx == 1 || idx == dedicatedCatalystIdx || idx == inputIdx || idx == outputIdx;
        if (tableSlot) {
            if (!this.moveItemStackTo(raw, playerStart, playerEnd, false)) return ItemStack.EMPTY;
        } else if (idx >= playerStart && idx < playerEnd) {
            if (dedicatedCatalystIdx >= 0 && EasyMagicCompat.isRerollCatalyst(raw)
                && this.moveItemStackTo(raw, dedicatedCatalystIdx, dedicatedCatalystIdx + 1, false)) {
            } else if (raw.is(net.minecraftforge.common.Tags.Items.ENCHANTING_FUELS)
                && this.moveItemStackTo(raw, 1, 2, false)) {
            } else if (!this.moveItemStackTo(raw, inputIdx, inputIdx + 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (raw.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (raw.getCount() == stack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, raw);
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
