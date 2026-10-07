package com.apotheosis_artifice.mixin.client;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.apotheosis_artifice.compat.EnigmaticLegacyCompat;
import com.apotheosis_artifice.enchant.EnchantingDiscountAccess;
import com.apotheosis_artifice.enchant.EnchantingDiscounts;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantScreen;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.util.ApothMiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;

@Mixin(ApothEnchantScreen.class)
public abstract class EnchantingDiscountScreenMixin extends EnchantmentScreen {

    protected EnchantingDiscountScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Redirect(method = "renderBg", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantmentMenu;getGoldCount()I"), require = 1)
    private int artifice$discountedFuelAvailability(ApothEnchantmentMenu menu) {
        int discount = menu instanceof EnchantingDiscountAccess access ? access.getLapisDiscount() : 0;
        return menu.getGoldCount() + discount;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", remap = false,
        target = "Ldev/shadowsoffire/apotheosis/util/ApothMiscUtil;getExpCostForSlot(II)I"), require = 1)
    private int artifice$discountedExperienceCost(int level, int slot) {
        return EnchantingDiscounts.experienceCost(this.menu, ApothMiscUtil.getExpCostForSlot(level, slot));
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"),
        index = 1, require = 1)
    private List<Component> artifice$discountedOptionTooltip(Font font, List<Component> tooltip, int mouseX, int mouseY) {
        boolean pearlActive = EnigmaticLegacyCompat.isEnchanterPearlActive(this.minecraft.player);
        if (!(this.menu instanceof EnchantingDiscountAccess access)
            || !pearlActive && access.getLapisDiscount() == 0 && access.getExperienceDiscount() == 0
            || this.minecraft.player.getAbilities().instabuild) return tooltip;
        for (int slot = 0; slot < 3; slot++) {
            if (!this.isHovering(60, 14 + 19 * slot, 108, 17, mouseX, mouseY)) continue;
            int level = this.menu.costs[slot];
            if (level <= 0 || this.minecraft.player.experienceLevel < level
                || Enchantment.byId(this.menu.enchantClue[slot]) == null || tooltip.size() < 2) return tooltip;
            List<Component> discounted = new ArrayList<>(tooltip);
            if (pearlActive || access.getLapisDiscount() > 0) {
                int cost = pearlActive ? 0
                    : EnchantingDiscounts.lapisCost(this.menu, slot + 1);
                ChatFormatting color = this.menu.getGoldCount() >= cost ? ChatFormatting.GRAY : ChatFormatting.RED;
                discounted.set(discounted.size() - 2,
                    Component.translatable("info.apotheosis_artifice.lapis_cost", cost).withStyle(color));
            }
            if (access.getExperienceDiscount() > 0) {
                int cost = EnchantingDiscounts.experienceCost(this.menu, ApothMiscUtil.getExpCostForSlot(level, slot));
                discounted.set(discounted.size() - 1,
                    Component.translatable("info.apotheosis_artifice.experience_cost", cost).withStyle(ChatFormatting.GRAY));
            }
            return discounted;
        }
        return tooltip;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", ordinal = 3, remap = false,
        target = "Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantScreen;drawOnLeft(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;I)V"),
        index = 1, require = 1)
    private List<Component> artifice$appendDiscountStats(List<Component> tooltip) {
        if (!(this.menu instanceof EnchantingDiscountAccess access)
            || access.getLapisDiscount() == 0 && access.getExperienceDiscount() == 0) return tooltip;
        List<Component> discounted = new ArrayList<>(tooltip);
        EnchantingDiscounts.appendBonuses(new EnchantingDiscounts.Bonuses(access.getLapisDiscount(), access.getExperienceDiscount()), discounted::add);
        return discounted;
    }
}
