package com.apotheosis_artifice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicates;

import dev.shadowsoffire.apotheosis.Apoth.RecipeTypes;
import dev.shadowsoffire.apotheosis.adventure.affix.salvaging.SalvagingRecipe;
import dev.shadowsoffire.apotheosis.adventure.affix.salvaging.SalvagingRecipe.OutputData;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import dev.shadowsoffire.placebo.menu.FilteredSlot;
import dev.shadowsoffire.placebo.menu.PlaceboContainerMenu;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class PortableSalvagingMenu extends PlaceboContainerMenu {

    protected final Player player;
    protected final InternalItemHandler inputInv = new InternalItemHandler(12);
    protected final InternalItemHandler outputInv = new InternalItemHandler(6);

    public PortableSalvagingMenu(int id, Inventory inv) {
        super(ApotheosisArtificeMod.PORTABLE_SALVAGING_MENU.get(), id, inv);
        this.player = inv.player;

        int leftOffset = 17, topOffset = 17;
        for (int i = 0; i < 12; i++) {
            this.addSlot(new UpdatingSlot(this.inputInv, i,
                leftOffset + i % 4 * 19, topOffset + i / 4 * 19,
                s -> findMatch(this.level, s) != null) {
                @Override public int getMaxStackSize() { return 1; }
            });
        }

        for (int i = 0; i < 6; i++) {
            this.addSlot(new FilteredSlot(this.outputInv, i, 124 + i % 2 * 19, 17 + i / 2 * 19, Predicates.alwaysFalse()));
        }

        this.addPlayerSlots(inv, 8, 92);
        this.mover.registerRule((stack, slot) -> slot >= this.playerInvStart && findMatch(this.level, stack) != null, 0, 12);
        this.mover.registerRule((stack, slot) -> slot < this.playerInvStart, this.playerInvStart, this.hotbarStart + 9);
        this.registerInvShuffleRules();

        // 注册输出槽变化监听 → 自动保存
        if (!this.level.isClientSide) {
            this.addSlotListener(new net.minecraft.world.inventory.ContainerListener() {
                @Override public void slotChanged(AbstractContainerMenu menu, int slotIdx, ItemStack stack) {
                    if (slotIdx >= 12 && slotIdx < 18) saveOutput();
                }
                @Override public void dataChanged(AbstractContainerMenu menu, int slotIdx, int value) {}
            });
        }

        // 从工具物品 NBT 恢复输出物品
        if (!this.level.isClientSide) {
            ItemStack tool = findToolByUUID(this.player);
            if (!tool.isEmpty()) {
                CompoundTag tag = tool.getTagElement("salvage_output");
                if (tag != null) this.outputInv.deserializeNBT(tag);
            }
        }
    }

    /** 根据 UUID 在背包中查找便携回收工具 */
    private static ItemStack findToolByUUID(Player player) {
        UUID targetId = PortableSalvagingItem.openingToolId.get();
        if (targetId == null) return ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ApotheosisArtificeMod.PORTABLE_SALVAGING_TOOL.get()
                && stack.hasTag() && targetId.equals(stack.getTag().getUUID("ToolId"))) return stack;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() == ApotheosisArtificeMod.PORTABLE_SALVAGING_TOOL.get()
            && offhand.hasTag() && targetId.equals(offhand.getTag().getUUID("ToolId"))) return offhand;
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!this.level.isClientSide) {
            this.clearContainer(player, new RecipeWrapper(this.inputInv));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            this.salvageAll();
            this.level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 0.99F, this.level.random.nextFloat() * 0.25F + 1F);
            this.level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.BLOCKS, 0.34F, this.level.random.nextFloat() * 0.2F + 0.8F);
            this.level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.45F, this.level.random.nextFloat() * 0.5F + 0.75F);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    protected void giveItem(Player player, ItemStack stack) {
        if (!player.isAlive() || player instanceof ServerPlayer && ((ServerPlayer) player).hasDisconnected()) {
            player.drop(stack, false);
        } else {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    protected void salvageAll() {
        for (int inSlot = 0; inSlot < 12; inSlot++) {
            Slot s = this.getSlot(inSlot);
            ItemStack stack = s.getItem();
            List<ItemStack> outputs = salvageItem(this.level, stack);
            s.set(ItemStack.EMPTY);
            for (ItemStack out : outputs) {
                for (int outSlot = 0; outSlot < 6; outSlot++) {
                    if (out.isEmpty()) break;
                    out = this.outputInv.insertItem(outSlot, out, false);
                }
                if (!out.isEmpty()) this.giveItem(this.player, out);
            }
        }
        // 输出槽变化由 slotListener 自动保存
    }

    private void saveOutput() {
        if (this.level.isClientSide) return;
        ItemStack tool = findToolByUUID(this.player);
        if (tool.isEmpty()) return;
        boolean hasItems = false;
        for (int i = 0; i < this.outputInv.getSlots(); i++) {
            if (!this.outputInv.getStackInSlot(i).isEmpty()) { hasItems = true; break; }
        }
        if (hasItems) {
            tool.getOrCreateTag().put("salvage_output", this.outputInv.serializeNBT());
        } else {
            tool.getTag().remove("salvage_output");
        }
    }

    public static int[] getSalvageCounts(OutputData output, ItemStack stack) {
        int[] out = { output.getMin(), output.getMax() };
        if (stack.isDamageableItem()) {
            out[1] = Math.max(out[0], Math.round(out[1] * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()));
        }
        return out;
    }

    public static List<ItemStack> salvageItem(Level level, ItemStack stack) {
        var recipe = findMatch(level, stack);
        if (recipe == null) return Collections.emptyList();
        List<ItemStack> outputs = new ArrayList<>();
        for (OutputData d : recipe.getOutputs()) {
            ItemStack out = d.getStack().copy();
            out.setCount(getSalvageCount(d, stack, level.random));
            outputs.add(out);
        }
        return outputs;
    }

    public static int getSalvageCount(OutputData output, ItemStack stack, RandomSource rand) {
        int[] counts = getSalvageCounts(output, stack);
        return rand.nextInt(counts[0], counts[1] + 1);
    }

    public static List<ItemStack> getBestPossibleSalvageResults(Level level, ItemStack stack) {
        var recipe = findMatch(level, stack);
        if (recipe == null) return Collections.emptyList();
        List<ItemStack> outputs = new ArrayList<>();
        for (OutputData d : recipe.getOutputs()) {
            ItemStack out = d.getStack().copy();
            out.setCount(getSalvageCounts(d, stack)[1]);
            outputs.add(out);
        }
        return outputs;
    }

    @Nullable
    public static SalvagingRecipe findMatch(Level level, ItemStack stack) {
        for (var recipe : level.getRecipeManager().getAllRecipesFor(RecipeTypes.SALVAGING)) {
            if (recipe.matches(stack)) return recipe;
        }
        return null;
    }
}
