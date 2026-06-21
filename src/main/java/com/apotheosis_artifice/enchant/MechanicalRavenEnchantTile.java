package com.apotheosis_artifice.enchant;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
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
    private ItemStack savedEnchantSlot = ItemStack.EMPTY;

    public ItemStack getSavedEnchantSlot() { return savedEnchantSlot; }
    public void setSavedEnchantSlot(ItemStack stack) { this.savedEnchantSlot = stack.copy(); setChanged(); }

    private final ItemStackHandler ioInv = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == INPUT) return !stack.isEmpty() && stack.getItem().getEnchantmentValue() > 0;
            return true;
        }
    };

    private LazyOptional<IItemHandler> ioCap = LazyOptional.of(() -> new IItemHandler() {
        private int fuelSlots() { return inv != null ? inv.getSlots() : 0; }
        @Override public int getSlots() { return 3; }
        @Override public ItemStack getStackInSlot(int slot) {
            if (slot == 0) return ioInv.getStackInSlot(0);
            if (slot == 1) return inv != null && fuelSlots() > 0 ? inv.getStackInSlot(0) : ItemStack.EMPTY;
            if (slot == 2) return ioInv.getStackInSlot(1);
            return ItemStack.EMPTY;
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return stack;
            if (slot == 0) return ioInv.insertItem(0, stack, simulate);
            if (slot == 1 && stack.getItem() == Items.LAPIS_LAZULI && inv != null && fuelSlots() > 0)
                return inv.insertItem(0, stack, simulate);
            return stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 2) return ioInv.extractItem(1, amount, simulate);
            return ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return !stack.isEmpty() && stack.getItem().getEnchantmentValue() > 0;
            if (slot == 1) return stack.getItem() == Items.LAPIS_LAZULI;
            return false;
        }
    });
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
    public boolean depositDirectToBound(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (libBound && libDim != null && stack.getItem() == Items.ENCHANTED_BOOK) {
            if (this.level == null || this.level.getServer() == null) return false;
            var dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, libDim);
            var libLevel = this.level.getServer().getLevel(dimKey);
            if (libLevel == null) return false;
            var be = libLevel.getBlockEntity(new BlockPos(libX, libY, libZ));
            if (!(be instanceof dev.shadowsoffire.apotheosis.ench.library.EnchLibraryTile lib)) return false;
            int count = stack.getCount();
            for (int c = 0; c < count; c++) {
                ItemStack single = stack.copy(); single.setCount(1);
                lib.depositBook(single);
            }
            return true;
        }
        if (contBound && contDim != null) {
            if (this.level == null || this.level.getServer() == null) return false;
            var dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, contDim);
            var contLevel = this.level.getServer().getLevel(dimKey);
            if (contLevel == null) return false;
            var be = contLevel.getBlockEntity(new BlockPos(contX, contY, contZ));
            if (be == null) return false;
            var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
            if (cap.isEmpty()) return false;
            IItemHandler handler = cap.get();
            ItemStack remaining = stack.copy();
            for (int s = 0; s < handler.getSlots() && !remaining.isEmpty(); s++)
                remaining = ItemHandlerHelper.insertItem(handler, remaining, false);
            return remaining.getCount() < stack.getCount();
        }
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MechanicalRavenEnchantTile tile) {
        if (level.isClientSide) return;
        if (++tile.tickCounter < 20) return;
        tile.tickCounter = 0;
        if (level.hasNeighborSignal(pos)) {
            if (level.players().stream().noneMatch(p -> p.containerMenu instanceof MechanicalRavenEnchantMenu)) {
                tile.tryAutoEnchant();
                tile.tryAutoEnchantSaved();
            }
        }
        tile.tryDepositOutput();
    }

    private void tryAutoEnchant() {
        ItemStack input = ioInv.getStackInSlot(INPUT);
        ApotheosisArtificeMod.LOGGER.info("[TE] tryAutoEnchant input={}", input.isEmpty() ? "EMPTY" : input.getHoverName().getString());
        if (input.isEmpty() || input.isEnchanted() || input.getItem().getEnchantmentValue() <= 0) return;
        ItemStack outNow = ioInv.getStackInSlot(OUTPUT);
        if (!outNow.isEmpty() && outNow.getCount() >= outNow.getMaxStackSize()) { ApotheosisArtificeMod.LOGGER.info("[TE] tryAutoEnchant output FULL"); return; }

        ItemStack toEnchant = ioInv.extractItem(INPUT, 1, false);
        if (toEnchant.isEmpty()) return;
        ApotheosisArtificeMod.LOGGER.info("[TE] tryAutoEnchant extracted {}", toEnchant.getHoverName().getString());

        ItemStack result = doEnchant(toEnchant, this.ravenStats);
        if (result.isEmpty()) { ioInv.insertItem(INPUT, toEnchant, false); ApotheosisArtificeMod.LOGGER.info("[TE] tryAutoEnchant FAILED, returned"); return; }
        ApotheosisArtificeMod.LOGGER.info("[TE] tryAutoEnchant SUCCESS {}", result.getHoverName().getString());

        // 优先直接存入绑定容器/图书馆
        if (!depositDirectToBound(result)) {
            ItemStack output = ioInv.getStackInSlot(OUTPUT);
            if (output.isEmpty()) {
                ioInv.setStackInSlot(OUTPUT, result);
            } else if (ItemStack.isSameItemSameTags(output, result) && output.getCount() < output.getMaxStackSize()) {
                int added = Math.min(result.getCount(), output.getMaxStackSize() - output.getCount());
                output.grow(added);
                int remaining = result.getCount() - added;
                if (remaining > 0) { result.setCount(remaining); ioInv.insertItem(INPUT, result, false); }
            } else {
                ioInv.insertItem(INPUT, result, false); return;
            }
        }
        setChanged();
    }

    /** 关闭菜单后对附魔槽残留物品进行附魔并输出 */
    private void tryAutoEnchantSaved() {
        ItemStack saved = this.savedEnchantSlot;
        if (saved.isEmpty() || saved.isEnchanted() || saved.getItem().getEnchantmentValue() <= 0) return;
        ItemStack result = doEnchant(saved, this.ravenStats);
        if (result.isEmpty()) return;
        if (!depositDirectToBound(result)) {
            ItemStack output = ioInv.getStackInSlot(OUTPUT);
            if (output.isEmpty()) {
                ioInv.setStackInSlot(OUTPUT, result);
            } else if (ItemStack.isSameItemSameTags(output, result) && output.getCount() < output.getMaxStackSize()) {
                int added = Math.min(result.getCount(), output.getMaxStackSize() - output.getCount());
                output.grow(added);
                int remaining = result.getCount() - added;
                if (remaining > 0) { result.setCount(remaining); this.savedEnchantSlot = result; return; }
            } else {
                this.savedEnchantSlot = result; return;
            }
        }
        this.savedEnchantSlot = ItemStack.EMPTY;
        setChanged();
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
        if (libBound && libDim != null && output.getItem() == Items.ENCHANTED_BOOK) {
            depositToLibrary(output); return;
        }
        if (contBound && contDim != null) depositToContainer(output);
    }

    private void depositToLibrary(ItemStack output) {
        if (this.level == null || this.level.getServer() == null) return;
        var dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, libDim);
        var libLevel = this.level.getServer().getLevel(dimKey);
        if (libLevel == null) return;
        var be = libLevel.getBlockEntity(new BlockPos(libX, libY, libZ));
        if (!(be instanceof dev.shadowsoffire.apotheosis.ench.library.EnchLibraryTile lib)) return;
        ItemStack books = ioInv.extractItem(OUTPUT, output.getCount(), false);
        int count = books.getCount();
        for (int c = 0; c < count; c++) {
            ItemStack single = books.copy(); single.setCount(1);
            lib.depositBook(single);
        }
        setChanged();
    }

    private void depositToContainer(ItemStack output) {
        if (this.level == null || this.level.getServer() == null) return;
        var dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, contDim);
        var contLevel = this.level.getServer().getLevel(dimKey);
        if (contLevel == null) return;
        var be = contLevel.getBlockEntity(new BlockPos(contX, contY, contZ));
        if (be == null) return;
        var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (cap.isEmpty()) return;
        IItemHandler handler = cap.get();
        ItemStack toInsert = output.copy();
        for (int s = 0; s < handler.getSlots() && !toInsert.isEmpty(); s++)
            toInsert = ItemHandlerHelper.insertItem(handler, toInsert, false);
        int inserted = output.getCount() - toInsert.getCount();
        if (inserted > 0) { ioInv.extractItem(OUTPUT, inserted, false); setChanged(); }
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return ioCap.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); ioCap.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        ioCap = LazyOptional.of(() -> new IItemHandler() {
            private int f() { return inv != null ? inv.getSlots() : 0; }
            @Override public int getSlots() { return 3; }
            @Override public ItemStack getStackInSlot(int s) {
                if (s == 0) return ioInv.getStackInSlot(0);
                if (s == 1) return inv != null && f() > 0 ? inv.getStackInSlot(0) : ItemStack.EMPTY;
                if (s == 2) return ioInv.getStackInSlot(1); return ItemStack.EMPTY;
            }
            @Override public ItemStack insertItem(int s, ItemStack stack, boolean sim) {
                if (stack.isEmpty()) return stack;
                if (s == 0) return ioInv.insertItem(0, stack, sim);
                if (s == 1 && stack.getItem() == Items.LAPIS_LAZULI && inv != null && f() > 0) return inv.insertItem(0, stack, sim);
                return stack;
            }
            @Override public ItemStack extractItem(int s, int a, boolean sim) {
                if (s == 2) return ioInv.extractItem(1, a, sim); return ItemStack.EMPTY;
            }
            @Override public int getSlotLimit(int s) { return 64; }
            @Override public boolean isItemValid(int s, ItemStack stack) {
                if (s == 0) return !stack.isEmpty() && stack.getItem().getEnchantmentValue() > 0;
                if (s == 1) return stack.getItem() == Items.LAPIS_LAZULI; return false;
            }
        });
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("io_inv", ioInv.serializeNBT()); tag.putLong("ench_seed", this.enchantmentSeed);
        if (!savedEnchantSlot.isEmpty()) {
            tag.put("ench_slot", savedEnchantSlot.save(new net.minecraft.nbt.CompoundTag()));
        }
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
        if (tag.contains("ench_slot")) {
            this.savedEnchantSlot = ItemStack.of(tag.getCompound("ench_slot"));
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
