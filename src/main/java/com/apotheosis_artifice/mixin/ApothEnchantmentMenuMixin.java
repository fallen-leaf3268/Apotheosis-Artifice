package com.apotheosis_artifice.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.compat.EasyMagicCompat;
import com.apotheosis_artifice.compat.EasyMagicEnchantingStorage;
import com.apotheosis_artifice.compat.EasyMagicInventoryMigration;
import com.apotheosis_artifice.compat.EasyMagicInventoryMigrator;
import com.apotheosis_artifice.compat.EnigmaticLegacyCompat;
import com.apotheosis_artifice.enchant.EnchantingCostRules;
import com.apotheosis_artifice.enchant.EnchantingDiscountAccess;
import com.apotheosis_artifice.enchant.EnchantingDiscounts;
import com.apotheosis_artifice.lead.EnderLeadAccess;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.Ench;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.StatsMessage;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantTile;
import dev.shadowsoffire.placebo.network.PacketDistro;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;

@Mixin(ApothEnchantmentMenu.class)
public abstract class ApothEnchantmentMenuMixin extends EnchantmentMenu implements EasyMagicInventoryMigration, EnchantingDiscountAccess {

    @Shadow(remap = false) protected ApothEnchantmentMenu.TableStats stats;
    @Unique private Player artifice$menuPlayer;
    @Unique private Inventory artifice$playerInventory;
    @Unique private Container artifice$pendingEasyMagicInventory;
    @Unique private EasyMagicEnchantingStorage artifice$legacyFuelStorage;
    @Unique private DataSlot artifice$lapisDiscount;
    @Unique private DataSlot artifice$experienceDiscount;

    protected ApothEnchantmentMenuMixin(int id, Inventory inventory) {
        super(id, inventory);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void artifice$bindClientEasyMagicInventory(int id, Inventory inventory, CallbackInfo ci) {
        this.artifice$rememberPlayer(inventory);
        if (EasyMagicCompat.isLoaded()) this.artifice$bindEasyMagicInventory(new SimpleContainer(3) {
            @Override
            public void setChanged() {
                super.setChanged();
                ApothEnchantmentMenuMixin.this.slotsChanged(this);
            }
        });
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantTile;)V", at = @At("TAIL"), remap = false)
    private void artifice$bindServerEasyMagicInventory(int id, Inventory inventory, ContainerLevelAccess access, ApothEnchantTile tile, CallbackInfo ci) {
        this.artifice$rememberPlayer(inventory);
        if (!(tile instanceof EasyMagicEnchantingStorage storage)) return;
        if (EasyMagicCompat.isLoaded()) {
            this.artifice$legacyFuelStorage = storage;
            this.artifice$bindEasyMagicInventory(storage.getEasyMagicInventory());
        } else {
            this.artifice$queueEasyMagicInventoryMigration(storage.getEasyMagicInventory());
        }
    }

    private void artifice$rememberPlayer(Inventory inventory) {
        this.artifice$menuPlayer = inventory.player;
        this.artifice$playerInventory = inventory;
        this.artifice$lapisDiscount = this.addDataSlot(DataSlot.standalone());
        this.artifice$experienceDiscount = this.addDataSlot(DataSlot.standalone());
        this.artifice$refreshDiscounts();
    }

    @Override
    public int getLapisDiscount() {
        return this.artifice$lapisDiscount == null ? 0 : this.artifice$lapisDiscount.get();
    }

    @Override
    public int getExperienceDiscount() {
        return this.artifice$experienceDiscount == null ? 0 : this.artifice$experienceDiscount.get();
    }

    @Unique
    private void artifice$refreshDiscounts() {
        if (this.artifice$menuPlayer == null || this.artifice$menuPlayer.level().isClientSide || this.artifice$lapisDiscount == null) return;
        this.access.execute((world, pos) -> {
            var bonuses = EnchantingDiscounts.gather(world, pos);
            this.artifice$lapisDiscount.set(bonuses.lapis());
            this.artifice$experienceDiscount.set(bonuses.experience());
        });
    }

    @Override
    public void broadcastChanges() {
        this.artifice$refreshDiscounts();
        super.broadcastChanges();
    }

    @Redirect(method = "clickMenuButton", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0))
    private boolean artifice$emptyFuelNeedsLapis(ItemStack fuel, Player player, int id) {
        return EnchantingCostRules.lapisCost(id + 1, this.getLapisDiscount()) > 0 && fuel.isEmpty();
    }

