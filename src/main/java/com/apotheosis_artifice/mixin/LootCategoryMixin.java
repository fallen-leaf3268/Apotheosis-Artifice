package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.ApotheosisConfig;
import com.apotheosis_artifice.CatOverride;
import com.apotheosis_artifice.compat.BetterCombatCompat;
import com.apotheosis_artifice.curios.CuriosApotheosisModule;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.AdventureConfig;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@Mixin(value = LootCategory.class, remap = false)
public class LootCategoryMixin {

    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void cf_skipCurioDuringNativeLookup(ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        if (!CatOverride.isNativeLookup() && CatOverride.isApotheosisBlocked(item)) {
            cir.setReturnValue(false);
            return;
        }
        String category = ((LootCategory) (Object) this).getName();
        if ((CatOverride.isNativeLookup() || CuriosApotheosisModule.isDisabled())
            && (category.equals("curio") || category.startsWith("curios:"))) cir.setReturnValue(false);
    }

    @Inject(method = "forItem", at = @At("HEAD"), cancellable = true)
    private static void cf_catOverride(ItemStack item, CallbackInfoReturnable<LootCategory> cir) {
        if (!CatOverride.isNativeLookup() && CatOverride.isApotheosisBlocked(item)) {
            cir.setReturnValue(LootCategory.NONE);
            return;
        }
        if (!CatOverride.isNativeLookup() && CuriosApotheosisModule.isDisabled() && CatOverride.isCurioItem(item)) {
            cir.setReturnValue(CatOverride.forNativeItem(item));
            return;
        }
        LootCategory override = CatOverride.get();
        if (override != null) {
            cir.setReturnValue(override);
            return;
        }

        var afxData = CatOverride.isNativeLookup() ? null : item.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData != null && afxData.contains("curio_artifice")) {
            String val = afxData.getString("curio_artifice");
            LootCategory cat = LootCategory.byId(val);
            if (cat != null && !cat.isNone()) {
                cir.setReturnValue(cat);
                return;
            }
        }

        if (ApotheosisConfig.USE_BETTERCOMBAT_HEAVY_OVERRIDE.get()
            && AdventureConfig.TYPE_OVERRIDES.get(ForgeRegistries.ITEMS.getKey(item.getItem())) == null
            && BetterCombatCompat.isTwoHanded(item)) {
            boolean isLightWeapon = LootCategory.SWORD.isValid(item) || LootCategory.TRIDENT.isValid(item);
            if (isLightWeapon) {
                cir.setReturnValue(LootCategory.HEAVY_WEAPON);
                return;
            }
        }
    }

    @Redirect(method = "forItem", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object cf_skipCurioTypeOverrideDuringNativeLookup(java.util.Map<?, ?> overrides, Object key) {
        Object value = overrides.get(key);
        if (CatOverride.isNativeLookup() && value instanceof LootCategory category && CatOverride.isCurioCategory(category)) return null;
        return value;
    }
}
