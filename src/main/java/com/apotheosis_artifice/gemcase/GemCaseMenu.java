package com.apotheosis_artifice.gemcase;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.menu.BlockEntityMenu;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GemCaseMenu extends BlockEntityMenu<GemCaseTile> {

    public static final int INPUT_SLOT = 0;
    public static final int FILTER_SLOT = 1;
    public static final int FIRST_GEM_SLOT = 2;
    public static final int FIRST_UPGRADE_MAT_SLOT = 8;
    public static final int UPGRADE_MAT_COUNT = 6;

    public static final ResourceLocation NONE_RARITY = new ResourceLocation("apotheosis_artifice", "nonexistent");

    protected final Player player;
    protected SimpleContainer ioInv = new SimpleContainer(2) {
        @Override public int getMaxStackSize() {
            return tile instanceof com.apotheosis_artifice.gemcase.GemCaseTile.AdvancedGemCaseTile ? Integer.MAX_VALUE : 64;
        }
    };
    public final net.minecraft.world.Container upgradeMatInv;
    private final List<ItemStack> lastMaterials = new ArrayList<>();
    protected List<ResourceLocation> rarityOrder = new ArrayList<>();
    @Nullable
    protected Gem selectedGem = null;
    protected Runnable notifier = null;

    public GemCaseMenu(int id, Inventory inv, BlockPos pos) {
        super(ApotheosisArtificeMod.GEM_CASE_MENU.get(), id, inv, pos);
        this.player = inv.player;
        this.upgradeMatInv = new net.minecraftforge.items.wrapper.RecipeWrapper(this.tile.upgradeMatInv) {
            @Override public int getMaxStackSize() { return tile.upgradeMatInv.getSlotLimit(0); }
            @Override public void setChanged() { tile.materialsChanged(); }
        };
        this.tile.addListener(this);
        buildRarityOrder();
        addInputSlot();
        addFilterSlot();
        addGemSlots();
        addUpgradeMatSlots();
        this.addPlayerSlots(inv, 8, 148);
        registerTransferRules();
    }

    private void buildRarityOrder() {
        for (DynamicHolder<LootRarity> holder : RarityRegistry.INSTANCE.getOrderedRarities()) {
            rarityOrder.add(RarityRegistry.INSTANCE.getKey(holder.get()));
        }
    }

    private void addInputSlot() {
        this.addSlot(new Slot(this.ioInv, 0, 142, 99) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // adventure 模块禁用时 GemItem.getGem 会触发 Adventure$Items 的失败初始化并崩溃
                if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return false;
                return GemItem.getGem(stack).isBound();
            }
            @Override
            public int getMaxStackSize() {
                return GemCaseMenu.this.tile instanceof com.apotheosis_artifice.gemcase.GemCaseTile.AdvancedGemCaseTile ? Integer.MAX_VALUE : 64;
            }
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return getMaxStackSize();
            }
            @Override
            public void setChanged() {
                super.setChanged();
                ItemStack inSlot = this.getItem();
                if (!inSlot.isEmpty()) {
                    int before = inSlot.getCount();
                    GemCaseMenu.this.tile.depositGem(inSlot); // 原地 shrink，超容部分保留
                    if (inSlot.getCount() != before) {
                        GemCaseMenu.this.player.level().playSound(GemCaseMenu.this.player, GemCaseMenu.this.pos,
                            SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.NEUTRAL, 0.5F, 0.7F);
                    }
                    if (inSlot.isEmpty()) {
                        GemCaseMenu.this.ioInv.setItem(0, ItemStack.EMPTY);
                    }
                }
            }
        });
    }

    private void addFilterSlot() {
        this.addSlot(new Slot(this.ioInv, 1, 142, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !LootCategory.forItem(stack).isNone();
            }
            @Override
            public int getMaxStackSize() { return 1; }
            @Override
            public void setChanged() {
                GemCaseMenu.this.onChanged();
            }
        });
    }

    private void addGemSlots() {
        for (int i = 0; i < 6; i++) {
            this.addSlot(new GemCaseSlot(this, i < rarityOrder.size() ? rarityOrder.get(i) : NONE_RARITY, 21 + i * 18, 91));
        }
    }

    private void addUpgradeMatSlots() {
        for (int i = 0; i < UPGRADE_MAT_COUNT; i++) {
            this.addSlot(new Slot(this.upgradeMatInv, i, -45 + 18 * (i % 2), 37 + 18 * (i / 2)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GemCaseMenu.this.isValidUpgradeMaterial(stack);
                }
                @Override
                public int getMaxStackSize() { return tile.upgradeMatInv.getSlotLimit(0); }
                @Override
                public int getMaxStackSize(ItemStack stack) { return getMaxStackSize(); }
                @Override
                public ItemStack safeInsert(ItemStack stack, int increment) {
                    if (stack.isEmpty() || !this.mayPlace(stack)) return stack;
                    if (!this.getItem().isEmpty() && !ItemStack.isSameItemSameTags(stack, this.getItem())) return stack;
                    int max = Math.min(this.getMaxStackSize(stack), this.container.getMaxStackSize());
                    int count = this.getItem().isEmpty() ? 0 : this.getItem().getCount();
                    int space = max - count;
                    if (space <= 0) return stack;
                    int transfer = Math.min(Math.min(stack.getCount(), increment), space);
                    if (count > 0) {
                        this.getItem().grow(transfer);
                        stack.shrink(transfer);
                    } else {
                        this.set(stack.split(transfer));
                    }
                    this.setChanged();
                    return stack;
                }
                @Override
                public void setChanged() {
                    super.setChanged();
                    GemCaseMenu.this.onChanged();
                }
            });
        }
    }

    private void registerTransferRules() {
        this.mover.registerRule((stack, slot) -> slot == FILTER_SLOT, this.playerInvStart, this.slots.size());
        this.mover.registerRule((stack, slot) -> slot >= FIRST_GEM_SLOT && slot < FIRST_UPGRADE_MAT_SLOT, this.playerInvStart, this.slots.size());
        this.mover.registerRule((stack, slot) -> slot >= FIRST_UPGRADE_MAT_SLOT && slot < FIRST_UPGRADE_MAT_SLOT + UPGRADE_MAT_COUNT, this.playerInvStart, this.slots.size());
        this.mover.registerRule((stack, slot) -> slot >= this.playerInvStart
            && dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure && GemItem.getGem(stack).isBound(), INPUT_SLOT, INPUT_SLOT + 1);
        this.mover.registerRule((stack, slot) -> slot >= this.playerInvStart && this.isValidUpgradeMaterial(stack), FIRST_UPGRADE_MAT_SLOT, FIRST_UPGRADE_MAT_SLOT + UPGRADE_MAT_COUNT);
        this.mover.registerRule((stack, slot) -> slot >= this.playerInvStart && !LootCategory.forItem(stack).isNone(), FILTER_SLOT, FILTER_SLOT + 1);
        this.registerInvShuffleRules();
    }

    public void setNotifier(Runnable r) {
        this.notifier = r;
    }

    public void onChanged() {
        if (this.notifier != null) this.notifier.run();
    }

    public void setSelectedGem(@Nullable Gem gem) {
        this.selectedGem = gem;
        if (this.notifier != null) this.notifier.run();
    }

    public void setSelectedGemFromServer(String gemId) {
        ResourceLocation id = ResourceLocation.tryParse(gemId);
        this.selectedGem = null;
        if (id != null) {
            DynamicHolder<Gem> holder = GemRegistry.INSTANCE.holder(id);
            this.selectedGem = holder.isBound() ? holder.get() : null;
        }
    }

    public void setPage(int page) {
        List<ResourceLocation> order = this.rarityOrder;
        // page 来自网络包，必须钳制下界与上界，否则负 page → order.get(负) 服务端越界崩溃。
        int maxPage = order.size() <= 6 ? 0 : (order.size() - 2) / 5;
        page = Math.max(0, Math.min(page, maxPage));
        int offset = page * 5;
        for (int i = 0; i < 6; i++) {
            Slot s = this.slots.get(FIRST_GEM_SLOT + i);
            if (s instanceof GemCaseSlot gs) {
                int idx = offset + i;
                gs.rarityId = (idx >= 0 && idx < order.size()) ? order.get(idx) : NONE_RARITY;
            }
        }
    }

    @Nullable
    public Gem getSelectedGem() {
        return this.selectedGem;
    }

    public List<ResourceLocation> getRarityOrder() {
        return this.rarityOrder;
    }

    public int getGemCount(ResourceLocation gemId, ResourceLocation rarityId) {
        return this.tile.getCount(gemId, rarityId);
    }

    public int getGemCount(Gem gem, ResourceLocation rarityId) {
        return this.tile.getCount(gem.getId(), rarityId);
    }

    public long getGemCount(Gem gem) {
        long total = 0;
        for (ResourceLocation rid : this.rarityOrder) {
            total += this.tile.getCount(gem.getId(), rid);
        }
        return total;
    }

    public ItemStack extractGem(ResourceLocation rarityId, int count) {
        if (this.selectedGem == null) return ItemStack.EMPTY;
        return this.tile.extractGem(this.selectedGem.getId(), rarityId, count);
    }

    public boolean isValidUpgradeMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return false;
        if (stack.getItem() == dev.shadowsoffire.apotheosis.adventure.Adventure.Items.GEM_DUST.get()) return true;
        for (LootRarity rarity : RarityRegistry.INSTANCE.getValues()) {
            if (stack.is(rarity.getMaterial())) return true;
        }
        return false;
    }

    @Nullable
    public GemCaseTile.RarityUpgradeMatch getUpgradeMatch(ResourceLocation currentRarityId) {
        if (this.selectedGem == null) return null;
        return this.tile.getUpgradeMatch(this.selectedGem.getId(), currentRarityId, this.upgradeMatInv);
    }

    public boolean handleUpgradeClick(int rarityOrdinal, boolean shift, int page) {
        if (this.selectedGem == null) return false;
        int maxPage = this.rarityOrder.size() <= 6 ? 0 : (this.rarityOrder.size() - 2) / 5;
        if (page < 0 || page > maxPage || rarityOrdinal < 1 || rarityOrdinal > 5) return false;
        int sourceIdx = page * 5 + rarityOrdinal - 1;
        if (sourceIdx < 0 || sourceIdx >= this.rarityOrder.size()) return false;
        ResourceLocation currentId = this.rarityOrder.get(sourceIdx);
        if (currentId == null) return false;

        DynamicHolder<LootRarity> holder = RarityRegistry.INSTANCE.holder(currentId);
        if (!holder.isBound()) return false;
        if (holder.get().next().ordinal() == holder.get().ordinal()) return false;

        int tries = shift ? 64 : 1;
        boolean any = false;

        while (tries-- > 0) {
            boolean result = this.tile.upgradeGem(this.selectedGem.getId(), currentId, this.upgradeMatInv);
            if (!result) break;
            any = true;
            this.level.playSound(null, this.pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1, 1.5F + 0.35F * (1 - 2 * this.level.getRandom().nextFloat()));
        }

        return any;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        Slot slot = slotId >= 0 && slotId < this.slots.size() ? this.getSlot(slotId) : null;
        if (slot != null && slot.index >= FIRST_UPGRADE_MAT_SLOT && slot.index < FIRST_UPGRADE_MAT_SLOT + UPGRADE_MAT_COUNT
            && slot.getItem().getCount() > slot.getItem().getMaxStackSize()) {
            if (clickType == ClickType.SWAP) return;
            if (clickType == ClickType.PICKUP && !this.getCarried().isEmpty()
                && !ItemStack.isSameItemSameTags(this.getCarried(), slot.getItem())) return;
            if (clickType == ClickType.THROW) {
                if (!player.level().isClientSide && this.getCarried().isEmpty()) {
                    player.drop(slot.remove(dragType == 0 ? 1 : slot.getItem().getMaxStackSize()), true);
                    slot.setChanged();
                }
                return;
            }
        }
        if (slot instanceof GemCaseSlot gs && clickType == ClickType.PICKUP && this.getCarried().isEmpty()) {
            if (!player.level().isClientSide && this.selectedGem != null) {
                int stored = this.tile.getCount(this.selectedGem.getId(), gs.rarityId);
                if (stored > 0) {
                    int amount = dragType == 0 ? Math.min(stored, 64) : Math.min(stored, 32);
                    ItemStack extracted = this.extractGem(gs.rarityId, amount);
                    if (!extracted.isEmpty()) this.setCarried(extracted);
                }
            }
            return;
        }
        if (slot != null && slot.index >= FIRST_UPGRADE_MAT_SLOT && slot.index < FIRST_UPGRADE_MAT_SLOT + UPGRADE_MAT_COUNT
            && clickType == ClickType.PICKUP && this.getCarried().isEmpty()) {
            if (!player.level().isClientSide) {
                ItemStack stack = this.upgradeMatInv.getItem(slot.getSlotIndex());
                if (!stack.isEmpty()) {
                    int half = (Math.min(stack.getCount(), 64) + 1) / 2;
                    int amount = dragType == 0 ? Math.min(stack.getCount(), 64) : half;
                    this.setCarried(stack.split(amount));
                    this.upgradeMatInv.setChanged();
                }
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.tile.removeListener(this);
        if (!this.level.isClientSide) {
            this.tile.setChanged();
            this.clearContainer(player, this.ioInv);
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        this.syncMaterials();
    }

    @Override
    public void sendAllDataToRemote() {
        super.sendAllDataToRemote();
        this.lastMaterials.clear();
        this.syncMaterials();
    }

    private void syncMaterials() {
        if (!(this.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
        List<ItemStack> materials = new ArrayList<>();
        boolean changed = this.lastMaterials.size() != UPGRADE_MAT_COUNT;
        for (int i = 0; i < UPGRADE_MAT_COUNT; i++) {
            ItemStack stack = this.upgradeMatInv.getItem(i);
            changed |= i >= this.lastMaterials.size() || !ItemStack.matches(stack, this.lastMaterials.get(i));
            materials.add(stack.copy());
        }
        if (changed) {
            this.lastMaterials.clear();
            this.lastMaterials.addAll(materials);
            com.apotheosis_artifice.ApotheosisNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                new com.apotheosis_artifice.ApotheosisNetwork.SyncGemCaseMaterialsPacket(this.containerId, materials));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(this.pos.getX(), this.pos.getY(), this.pos.getZ()) < 16 * 16 && this.tile != null && !this.tile.isRemoved();
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        Slot slot = this.getSlot(pIndex);
        if (slot instanceof GemCaseSlot gs) {
            if (this.selectedGem != null) {
                int stored = this.tile.getCount(this.selectedGem.getId(), gs.rarityId);
                if (stored > 0) {
                    ItemStack toMove = this.extractGem(gs.rarityId, Math.min(stored, 64));
                    if (!toMove.isEmpty()) {
                        this.moveItemStackTo(toMove, this.playerInvStart, this.slots.size(), false);
                        if (!toMove.isEmpty()) {
                            this.tile.depositGem(toMove);
                        }
                    }
                }
            return ItemStack.EMPTY;
        }
        }
        if (slot.index >= FIRST_UPGRADE_MAT_SLOT && slot.index < FIRST_UPGRADE_MAT_SLOT + UPGRADE_MAT_COUNT) {
            ItemStack stack = this.upgradeMatInv.getItem(slot.getSlotIndex());
            if (!stack.isEmpty()) {
                int toTake = Math.min(stack.getCount(), 64);
                ItemStack toMove = stack.split(toTake);
                this.upgradeMatInv.setChanged();
                this.moveItemStackTo(toMove, this.playerInvStart, this.slots.size(), false);
                if (!toMove.isEmpty()) {
                    stack.grow(toMove.getCount());
                    this.upgradeMatInv.setChanged();
                }
            }
            return ItemStack.EMPTY;
        }
        if (slot.index >= this.playerInvStart) {
            int fromSlot = slot.index;
            ItemStack stack = slot.getItem().copy();
            if (!stack.isEmpty() && this.isValidUpgradeMaterial(stack)) {
                for (int si = 0; si < UPGRADE_MAT_COUNT; si++) {
                    Slot matSlot = this.getSlot(FIRST_UPGRADE_MAT_SLOT + si);
                    if (!matSlot.mayPlace(stack)) continue;
                    int limit = this.upgradeMatInv.getMaxStackSize();
                    ItemStack matStack = this.upgradeMatInv.getItem(si);
                    if (!matStack.isEmpty()) {
                        if (!ItemStack.isSameItemSameTags(stack, matStack)) continue;
                        int space = limit - matStack.getCount();
                        if (space <= 0) continue;
                        int add = Math.min(stack.getCount(), space);
                        matStack.grow(add);
                        stack.shrink(add);
                        this.upgradeMatInv.setChanged();
                    } else {
                        int add = Math.min(stack.getCount(), limit);
                        this.upgradeMatInv.setItem(si, stack.split(add));
                    }
                    if (stack.isEmpty()) break;
                }
                this.getSlot(fromSlot).set(stack);
                return ItemStack.EMPTY;
            }
        }
        return this.mover.quickMoveStack(this, pPlayer, pIndex);
    }
}
