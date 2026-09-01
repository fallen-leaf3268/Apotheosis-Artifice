package com.apotheosis_artifice.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.compat.EasyMagicCompat;
import com.apotheosis_artifice.compat.EasyMagicEnchantingStorage;
import com.apotheosis_artifice.compat.EnigmaticLegacyCompat;

import dev.shadowsoffire.apotheosis.advancements.EnchantedTrigger;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantTile;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingRecipe;
import dev.shadowsoffire.apotheosis.ench.table.IEnchantableItem;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

@Mixin(ApothEnchantmentMenu.class)
public abstract class ApothEnchantmentMenuMixin extends EnchantmentMenu {

    @Shadow(remap = false) protected ApothEnchantmentMenu.TableStats stats;

    protected ApothEnchantmentMenuMixin(int id, Inventory inventory) {
        super(id, inventory);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void artifice$bindClientEasyMagicInventory(int id, Inventory inventory, CallbackInfo ci) {
        if (EasyMagicCompat.isLoaded()) this.artifice$bindEasyMagicInventory(new SimpleContainer(2));
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantTile;)V", at = @At("TAIL"), remap = false)
    private void artifice$bindServerEasyMagicInventory(int id, Inventory inventory, ContainerLevelAccess access, ApothEnchantTile tile, CallbackInfo ci) {
        if (EasyMagicCompat.isLoaded() && tile instanceof EasyMagicEnchantingStorage storage) {
            this.artifice$bindEasyMagicInventory(storage.getEasyMagicInventory());
        }
    }

    private void artifice$bindEasyMagicInventory(Container inventory) {
        this.enchantSlots = inventory;
        Slot input = new Slot(inventory, 0, EasyMagicCompat.dedicatedRerollButton() ? 5 : 15, 47) {
            @Override public int getMaxStackSize() { return 1; }
        };
        input.index = 0;
        this.slots.set(0, input);
        Slot oldFuel = this.slots.get(1);
        Slot fuel = new Slot(oldFuel.container, oldFuel.getContainerSlot(), EasyMagicCompat.dedicatedRerollButton() ? 23 : 35, 47) {
            @Override public boolean mayPlace(ItemStack stack) {
                return oldFuel.mayPlace(stack) || EasyMagicCompat.isEnchantingCatalyst(stack);
            }
        };
        fuel.index = 1;
        this.slots.set(1, fuel);
        if (EasyMagicCompat.dedicatedRerollButton()) {
            this.addSlot(new Slot(inventory, 1, 41, 47) {
                @Override public boolean mayPlace(ItemStack stack) { return EasyMagicCompat.isRerollCatalyst(stack); }
            });
        }
        this.slotsChanged(this.enchantSlots);
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void artifice_handleEnchanterPearl(Player player, int data, CallbackInfoReturnable<Boolean> cir) {
        if (data == 4) {
            cir.setReturnValue(EasyMagicCompat.tryReroll((ApothEnchantmentMenu) (Object) this, player));
            return;
        }
        if (((Object) this).getClass() != ApothEnchantmentMenu.class
            || !EnigmaticLegacyCompat.isEnchanterPearlActive(player)) return;
        int id = data;
        if (id < 0 || id >= this.costs.length) {
            cir.setReturnValue(false);
            return;
        }

        int level = this.costs[id];
        int levelsRequired = id + 1;
        ItemStack input = this.enchantSlots.getItem(0);
        if (level <= 0 || input.isEmpty()
            || (player.experienceLevel < levelsRequired || player.experienceLevel < level) && !player.getAbilities().instabuild) {
            cir.setReturnValue(false);
            return;
        }

        this.access.execute((world, pos) -> {
            float eterna = this.stats.eterna();
            float quanta = this.stats.quanta();
            float arcana = this.stats.arcana();
            EnchantingRecipe recipe = id == 2 ? EnchantingRecipe.findMatch(world, input, eterna, quanta, arcana) : null;
            RandomSource rollRandom = RandomSource.create(this.enchantmentSeed.get() + id);
            List<EnchantmentInstance> enchantments = RealEnchantmentHelper.selectEnchantment(
                rollRandom, input, level, quanta, arcana, this.stats.rectification(), this.stats.treasure(), this.stats.blacklist());
            if (recipe == null && enchantments.isEmpty()) return;

            EnchantmentUtils.chargeExperience(player, ApothMiscUtil.getExpCostForSlot(level, id));
            player.onEnchantmentPerformed(input, 0);
            ItemStack result;
            if (recipe != null) {
                result = recipe.assemble(input, eterna, quanta, arcana);
            } else {
                ItemStack bonus = EnchantmentHelper.enchantItem(player.getRandom(), input.copy(), Math.min(level + 7, 40), true);
                result = ((IEnchantableItem) input.getItem()).onEnchantment(input, enchantments);
                result = EnigmaticLegacyCompat.mergePearlEnchantments(result, bonus);
            }
            this.enchantSlots.setItem(0, result);
            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                ((EnchantedTrigger) CriteriaTriggers.ENCHANTED_ITEM).trigger(
                    serverPlayer, result, level, eterna, quanta, arcana, this.stats.rectification());
            }
            this.enchantSlots.setChanged();
            this.enchantmentSeed.set(player.getEnchantmentSeed());
            this.slotsChanged(this.enchantSlots);
            world.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
        });
        cir.setReturnValue(true);
    }
}
