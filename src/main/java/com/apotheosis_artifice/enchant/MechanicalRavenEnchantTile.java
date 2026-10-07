package com.apotheosis_artifice.enchant;

import com.apotheosis_artifice.ApotheosisConfig;
import com.apotheosis_artifice.compat.EasyMagicCompat;
import com.apotheosis_artifice.compat.EasyMagicEnchantingStorage;
import com.apotheosis_artifice.lead.EnderLeadAccess;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingRecipe;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
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
    private ItemStack completedWorkStack = ItemStack.EMPTY;
    private boolean movingInventory;
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
            if (slot == INPUT) return canEnchantInput(stack);
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
            if (slot == 0) return canEnchantInput(stack);
            if (slot == 1) return getFuelInv().isItemValid(0, stack);
            return false;
        }
        };
    }
    private int tickCounter = 0;
    private int autoTickCounter = 0;

    private boolean canEnchantInput(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack single = stack.copyWithCount(1);
        return single.isEnchantable() && single.getEnchantmentValue() > 0
            || this.level != null && EnchantingRecipe.findItemMatch(this.level, stack) != null;
    }

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

    public boolean isOutputPending() {
        ItemStack current = this.getActiveEnchantInventory().getItem(0);
        if (current.isEmpty() || current != this.completedWorkStack) {
            this.completedWorkStack = ItemStack.EMPTY;
            return false;
        }
        return true;
    }

    public boolean canAcceptManualOutput() { return !this.isOutputPending(); }

    public void refillEnchantSlot() {
        if (this.movingInventory) return;
        Container input = this.getActiveEnchantInventory();
        if (!input.getItem(0).isEmpty()) return;
        ItemStack buffered = this.ioInv.getStackInSlot(INPUT);
        if (!this.ioInv.isItemValid(INPUT, buffered)) return;
        this.movingInventory = true;
        try {
            this.completedWorkStack = ItemStack.EMPTY;
            input.setItem(0, this.ioInv.extractItem(INPUT, 1, false));
            this.setChanged();
        } finally {
            this.movingInventory = false;
        }
    }

    public boolean submitManualOutput() {
        if (this.movingInventory || !this.canAcceptManualOutput()) return false;
        Container input = this.getActiveEnchantInventory();
        ItemStack result = input.getItem(0);
        if (result.isEmpty()) return false;
        this.completedWorkStack = result;
        this.setChanged();
        this.flushEnchantedInput();
        this.refillEnchantSlot();
        return true;
    }

    public void flushEnchantedInput() {
        if (this.movingInventory || !this.isOutputPending()) return;
        Container input = this.getActiveEnchantInventory();
        ItemStack stack = input.getItem(0);
        this.movingInventory = true;
        try {
            ItemStack remaining = this.storeOutput(stack.copy());
            if (remaining.getCount() != stack.getCount()) {
                this.completedWorkStack = remaining;
                input.setItem(0, remaining);
                this.setChanged();
            }
        } finally {
            this.movingInventory = false;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MechanicalRavenEnchantTile tile) {
        if (level.isClientSide) return;
        tile.refillEnchantSlot();
        boolean moveOutput = ++tile.tickCounter >= 20;
        if (moveOutput) {
            tile.tickCounter = 0;
            tile.tryDepositOutput();
            tile.flushEnchantedInput();
            tile.refillEnchantSlot();
        }
        if (tile.advanceAutoEnchantTimer(Apotheosis.enableEnch && level.hasNeighborSignal(pos), ApotheosisConfig.getMechanicalEnchantInterval())) {
            tile.tryAutoEnchant();
        }
        if (moveOutput) tile.tryDepositOutput();
    }

    private boolean advanceAutoEnchantTimer(boolean active, int interval) {
        if (!active) {
            this.autoTickCounter = 0;
            return false;
        }
        if (++this.autoTickCounter < interval) return false;
        this.autoTickCounter = 0;
        return true;
    }

    private int getAutoEnchantLapisCost() {
        return EnchantingCostRules.lapisCost(3, EnchantingDiscounts.gather(this.level, this.worldPosition).lapis());
    }

    static boolean hasAutoEnchantFuel(IItemHandler fuel, int lapisCost) {
        if (lapisCost <= 0) return true;
        ItemStack availableFuel = fuel.extractItem(0, lapisCost, true);
        return availableFuel.getCount() == lapisCost
            && (availableFuel.is(Items.LAPIS_LAZULI) || fuel.isItemValid(0, availableFuel));
    }

    public boolean isMissingAutoEnchantLapis() {
        int lapisCost = this.getAutoEnchantLapisCost();
        return lapisCost > 0 && !hasAutoEnchantFuel(this.getFuelInv(), lapisCost);
    }

    private void tryAutoEnchant() {
        if (!Apotheosis.enableEnch || this.movingInventory || this.isOutputPending()) return;
        this.refillEnchantSlot();
        Container input = this.getActiveEnchantInventory();
        ItemStack stack = input.getItem(0);
        if (stack.getCount() != 1 || !this.canEnchantInput(stack)) return;
        int lapisCost = this.getAutoEnchantLapisCost();
        IItemHandler fuel = this.getFuelInv();
        if (!hasAutoEnchantFuel(fuel, lapisCost)) return;
        ItemStack result = this.doEnchant(stack.copy(), this.ravenStats);
        if (result.isEmpty()) return;
        this.movingInventory = true;
        try {
            if (lapisCost > 0) fuel.extractItem(0, lapisCost, false);
            this.completedWorkStack = result;
            input.setItem(0, result);
            this.setChanged();
        } finally {
            this.movingInventory = false;
        }
        this.flushEnchantedInput();
        this.refillEnchantSlot();
    }

    /** 核心附魔逻辑：输入物品，返回已附魔的物品 */
    public ItemStack doEnchant(ItemStack input, RavenTableStats stats) {
        if (input.isEmpty()) return ItemStack.EMPTY;

        float eterna = stats.eterna();
        float quanta = stats.quanta();
        float arcana = stats.arcana();
        var recipe = EnchantingRecipe.findMatch(this.level, input, eterna, quanta, arcana);

        if (recipe != null) {
            input = recipe.assemble(input, eterna, quanta, arcana);
            this.enchantmentSeed = this.level.random.nextInt();
            return input;
        }

        if (!input.isEnchantable() || input.getEnchantmentValue() <= 0 || input.getItem() instanceof EnderLeadAccess) return ItemStack.EMPTY;

        var bs = dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.gatherStats(this.level, this.worldPosition, input.getEnchantmentValue());
        eterna = Math.max(1.5F, eterna);
        var random = RandomSource.create((int) this.enchantmentSeed);
        int level = RealEnchantmentHelper.getEnchantmentCost(random, 2, eterna, input);
        if (level < 3) level++;
        level = ForgeEventFactory.onEnchantmentLevelSet(this.level, this.worldPosition, 2, Math.round(eterna), input, level);
        if (level <= 0) return ItemStack.EMPTY;
        random.setSeed((int) this.enchantmentSeed + 2);
        var list = RealEnchantmentHelper.selectEnchantment(
            random, input, level, quanta, arcana, bs.rectification(), bs.treasure(), bs.blacklist());
        if (list.isEmpty()) return ItemStack.EMPTY;
        input = ((dev.shadowsoffire.apotheosis.ench.table.IEnchantableItem) input.getItem()).onEnchantment(input, list);
        if (input.isEmpty()) return ItemStack.EMPTY;
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
        tag.putBoolean("ench_completed", this.isOutputPending());
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
        if (tag.contains("ench_slot")) {
            if ((Object) this instanceof EasyMagicEnchantingStorage storage
                && EasyMagicCompat.isLoaded() && !this.getSavedEnchantSlot().isEmpty()
                && storage.getEasyMagicInventory().getItem(0).isEmpty()) {
                ItemStack saved = this.enchantInventory.removeItemNoUpdate(0);
                storage.getEasyMagicInventory().setItem(0, saved);
            }
        }
        this.completedWorkStack = tag.getBoolean("ench_completed") ? this.getActiveEnchantInventory().getItem(0) : ItemStack.EMPTY;
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
