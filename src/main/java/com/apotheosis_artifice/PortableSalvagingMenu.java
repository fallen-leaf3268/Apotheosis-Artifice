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
    private final UUID toolId;
    protected final InternalItemHandler inputInv = new InternalItemHandler(12);
    protected final InternalItemHandler outputInv = new InternalItemHandler(6) {
        @Override protected void onContentsChanged(int slot) {
            if (outputReady) saveOutput();
        }
    };
    private boolean outputReady;

    public PortableSalvagingMenu(int id, Inventory inv) {
        super(ApotheosisArtificeMod.PORTABLE_SALVAGING_MENU.get(), id, inv);
        this.player = inv.player;
        this.toolId = PortableSalvagingItem.openingToolId.get();

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

        // 从工具物品 NBT 恢复输出物品
        if (!this.level.isClientSide) {
            ItemStack tool = findToolByUUID(this.player);
            if (!tool.isEmpty()) {
                CompoundTag tag = tool.getTagElement("salvage_output");
                if (tag != null) this.outputInv.deserializeNBT(tag);
            }
        }
        this.outputReady = true;
    }

    /** 根据 UUID 在背包中查找便携回收工具 */
    private ItemStack findToolByUUID(Player player) {
        UUID targetId = this.toolId;
        if (targetId == null) return ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ApotheosisArtificeMod.PORTABLE_SALVAGING_TOOL.get()
                && stack.hasTag() && stack.getTag().hasUUID("ToolId") && targetId.equals(stack.getTag().getUUID("ToolId"))) return stack;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() == ApotheosisArtificeMod.PORTABLE_SALVAGING_TOOL.get()
            && offhand.hasTag() && offhand.getTag().hasUUID("ToolId") && targetId.equals(offhand.getTag().getUUID("ToolId"))) return offhand;
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level.isClientSide || !this.findToolByUUID(player).isEmpty();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!this.level.isClientSide) {
            this.saveOutput();
            this.clearContainer(player, new RecipeWrapper(this.inputInv));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && !this.level.isClientSide && this.stillValid(player)) {
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
            if (findMatch(this.level, stack) == null) continue;
            List<ItemStack> outputs = salvageItem(this.level, stack.copyWithCount(1));
            stack.shrink(1);
            s.setChanged();
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
        if (!this.outputReady || this.level.isClientSide) return;
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

    private boolean isOpeningTool(ItemStack stack) {
        return this.toolId != null && stack.hasTag() && stack.getTag().hasUUID("ToolId")
            && this.toolId.equals(stack.getTag().getUUID("ToolId"));
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (slotId >= 0 && slotId < this.slots.size() && this.isOpeningTool(this.getSlot(slotId).getItem())) return;
        if (type == net.minecraft.world.inventory.ClickType.SWAP && button >= 0 && button < player.getInventory().getContainerSize()
            && this.isOpeningTool(player.getInventory().getItem(button))) return;
        super.clicked(slotId, button, type, player);
        this.saveOutput();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size() || this.isOpeningTool(this.getSlot(index).getItem())) return ItemStack.EMPTY;
        ItemStack result = super.quickMoveStack(player, index);
        this.saveOutput();
        return result;
    }

    public static int[] getSalvageCounts(OutputData output, ItemStack stack) {
        int[] out = { output.getMin(), output.getMax() };
        if (stack.isDamageableItem()) {
            out[1] = Math.max(out[0], Math.round(out[1] * (float)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()));
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
