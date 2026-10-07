package com.apotheosis_artifice.mixin;

import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.AdventureEvents;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketedGems;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;

@Mixin(value = AdventureEvents.class, remap = false)
public class AdventureEventsMixin {

    @Redirect(method = "affixModifiers", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/apotheosis/adventure/socket/SocketedGems;addModifiers(Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"))
    private void cf_skipCurioGems(SocketedGems gems, LootCategory category, EquipmentSlot slot,
        BiConsumer<Attribute, AttributeModifier> modifiers, ItemAttributeModifierEvent event) {
        if (!cf_isCurioItem(event.getItemStack())) gems.addModifiers(category, slot, modifiers);
    }

    @ModifyArg(method = "affixModifiers", at = @At(value = "INVOKE",
        target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"), index = 0)
    private BiConsumer<DynamicHolder<? extends Affix>, AffixInstance> cf_skipCurioAffixes(
        BiConsumer<DynamicHolder<? extends Affix>, AffixInstance> original) {
        return (affix, instance) -> {
            if (!cf_isCurioItem(instance.stack())) original.accept(affix, instance);
        };
    }

    @Unique
    private static boolean cf_isCurioItem(ItemStack stack) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData == null) return false;
        String category = afxData.getString("curio_artifice");
        return category.equals("curio") || category.startsWith("curios:");
    }
}
