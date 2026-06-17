package com.apotheosis_artifice.gemcase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

public class GemCaseTile extends BlockEntity implements dev.shadowsoffire.placebo.block_entity.TickingBlockEntity {

    // gemId -> rarityId -> count
    private final Map<ResourceLocation, Map<ResourceLocation, Integer>> gems = new HashMap<>();
    protected final Set<GemCaseMenu> activeContainers = new HashSet<>();
    protected final int maxCount;
    private GemCaseAnimationState animationState;

    public final ItemStackHandler upgradeMatInv;

    public GemCaseTile(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxCount, int materialSlotLimit) {
        super(type, pos, state);
        this.maxCount = maxCount;
        this.upgradeMatInv = new ItemStackHandler(6) {
            @Override
            public int getSlotLimit(int slot) { return materialSlotLimit; }

            @Override
            public CompoundTag serializeNBT() {
                CompoundTag tag = new CompoundTag();
                tag.putInt("Size", this.getSlots());
                CompoundTag items = new CompoundTag();
                for (int i = 0; i < this.getSlots(); i++) {
                    ItemStack stack = this.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putString("id", ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
                        itemTag.putInt("Count", stack.getCount());
                        if (stack.hasTag()) {
                            itemTag.put("tag", stack.getTag());
                        }
                        items.put(String.valueOf(i), itemTag);
                    }
                }
                tag.put("Items", items);
                return tag;
            }

            @Override
            public void deserializeNBT(CompoundTag tag) {
                int size = tag.getInt("Size");
                CompoundTag items = tag.getCompound("Items");
                for (int i = 0; i < size; i++) {
                    String key = String.valueOf(i);
                    if (items.contains(key)) {
                        CompoundTag itemTag = items.getCompound(key);
                        String id = itemTag.getString("id");
                        int count = itemTag.getInt("Count");
                        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)), 1);
                        if (!stack.isEmpty()) {
                            stack.setCount(count);
                            if (itemTag.contains("tag")) {
                                stack.setTag(itemTag.getCompound("tag"));
                            }
                        }
                        this.setStackInSlot(i, stack);
                    } else {
                        this.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
        };
    }

    // ---- 存入 ----

    public void depositGem(ItemStack stack) {
        DynamicHolder<Gem> gem = GemItem.getGem(stack);
        if (!gem.isBound()) return;
        DynamicHolder<LootRarity> rarity = AffixHelper.getRarity(stack);
        if (!rarity.isBound()) return;

        ResourceLocation gemId = gem.getId();
        ResourceLocation rarityId = RarityRegistry.INSTANCE.getKey(rarity.get());

        Map<ResourceLocation, Integer> rarities = this.gems.computeIfAbsent(gemId, k -> autoFillRarities());
        int stored = rarities.getOrDefault(rarityId, 0);
        // 用 long 防 int 溢出；只存放得下的部分，余量留在输入栈里（原地 shrink），避免吞宝石。
        long space = (long) this.maxCount - stored;
        if (space <= 0) return;
        int toStore = (int) Math.min(space, stack.getCount());
        rarities.put(rarityId, stored + toStore);
        stack.shrink(toStore);

        this.setChanged();
        if (!this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.activeContainers.forEach(GemCaseMenu::onChanged);
        }
    }

    private static Map<ResourceLocation, Integer> autoFillRarities() {
        Map<ResourceLocation, Integer> map = new HashMap<>();
        for (LootRarity r : RarityRegistry.INSTANCE.getValues()) {
            map.put(RarityRegistry.INSTANCE.getKey(r), 0);
        }
        return map;
    }

    // ---- 提取 ----

    public ItemStack extractGem(ResourceLocation gemId, ResourceLocation rarityId, int count) {
        Map<ResourceLocation, Integer> rarities = this.gems.get(gemId);
        if (rarities == null) return ItemStack.EMPTY;
        int stored = rarities.getOrDefault(rarityId, 0);
        int extracted = Math.min(count, stored);
        if (extracted <= 0) return ItemStack.EMPTY;

        rarities.put(rarityId, stored - extracted);
        if (rarities.get(rarityId) <= 0) rarities.remove(rarityId);
        if (rarities.isEmpty()) this.gems.remove(gemId);

        DynamicHolder<Gem> gem = GemRegistry.INSTANCE.holder(gemId);
        DynamicHolder<LootRarity> rarity = RarityRegistry.INSTANCE.holder(rarityId);
        if (!gem.isBound()) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
        GemItem.setGem(stack, gem.get());
        if (rarity.isBound()) AffixHelper.setRarity(stack, rarity.get());
        stack.setCount(extracted);

        this.setChanged();
        if (!this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.activeContainers.forEach(GemCaseMenu::onChanged);
        }
        return stack;
    }

