package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.ench.EnchModule;
import dev.shadowsoffire.apotheosis.ench.EnchantmentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

@Mixin(EnchantmentInfo.class)
public abstract class EnchantMaxLevelMixin {

    @Shadow(remap = false)
    @Final
    protected Enchantment ench;

    @Shadow(remap = false)
    @Final
    protected int maxLevel;

    private static final TagKey<Enchantment> ARTIFICE_EXTRA_LEVEL =
        TagKey.create(Registries.ENCHANTMENT, new ResourceLocation(ApotheosisArtificeMod.MODID, "extra_level"));

    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private void artifice_boostMaxLevel(CallbackInfoReturnable<Integer> cir) {
        var tag = ForgeRegistries.ENCHANTMENTS.tags();
        if (tag == null) return;
        if (!tag.getTag(ARTIFICE_EXTRA_LEVEL).contains(this.ench)) return;
        int boosted = Math.max(this.maxLevel, EnchModule.getDefaultMax(this.ench));
        int hardCap = Math.min(
            EnchModule.ENCH_HARD_CAPS.getOrDefault(this.ench, 127),
            ApotheosisConfig.EXTRA_LEVEL_CAP.get());
        cir.setReturnValue(Math.min(hardCap, boosted));
    }
}
