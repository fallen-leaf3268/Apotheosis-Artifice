package com.apotheosis_artifice.enchant;

import com.apotheosis_artifice.compat.EasyMagicCompat;
import com.apotheosis_artifice.compat.EasyMagicEnchantingStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class MechanicalRavenEnchantTile extends RavenEnchantTile {

    public static BlockEntityType<MechanicalRavenEnchantTile> TYPE;

    private static final int INPUT = 0;
    private static final int OUTPUT = 1;

    private ResourceLocation libDim;
    private int libX, libY, libZ;
    private boolean libBound;
    private String libName = "";

    private ResourceLocation contDim;
    private int contX, contY, contZ;
    private boolean contBound;
    private String contName = "";

    private long enchantmentSeed = 0;
    private ItemStack pendingOutput = ItemStack.EMPTY;
    private final SimpleContainer enchantInventory = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            MechanicalRavenEnchantTile.this.setChanged();
            if (level == null || level.isClientSide) return;
            level.players().forEach(player -> {
                if (player.containerMenu instanceof MechanicalRavenEnchantMenu menu && menu.enchantSlots == this) {
                    menu.slotsChanged(this);
                }
            });
        }
    };

    public Container getEnchantInventory() { return this.enchantInventory; }
    public ItemStack getSavedEnchantSlot() { return this.enchantInventory.getItem(0); }
    public void setSavedEnchantSlot(ItemStack stack) { this.enchantInventory.setItem(0, stack.copy()); }

    private Container getActiveEnchantInventory() {
        if ((Object) this instanceof EasyMagicEnchantingStorage storage && EasyMagicCompat.isLoaded()) {
            return storage.getEasyMagicInventory();
        }
        return this.enchantInventory;
    }

    private final ItemStackHandler ioInv = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == INPUT) return !stack.isEmpty() && stack.getItem().getEnchantmentValue() > 0;
            return true;
        }
    };

    private LazyOptional<IItemHandler> ioCap = LazyOptional.of(this::createIOHandler);

    private IItemHandler createIOHandler() {
        return new IItemHandler() {
        @Override public int getSlots() { return 3; }
        @Override public ItemStack getStackInSlot(int slot) {
            if (slot == 0) return ioInv.getStackInSlot(0);
            if (slot == 1) return getFuelInv().getStackInSlot(0);
            if (slot == 2) return ioInv.getStackInSlot(1);
            return ItemStack.EMPTY;
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return stack;
            if (slot == 0) return ioInv.insertItem(0, stack, simulate);
            if (slot == 1) return getFuelInv().insertItem(0, stack, simulate);
            return stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 2) return ioInv.extractItem(1, amount, simulate);
            return ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return !stack.isEmpty() && stack.getItem().getEnchantmentValue() > 0;
            if (slot == 1) return getFuelInv().isItemValid(0, stack);
            return false;
        }
        };
    }
    private int tickCounter = 0;

    public MechanicalRavenEnchantTile(BlockPos pos, BlockState state) { super(pos, state); }

    public ItemStackHandler getIOInv() { return ioInv; }
    public void setEnchantmentSeed(long seed) { this.enchantmentSeed = seed; }
    public long getEnchantmentSeed() { return this.enchantmentSeed; }
    public boolean isContBound() { return contBound; }
    public ResourceLocation getContDim() { return contDim; }
    public BlockPos getContPos() { return new BlockPos(contX, contY, contZ); }
    public String getLibName() { return libName; }
    public String getContName() { return contName; }

    /** 直接存入绑定的容器/图书馆，不经过输出槽 */
    public ItemStack depositDirectToBound(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (libBound && libDim != null && stack.getItem() == Items.ENCHANTED_BOOK) {
            if (this.level == null || this.level.getServer() == null) return stack;
            var dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, libDim);
            var libLevel = this.level.getServer().getLevel(dimKey);
            if (libLevel == null) return stack;
            var be = libLevel.getBlockEntity(new BlockPos(libX, libY, libZ));
            if (!(be instanceof dev.shadowsoffire.apotheosis.ench.library.EnchLibraryTile lib)) return stack;
            int count = stack.getCount();
            for (int c = 0; c < count; c++) {
                ItemStack single = stack.copy(); single.setCount(1);
                lib.depositBook(single);
            }
            return ItemStack.EMPTY;
        }
        if (contBound && contDim != null) {
            if (this.level == null || this.level.getServer() == null) return stack;
            var dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, contDim);
            var contLevel = this.level.getServer().getLevel(dimKey);
            if (contLevel == null) return stack;
            var be = contLevel.getBlockEntity(new BlockPos(contX, contY, contZ));
            if (be == null || be == this) return stack;
            var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
            if (cap.isEmpty()) return stack;
            IItemHandler handler = cap.get();
            return ItemHandlerHelper.insertItem(handler, stack.copy(), false);
        }
        return stack;
    }

    public ItemStack storeOutput(ItemStack stack) {
        ItemStack remaining = this.depositDirectToBound(stack);
        return this.ioInv.insertItem(OUTPUT, remaining, false);
    }

    public void flushEnchantedInput() {
        Container input = this.getActiveEnchantInventory();
        ItemStack stack = input.getItem(0);
        if (!stack.isEnchanted() && !(stack.getItem() instanceof EnchantedBookItem)) return;
        ItemStack remaining = this.storeOutput(stack.copy());
        if (remaining.getCount() != stack.getCount()) input.setItem(0, remaining);
    }

    private void flushPendingOutput() {
        if (this.pendingOutput.isEmpty()) return;
        this.pendingOutput = this.storeOutput(this.pendingOutput);
        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MechanicalRavenEnchantTile tile) {
        if (level.isClientSide) return;
        if (++tile.tickCounter < 20) return;
        tile.tickCounter = 0;
        tile.tryDepositOutput();
        tile.flushPendingOutput();
        tile.flushEnchantedInput();
        if (level.hasNeighborSignal(pos)) {
            tile.tryAutoEnchant();
        }
        tile.tryDepositOutput();
    }

    private void tryAutoEnchant() {
        if (!this.pendingOutput.isEmpty()) return;
        if (this.tryAutoEnchantSaved()) return;
        ItemStack input = ioInv.getStackInSlot(INPUT);
        if (input.isEmpty() || input.isEnchanted() || input.getItem().getEnchantmentValue() <= 0) return;
        ItemStack toEnchant = ioInv.extractItem(INPUT, 1, true);
        if (toEnchant.isEmpty()) return;
        ItemStack result = doEnchant(toEnchant, this.ravenStats);
        if (result.isEmpty()) return;
        ioInv.extractItem(INPUT, 1, false);
        this.pendingOutput = result;
        this.flushPendingOutput();
        setChanged();
    }

    /** 关闭菜单后对附魔槽残留物品进行附魔并输出 */
    private boolean tryAutoEnchantSaved() {
        if (!this.pendingOutput.isEmpty()) return false;
        Container persistent = this.getActiveEnchantInventory();
        ItemStack saved = persistent.getItem(0);
        if (saved.isEmpty() || saved.isEnchanted() || saved.getItem().getEnchantmentValue() <= 0) return false;
        ItemStack toEnchant = saved.copy();
        toEnchant.setCount(1);
        ItemStack result = doEnchant(toEnchant, this.ravenStats);
        if (result.isEmpty()) return false;
        this.pendingOutput = result;
        persistent.removeItem(0, 1);
        this.flushPendingOutput();
        setChanged();
        return true;
    }

    /** 核心附魔逻辑：输入物品，返回已附魔的物品 */
    public ItemStack doEnchant(ItemStack input, RavenTableStats stats) {
        if (input.isEmpty() || input.isEnchanted() || input.getItem().getEnchantmentValue() <= 0) return ItemStack.EMPTY;

        float eterna = stats.eterna();
        float quanta = stats.quanta();
        float arcana = stats.arcana();
        var recipe = dev.shadowsoffire.apotheosis.ench.table.EnchantingRecipe.findMatch(this.level, input, eterna, quanta, arcana);

        if (recipe != null) {
            input = recipe.assemble(input, eterna, quanta, arcana);
            this.enchantmentSeed = this.level.random.nextInt();
            return input;
        }

        var bs = dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.gatherStats(this.level, this.worldPosition, input.getEnchantmentValue());
        int level = Math.round(eterna * 2);
        if (level <= 0) return ItemStack.EMPTY;
        var list = dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper.selectEnchantment(
            this.level.random, input, level, quanta, arcana, bs.rectification(), bs.treasure(), bs.blacklist());
        if (list.isEmpty()) return ItemStack.EMPTY;
        var instance = list.get(this.level.random.nextInt(list.size()));
        input = ((dev.shadowsoffire.apotheosis.ench.table.IEnchantableItem) input.getItem()).onEnchantment(input, list);
        this.enchantmentSeed = this.level.random.nextInt();
        return input;
    }

    private void tryDepositOutput() {
        ItemStack output = ioInv.getStackInSlot(OUTPUT);
        if (output.isEmpty()) return;
        ItemStack remaining = this.depositDirectToBound(output.copy());
        if (remaining.getCount() != output.getCount()) ioInv.setStackInSlot(OUTPUT, remaining);
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return ioCap.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); ioCap.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        ioCap = LazyOptional.of(this::createIOHandler);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("io_inv", ioInv.serializeNBT()); tag.putLong("ench_seed", this.enchantmentSeed);
        ItemStack saved = this.getSavedEnchantSlot();
        if (!saved.isEmpty()) tag.put("ench_slot", saved.save(new CompoundTag()));
        if (!this.pendingOutput.isEmpty()) tag.put("pending_output", this.pendingOutput.save(new CompoundTag()));
        if (libBound && libDim != null) {
            tag.putString("lb_dim", libDim.toString()); tag.putInt("lb_x", libX); tag.putInt("lb_y", libY); tag.putInt("lb_z", libZ);
            tag.putString("lb_name", this.libName);
        }
        if (contBound && contDim != null) {
            tag.putString("cont_dim", contDim.toString()); tag.putInt("cont_x", contX); tag.putInt("cont_y", contY); tag.putInt("cont_z", contZ);
            tag.putString("cont_name", this.contName);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("io_inv")) ioInv.deserializeNBT(tag.getCompound("io_inv"));
        if (tag.contains("ench_seed")) this.enchantmentSeed = tag.getLong("ench_seed");
        this.setSavedEnchantSlot(ItemStack.of(tag.getCompound("ench_slot")));
        this.pendingOutput = ItemStack.of(tag.getCompound("pending_output"));
        if (tag.contains("ench_slot")) {
            if ((Object) this instanceof EasyMagicEnchantingStorage storage
                && EasyMagicCompat.isLoaded() && !this.getSavedEnchantSlot().isEmpty()
                && storage.getEasyMagicInventory().getItem(0).isEmpty()) {
                ItemStack saved = this.enchantInventory.removeItemNoUpdate(0);
                storage.getEasyMagicInventory().setItem(0, saved);
            }
        }
        if (tag.contains("lb_dim")) {
            libBound = true; libDim = ResourceLocation.tryParse(tag.getString("lb_dim"));
            libX = tag.getInt("lb_x"); libY = tag.getInt("lb_y"); libZ = tag.getInt("lb_z");
            this.libName = tag.getString("lb_name");
        }
        if (tag.contains("cont_dim")) {
            contBound = true; contDim = ResourceLocation.tryParse(tag.getString("cont_dim"));
            contX = tag.getInt("cont_x"); contY = tag.getInt("cont_y"); contZ = tag.getInt("cont_z");
            this.contName = tag.getString("cont_name");
        }
    }

    @Override public BlockEntityType<?> getType() { return TYPE; }
}
