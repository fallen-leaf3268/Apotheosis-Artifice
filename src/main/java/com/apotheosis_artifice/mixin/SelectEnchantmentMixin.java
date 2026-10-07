package com.apotheosis_artifice.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.Arcana;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper.ArcanaEnchantmentData;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

@Mixin(RealEnchantmentHelper.class)
public class SelectEnchantmentMixin {

    @ModifyArg(method = "selectEnchantment", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I", remap = true), index = 2, remap = false)
    private static int artifice$extendPowerCap(int nativeCap) {
        return Math.max(nativeCap, ApotheosisConfig.getMaxEterna() * 4);
    }

    @ModifyConstant(method = "selectEnchantment", constant = @Constant(floatValue = 75F), remap = false)
    private static float artifice$linearThirdGuarantee(float threshold) {
        return 50F;
    }

    @Inject(method = "selectEnchantment", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", shift = At.Shift.AFTER, remap = false),
        locals = LocalCapture.CAPTURE_FAILHARD, remap = false)
    private static void artifice$capChoices(RandomSource rand, ItemStack stack, int level, float quanta, float arcana, float rectification,
        boolean treasure, Set<Enchantment> blacklist, CallbackInfoReturnable<List<EnchantmentInstance>> cir,
        List<EnchantmentInstance> chosenEnchants, int enchantability, int srcLevel, float quantaFactor, Arcana arcanaVals,
        List<EnchantmentInstance> allEnchants, Map<Enchantment, Integer> enchants, List<ArcanaEnchantmentData> possibleEnchants) {
        if (chosenEnchants.size() >= ApotheosisConfig.getMaxEnchantments()) possibleEnchants.clear();
    }

    @Inject(method = "selectEnchantment", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 0, remap = true),
        locals = LocalCapture.CAPTURE_FAILHARD, remap = false)
    private static void artifice$extendGuarantees(RandomSource rand, ItemStack stack, int level, float quanta, float arcana, float rectification,
        boolean treasure, Set<Enchantment> blacklist, CallbackInfoReturnable<List<EnchantmentInstance>> cir,
        List<EnchantmentInstance> chosenEnchants, int enchantability, int srcLevel, float quantaFactor, Arcana arcanaVals,
        List<EnchantmentInstance> allEnchants, Map<Enchantment, Integer> enchants, List<ArcanaEnchantmentData> possibleEnchants) {
        int guaranteed = Math.min(ApotheosisConfig.getMaxEnchantments(), 1 + (int) (arcana / 25F));
        while (chosenEnchants.size() < guaranteed && !possibleEnchants.isEmpty()) {
            RealEnchantmentHelper.removeIncompatible(possibleEnchants, Util.lastOf(chosenEnchants));
            if (possibleEnchants.isEmpty()) break;
            chosenEnchants.add(pickData(rand, possibleEnchants));
        }
        if (chosenEnchants.size() >= ApotheosisConfig.getMaxEnchantments()) possibleEnchants.clear();
    }

    private static EnchantmentInstance pickData(RandomSource rand, List<ArcanaEnchantmentData> pool) {
        ArcanaEnchantmentData picked = WeightedRandom.getRandomItem(rand, pool).get();
        return ((ArcanaEnchantmentDataAccessor) (Object) picked).getData();
    }
}