    // ---- 升级 ----

    private static final int NEXT_MAT_COST = 1;
    private static final int STD_MAT_COST = 3;
    private static final int PREV_MAT_COST = 9;

    public boolean upgradeGem(ResourceLocation gemId, ResourceLocation currentRarityId, Container matInv) {
        RarityUpgradeMatch match = getUpgradeMatch(gemId, currentRarityId, matInv);
        if (match == null || !match.canUpgrade) return false;

        Map<ResourceLocation, Integer> rarities = this.gems.get(gemId);
        ResourceLocation nextId = RarityRegistry.INSTANCE.getKey(match.nextRarity);

        rarities.put(currentRarityId, rarities.get(currentRarityId) - 2);
        if (rarities.get(currentRarityId) <= 0) rarities.remove(currentRarityId);
        rarities.merge(nextId, 1, Integer::sum);

        int toConsume = match.dustNeeded;
        for (int i = 0; i < matInv.getContainerSize() && toConsume > 0; i++) {
            ItemStack stack = matInv.getItem(i);
            if (stack.getItem() == Adventure.Items.GEM_DUST.get()) {
                int consume = Math.min(stack.getCount(), toConsume);
                stack.shrink(consume);
                toConsume -= consume;
            }
        }

        toConsume = match.matNeeded;
        for (int i = 0; i < matInv.getContainerSize() && toConsume > 0; i++) {
            ItemStack stack = matInv.getItem(i);
            if (!stack.isEmpty() && RarityRegistry.isMaterial(stack.getItem())) {
                DynamicHolder<LootRarity> matRarity = RarityRegistry.getMaterialRarity(stack.getItem());
                if (matRarity.isBound() && matRarity.get() == match.materialRarity) {
                    int consume = Math.min(stack.getCount(), toConsume);
                    stack.shrink(consume);
                    toConsume -= consume;
                }
            }
        }

        this.setChanged();
        if (!this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.activeContainers.forEach(GemCaseMenu::onChanged);
        }
        return true;
    }

    @Nullable
    public RarityUpgradeMatch getUpgradeMatch(ResourceLocation gemId, ResourceLocation currentRarityId, Container matInv) {
        Map<ResourceLocation, Integer> rarities = this.gems.get(gemId);
        if (rarities == null || rarities.getOrDefault(currentRarityId, 0) < 2) return null;

        DynamicHolder<LootRarity> currentHolder = RarityRegistry.INSTANCE.holder(currentRarityId);
        if (!currentHolder.isBound()) return null;
        LootRarity nextRarity = currentHolder.get().next();
        if (nextRarity == currentHolder.get()) return null;
        DynamicHolder<Gem> gemHolder = GemRegistry.INSTANCE.holder(gemId);
        if (gemHolder.isBound() && nextRarity.ordinal() > gemHolder.get().getMaxRarity().ordinal()) return null;

        int dustNeeded = 1 + currentHolder.get().ordinal() * 2;
        int dustFound = 0;
        for (int i = 0; i < matInv.getContainerSize(); i++) {
            if (matInv.getItem(i).getItem() == Adventure.Items.GEM_DUST.get()) {
                dustFound += matInv.getItem(i).getCount();
            }
        }
        if (dustFound < dustNeeded) {
            return new RarityUpgradeMatch(nextRarity, false, dustNeeded, dustFound, null, 0, 0);
        }

        DynamicHolder<LootRarity> nextHolder = RarityRegistry.INSTANCE.holder(RarityRegistry.INSTANCE.getKey(nextRarity));
        DynamicHolder<LootRarity> prevHolder = RarityRegistry.prev(currentHolder);

        int nextMatCount = 0, sameMatCount = 0, prevMatCount = 0;
        for (int i = 0; i < matInv.getContainerSize(); i++) {
            ItemStack stack = matInv.getItem(i);
            if (stack.isEmpty() || !RarityRegistry.isMaterial(stack.getItem())) continue;
            DynamicHolder<LootRarity> matRarity = RarityRegistry.getMaterialRarity(stack.getItem());
            if (!matRarity.isBound()) continue;
            if (matRarity == nextHolder) nextMatCount += stack.getCount();
            else if (matRarity == currentHolder) sameMatCount += stack.getCount();
            else if (prevHolder.isBound() && matRarity == prevHolder) prevMatCount += stack.getCount();
        }

        int matNeeded = 0;
        LootRarity materialRarity = null;
        if (nextMatCount >= NEXT_MAT_COST) {
            matNeeded = NEXT_MAT_COST;
            materialRarity = nextRarity;
        }
        else if (sameMatCount >= STD_MAT_COST) {
            matNeeded = STD_MAT_COST;
            materialRarity = currentHolder.get();
        }
        else if (prevHolder.isBound() && prevMatCount >= PREV_MAT_COST) {
            matNeeded = PREV_MAT_COST;
            materialRarity = prevHolder.get();
        }
        else {
            return new RarityUpgradeMatch(nextRarity, false, dustNeeded, dustFound, null, 0, 0);
        }

        return new RarityUpgradeMatch(nextRarity, true, dustNeeded, dustFound, materialRarity, matNeeded, matNeeded);
    }

