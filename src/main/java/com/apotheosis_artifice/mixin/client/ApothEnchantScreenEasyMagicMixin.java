package com.apotheosis_artifice.mixin.client;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.apotheosis_artifice.compat.EasyMagicCompat;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantScreen;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

@Mixin(ApothEnchantScreen.class)
public abstract class ApothEnchantScreenEasyMagicMixin extends EnchantmentScreen {

    private static final ResourceLocation ARTIFICE_REROLL_TEXTURE = new ResourceLocation("easymagic", "textures/gui/container/enchanting_table_reroll.png");
    private static final ResourceLocation ARTIFICE_ENCHANTING_TEXTURE = new ResourceLocation("textures/gui/container/enchanting_table.png");

    public ApothEnchantScreenEasyMagicMixin(ApothEnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void artifice$renderRerollButton(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!EasyMagicCompat.isLoaded() || !EasyMagicCompat.rerollEnchantments()) return;
        ApothEnchantmentMenu menu = (ApothEnchantmentMenu) this.menu;
        boolean usable = EasyMagicCompat.canUseReroll(menu);
        boolean affordable = this.minecraft.player.getAbilities().instabuild
            || EasyMagicCompat.getTotalExperience(this.minecraft.player) >= EasyMagicCompat.rerollExperienceCost()
            && EasyMagicCompat.getRerollCatalystCount(menu) >= EasyMagicCompat.rerollCatalystCost();
        int x = this.artifice$rerollButtonX();
        int y = this.topPos + 16;
        boolean hovered = mouseX > x && mouseX <= x + 38 && mouseY > y && mouseY <= y + 27;
        graphics.blit(ARTIFICE_REROLL_TEXTURE, x, y, 0, !usable || !affordable ? 0 : hovered ? 54 : 27, 38, 27);
        if (EasyMagicCompat.dedicatedRerollButton()) {
            graphics.blit(ARTIFICE_ENCHANTING_TEXTURE, this.leftPos + 4, this.topPos + 46, 14, 46, 18, 18);
            graphics.blit(ARTIFICE_ENCHANTING_TEXTURE, this.leftPos + 22, this.topPos + 46, 34, 46, 18, 18);
            graphics.blit(ARTIFICE_REROLL_TEXTURE, this.leftPos + 40, this.topPos + 46, 0, 81, 18, 18);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void artifice$clickReroll(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!EasyMagicCompat.isLoaded() || !EasyMagicCompat.rerollEnchantments()) return;
        int x = this.artifice$rerollButtonX();
        int y = this.topPos + 16;
        if (mouseX <= x || mouseX > x + 38 || mouseY <= y || mouseY > y + 27) return;
        if (this.menu.clickMenuButton(this.minecraft.player, 4)) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 4);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void artifice$renderRerollTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!EasyMagicCompat.isLoaded() || !EasyMagicCompat.rerollEnchantments()) return;
        int x = this.artifice$rerollButtonX();
        int y = this.topPos + 16;
        if (mouseX <= x || mouseX > x + 38 || mouseY <= y || mouseY > y + 27) return;
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("container.enchant.reroll"));
        int experience = EasyMagicCompat.rerollExperienceCost();
        int catalyst = EasyMagicCompat.rerollCatalystCost();
        if (experience > 0) tooltip.add(Component.translatable(experience == 1 ? "container.enchant.experience.one" : "container.enchant.experience.many", experience).withStyle(ChatFormatting.GREEN));
        if (catalyst > 0) tooltip.add(Component.literal(catalyst + " × ").append(Component.translatable("item.minecraft.lapis_lazuli")).withStyle(ChatFormatting.BLUE));
        graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private int artifice$rerollButtonX() {
        return this.leftPos - 40;
    }
}