    @Redirect(method = "clickMenuButton", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getCount()I", ordinal = 0))
    private int artifice$availableDiscountedFuel(ItemStack fuel) {
        return fuel.getCount() + this.getLapisDiscount();
    }

    @ModifyArg(method = "lambda$clickMenuButton$0", at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"), index = 0, remap = false)
    private int artifice$reduceLapisConsumption(int original) {
        return EnchantingCostRules.lapisCost(original, this.getLapisDiscount());
    }

    @ModifyArg(method = "lambda$clickMenuButton$0", at = @At(value = "INVOKE", remap = false,
        target = "Ldev/shadowsoffire/placebo/util/EnchantmentUtils;chargeExperience(Lnet/minecraft/world/entity/player/Player;I)Z"), index = 1, remap = false)
    private int artifice$reduceExperienceConsumption(int original) {
        if (this.artifice$menuPlayer != null && this.artifice$menuPlayer.getAbilities().instabuild) return 0;
        return EnchantingCostRules.experienceCost(original, this.getExperienceDiscount());
    }

    @Unique
    private void artifice$queueEasyMagicInventoryMigration(Container inventory) {
        this.artifice$pendingEasyMagicInventory = inventory;
    }

    @Override
    public void artifice$migrateEasyMagicInventory() {
        if (this.artifice$legacyFuelStorage != null) {
            EasyMagicEnchantingStorage storage = this.artifice$legacyFuelStorage;
            this.artifice$legacyFuelStorage = null;
            storage.migrateLegacyFuel(this.artifice$menuPlayer);
        }
        Container source = this.artifice$pendingEasyMagicInventory;
        if (source == null) return;
        this.artifice$pendingEasyMagicInventory = null;
        EasyMagicInventoryMigrator.migrate(new EasyMagicInventoryMigrator.Source<ItemStack>() {
            @Override public ItemStack get(int slot) { return source.getItem(slot); }
            @Override public void clear(int slot) { source.setItem(slot, ItemStack.EMPTY); }
        }, new EasyMagicInventoryMigrator.Target<ItemStack>() {
            @Override public boolean hasItem() { return ApothEnchantmentMenuMixin.this.slots.get(0).hasItem(); }
            @Override public boolean mayPlace(ItemStack stack) { return ApothEnchantmentMenuMixin.this.slots.get(0).mayPlace(stack); }
            @Override public void set(ItemStack stack) { ApothEnchantmentMenuMixin.this.slots.get(0).set(stack); }
        }, new EasyMagicInventoryMigrator.Target<ItemStack>() {
            @Override public boolean hasItem() { return ApothEnchantmentMenuMixin.this.slots.get(1).hasItem(); }
            @Override public boolean mayPlace(ItemStack stack) { return ApothEnchantmentMenuMixin.this.slots.get(1).mayPlace(stack); }
            @Override public void set(ItemStack stack) { ApothEnchantmentMenuMixin.this.slots.get(1).set(stack); }
        }, this.artifice$playerInventory::placeItemBackInInventory, ItemStack::copy, ItemStack::isEmpty);
    }

    @Unique
    private void artifice$moveOrReturnEasyMagicStack(Container source, int sourceSlot, int targetSlot) {
        ItemStack stack = source.getItem(sourceSlot).copy();
        if (stack.isEmpty()) return;
        Slot target = this.slots.get(targetSlot);
        if (!target.hasItem() && target.mayPlace(stack)) {
            target.set(stack);
        } else {
            this.artifice$playerInventory.placeItemBackInInventory(stack);
        }
        source.setItem(sourceSlot, ItemStack.EMPTY);
    }

    @Unique
    private void artifice$returnEasyMagicStack(Container source, int sourceSlot) {
        ItemStack stack = source.getItem(sourceSlot).copy();
        if (stack.isEmpty()) return;
        this.artifice$playerInventory.placeItemBackInInventory(stack);
        source.setItem(sourceSlot, ItemStack.EMPTY);
    }

    private void artifice$bindEasyMagicInventory(Container inventory) {
        this.enchantSlots = inventory;
        boolean dedicatedReroll = EasyMagicCompat.dedicatedRerollButton();
        Slot input = new Slot(inventory, 0, dedicatedReroll ? 5 : 15, 47) {
            @Override public int getMaxStackSize() { return 1; }
        };
        input.index = 0;
        this.slots.set(0, input);
        Slot oldFuel = this.slots.get(1);
        Slot fuel = new Slot(inventory, 1, dedicatedReroll ? 23 : 35, 47) {
            @Override public boolean mayPlace(ItemStack stack) {
                return oldFuel.mayPlace(stack) || EasyMagicCompat.isEnchantingCatalyst(stack);
            }
        };
        fuel.index = 1;
        this.slots.set(1, fuel);
        if (dedicatedReroll) {
            this.addSlot(new Slot(inventory, 2, 41, 47) {
                @Override public boolean mayPlace(ItemStack stack) { return EasyMagicCompat.isRerollCatalyst(stack); }
            });
        }
        if (((Object) this).getClass() == ApothEnchantmentMenu.class) {
            this.slotsChanged(this.enchantSlots);
        }
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void artifice$handleEasyMagicReroll(Player player, int data, CallbackInfoReturnable<Boolean> cir) {
        this.artifice$refreshDiscounts();
        if (data == 5) {
            ApothEnchantmentMenu menu = (ApothEnchantmentMenu) (Object) this;
            if (!player.level().isClientSide) menu.slotsChanged(menu.enchantSlots);
            cir.setReturnValue(true);
            return;
        }
        if (data == 4) {
            cir.setReturnValue(EasyMagicCompat.tryReroll((ApothEnchantmentMenu) (Object) this, player));
            return;
        }
        if (data < 0 || data >= 3 || data < 2 && this.enchantSlots.getItem(0).getItem() instanceof EnderLeadAccess) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getEnchantmentList", at = @At("RETURN"), cancellable = true)
    private void artifice$keepEnderLeadInfusionOnly(ItemStack stack, int slot, int level, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        if (!(stack.getItem() instanceof EnderLeadAccess)) return;
        List<EnchantmentInstance> enchantments = cir.getReturnValue();
        if (slot == 2 && enchantments.size() == 1 && enchantments.get(0).enchantment == Ench.Enchantments.INFUSION.get()) return;
        if (slot < 2) {
            this.costs[slot] = 0;
            this.enchantClue[slot] = -1;
            this.levelClue[slot] = -1;
        }
        cir.setReturnValue(new ArrayList<>());
    }

    @Override
    public int getGoldCount() {
        if (EnigmaticLegacyCompat.isEnchanterPearlActive(this.artifice$menuPlayer)) return 64;
        return super.getGoldCount();
    }

    @ModifyVariable(method = "clickMenuButton", at = @At("STORE"), ordinal = 1)
    private ItemStack artifice$provideVirtualPearlFuel(ItemStack fuel, Player player, int id) {
        if (id < 0 || id >= 3 || !EnigmaticLegacyCompat.isEnchanterPearlActive(player)) return fuel;
        if (fuel.isEmpty()) return new ItemStack(Items.LAPIS_LAZULI, 64);
        ItemStack virtualFuel = fuel.copy();
        virtualFuel.setCount(64);
        return virtualFuel;
    }

    @Inject(method = "canReadStatsFrom", at = @At("HEAD"), cancellable = true, remap = false)
    private static void artifice$allowLenientBookshelfPath(Level level, BlockPos tablePos,
        BlockPos shelfOffset, CallbackInfoReturnable<Boolean> cir) {
        if (!EasyMagicCompat.lenientBookshelves()) return;
        BlockPos between = tablePos.offset(
            shelfOffset.getX() / 2, shelfOffset.getY(), shelfOffset.getZ() / 2);
        if (level.getBlockState(between).getCollisionShape(level, between) != Shapes.block()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "gatherStats", at = @At("TAIL"), remap = false)
    private void artifice$enableEnchanterPearlTreasure(CallbackInfo ci) {
        if (this.artifice$menuPlayer == null || this.artifice$menuPlayer.level().isClientSide) return;
        ApothEnchantmentMenu.TableStats updated = EnigmaticLegacyCompat.enableTreasure(this.stats, this.artifice$menuPlayer);
        if (updated == this.stats) return;
        this.stats = updated;
        PacketDistro.sendTo(Apotheosis.CHANNEL, new StatsMessage(this.stats), this.artifice$menuPlayer);
    }
}
