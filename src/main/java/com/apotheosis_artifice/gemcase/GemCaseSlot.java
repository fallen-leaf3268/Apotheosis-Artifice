package com.apotheosis_artifice.gemcase;

import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GemCaseSlot extends Slot {

    private static final SimpleContainer EMPTY = new SimpleContainer(0);
    private final GemCaseMenu menu;
    public ResourceLocation rarityId;

    public GemCaseSlot(GemCaseMenu menu, ResourceLocation rarityId, int x, int y) {
        super(EMPTY, -1, x, y);
        this.menu = menu;
        this.rarityId = rarityId;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.hasItem();
    }

    @Override
    public ItemStack getItem() {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return ItemStack.EMPTY;
        Gem gem = this.menu.getSelectedGem();
        if (gem == null) return ItemStack.EMPTY;
        int count = this.menu.getGemCount(gem, this.rarityId);
        if (count <= 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
        GemItem.setGem(stack, gem);
        DynamicHolder<LootRarity> rarity = RarityRegistry.INSTANCE.holder(this.rarityId);
        if (rarity.isBound()) AffixHelper.setRarity(stack, rarity.get());
        stack.setCount(1);
        return stack;
    }

    @Override
    public boolean hasItem() {
        Gem gem = this.menu.getSelectedGem();
        return gem != null && this.menu.getGemCount(gem, this.rarityId) > 0;
    }

    @Override
    public ItemStack remove(int amount) {
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) return ItemStack.EMPTY;
        Gem gem = this.menu.getSelectedGem();
        if (gem == null) return ItemStack.EMPTY;
        int count = this.menu.getGemCount(gem, this.rarityId);
        int toExtract = Math.min(count, amount);
        if (toExtract <= 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
        GemItem.setGem(stack, gem);
        DynamicHolder<LootRarity> rarity = RarityRegistry.INSTANCE.holder(this.rarityId);
        if (rarity.isBound()) AffixHelper.setRarity(stack, rarity.get());
        stack.setCount(toExtract);
        return stack;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        if (!stack.isEmpty()) {
            this.menu.extractGem(this.rarityId, stack.getCount());
        }
    }

    @Override
    public void setByPlayer(ItemStack stack) {}

    @Override
    public void set(ItemStack stack) {}

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return false;
    }

    @Override
    public boolean allowModification(Player player) {
        return false;
    }

    @Override
    public boolean isActive() {
        Gem gem = this.menu.getSelectedGem();
        if (gem == null) return false;
        DynamicHolder<LootRarity> holder = RarityRegistry.INSTANCE.holder(this.rarityId);
        if (!holder.isBound()) return false;
        LootRarity rarity = holder.get();
        return rarity.isAtLeast(gem.getMinRarity()) && rarity.isAtMost(gem.getMaxRarity());
    }
}
