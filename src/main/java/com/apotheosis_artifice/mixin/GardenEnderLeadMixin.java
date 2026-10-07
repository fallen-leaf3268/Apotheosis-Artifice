package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.lead.EnderLeadAccess;
import com.apotheosis_artifice.lead.EnderLeadBehavior;
import com.apotheosis_artifice.lead.EnderLeadData;
import com.apotheosis_artifice.lead.EnderLeadTier;
import dev.shadowsoffire.apotheosis.garden.EnderLeadItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderLeadItem.class)
public abstract class GardenEnderLeadMixin extends Item implements EnderLeadAccess {
    protected GardenEnderLeadMixin(Properties properties) { super(properties); }

    public EnderLeadTier artifice$getLeadTier() { return EnderLeadTier.NORMAL; }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;<init>(Lnet/minecraft/world/item/Item$Properties;)V"), index = 0)
    private static Properties artifice$normalDurability(Properties properties) {
        return properties.durability(EnderLeadTier.NORMAL.durability());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return EnderLeadBehavior.capture(stack, player, target, hand, EnderLeadTier.NORMAL);
    }

    @Override
    public Component getName(ItemStack stack) {
        return EnderLeadBehavior.displayName(stack, super.getName(stack));
    }

    @Inject(method = "onLeftClickEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void artifice$captureOnLegacyAttack(ItemStack stack, Player player, Entity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(EnderLeadBehavior.capture(stack, player, target, InteractionHand.MAIN_HAND, EnderLeadTier.NORMAL) != InteractionResult.PASS);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void artifice$safeRelease(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        cir.setReturnValue(EnderLeadBehavior.useOn(context, EnderLeadTier.NORMAL));
    }

    @Inject(method = "isFoil", at = @At("HEAD"), cancellable = true)
    private void artifice$storedEntityGlint(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(EnderLeadData.containsEntity(stack.getTag()));
    }

    @Inject(method = "getShareTag", at = @At("HEAD"), cancellable = true, remap = false)
    private void artifice$shareLegacyStorage(ItemStack stack, CallbackInfoReturnable<CompoundTag> cir) {
        cir.setReturnValue(EnderLeadData.shareTag(stack.getTag()));
    }
}