    public record RarityUpgradeMatch(
        LootRarity nextRarity,
        boolean canUpgrade,
        int dustNeeded, int dustFound,
        @Nullable LootRarity materialRarity,
        int matNeeded, int matFound
    ) {
        public boolean hasDust() { return dustFound >= dustNeeded; }
        public boolean hasMaterial() { return materialRarity != null && matFound >= matNeeded; }
    }

    // ---- 查询 ----

    public int getCount(ResourceLocation gemId, ResourceLocation rarityId) {
        Map<ResourceLocation, Integer> rarities = this.gems.get(gemId);
        if (rarities == null) return 0;
        return rarities.getOrDefault(rarityId, 0);
    }

    public int getCount(Gem gem, ResourceLocation rarityId) {
        return getCount(gem.getId(), rarityId);
    }

    public GemCaseAnimationState getAnimationState() {
        if (this.animationState == null) {
            this.animationState = new GemCaseAnimationState(new java.util.Random());
        }
        return this.animationState;
    }

    public Map<ResourceLocation, Map<ResourceLocation, Integer>> getGemMap() {
        return this.gems;
    }

    // ---- 监听器 ----

    public void addListener(GemCaseMenu menu) {
        this.activeContainers.add(menu);
    }

    public void removeListener(GemCaseMenu menu) {
        this.activeContainers.remove(menu);
    }

    // ---- NBT ----

    public void saveGemData(CompoundTag tag) {
        CompoundTag gemsTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, Integer>> e : this.gems.entrySet()) {
            CompoundTag rarityTag = new CompoundTag();
            for (Map.Entry<ResourceLocation, Integer> re : e.getValue().entrySet()) {
                if (re.getValue() > 0) {
                    rarityTag.putInt(re.getKey().toString(), re.getValue());
                }
            }
            if (!rarityTag.isEmpty()) {
                gemsTag.put(e.getKey().toString(), rarityTag);
            }
        }
        tag.put("gems", gemsTag);
        tag.put("upgrade_mats", this.upgradeMatInv.serializeNBT());
    }

    public void loadGemData(CompoundTag tag) {
        this.gems.clear();
        CompoundTag gemsTag = tag.getCompound("gems");
        for (String gemKey : gemsTag.getAllKeys()) {
            ResourceLocation gemId = ResourceLocation.tryParse(gemKey);
            if (gemId == null) continue;
            CompoundTag rarityTag = gemsTag.getCompound(gemKey);
            Map<ResourceLocation, Integer> rarities = autoFillRarities();
            for (String rarityKey : rarityTag.getAllKeys()) {
                int count = rarityTag.getInt(rarityKey);
                if (count > 0) {
                    ResourceLocation rarityId = ResourceLocation.tryParse(rarityKey);
                    if (rarityId != null) rarities.put(rarityId, count);
                }
            }
            this.gems.put(gemId, rarities);
        }
        if (tag.contains("upgrade_mats")) {
            this.upgradeMatInv.deserializeNBT(tag.getCompound("upgrade_mats"));
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.saveGemData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.loadGemData(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        this.saveGemData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.loadGemData(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.loadGemData(tag);
            for (GemCaseMenu ctr : this.activeContainers) {
                ctr.onChanged();
                ctr.broadcastChanges();
            }
        }
    }

    @Override
    public void clientTick(Level level, BlockPos pos, BlockState state) {
        GemCaseAnimationState animState = this.getAnimationState();
        int uniqueGems = 0;
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, Integer>> e : this.gems.entrySet()) {
            int total = 0;
            for (int count : e.getValue().values()) total += count;
            if (total > 0) uniqueGems++;
        }
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, false);
        animState.tick(Math.min(uniqueGems, 16), player != null);
    }

    public static class BasicGemCaseTile extends GemCaseTile {
        public BasicGemCaseTile(BlockPos pos, BlockState state) {
            super(ApotheosisArtificeMod.GEM_CASE_TILE.get(), pos, state, Short.MAX_VALUE, 64);
        }
    }

    public static class AdvancedGemCaseTile extends GemCaseTile {
        public AdvancedGemCaseTile(BlockPos pos, BlockState state) {
            super(ApotheosisArtificeMod.ENDER_GEM_CASE_TILE.get(), pos, state, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
    }
}
