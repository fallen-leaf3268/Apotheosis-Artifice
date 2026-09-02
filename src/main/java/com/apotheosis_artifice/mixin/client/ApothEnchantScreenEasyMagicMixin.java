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

    @Inject(method = "renderBg", at = @At("HEAD"))
    private void artifice$renderRerollButton(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!EasyMagicCompat.isLoaded() || !EasyMagicCompat.rerollEnchantments()) return;
        ApothEnchantmentMenu menu = (ApothEnchantmentMenu) this.menu;
        boolean usable = EasyMagicCompat.canUseReroll(menu);
        int experience = EasyMagicCompat.rerollExperienceCost();
        int catalyst = EasyMagicCompat.rerollCatalystCost(this.minecraft.player);
        boolean missingResources = !this.minecraft.player.getAbilities().instabuild
            && (EasyMagicCompat.getTotalExperience(this.minecraft.player) < experience
                || EasyMagicCompat.getRerollCatalystCount(menu) < catalyst);
        int x = this.artifice$rerollButtonX();
        int y = this.topPos + 16;
        boolean hovered = mouseX > x && mouseX <= x + 38 && mouseY > y && mouseY <= y + 27;
        graphics.blit(ARTIFICE_REROLL_TEXTURE, x, y, 0, !usable || missingResources ? 0 : hovered ? 54 : 27, 38, 27);
        if (usable) this.artifice$renderRerollContents(graphics, x, y, missingResources, hovered, experience, catalyst);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void artifice$renderDedicatedCatalystSlot(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!EasyMagicCompat.isLoaded() || !EasyMagicCompat.rerollEnchantments()
            || !EasyMagicCompat.dedicatedRerollButton()) return;
        graphics.blit(ARTIFICE_ENCHANTING_TEXTURE, this.leftPos + 4, this.topPos + 46, 14, 46, 18, 18);
        graphics.blit(ARTIFICE_ENCHANTING_TEXTURE, this.leftPos + 22, this.topPos + 46, 34, 46, 18, 18);
        graphics.blit(ARTIFICE_REROLL_TEXTURE, this.leftPos + 40, this.topPos + 46, 0, 81, 18, 18);
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
        int catalyst = EasyMagicCompat.rerollCatalystCost(this.minecraft.player);
        if (experience > 0) tooltip.add(Component.translatable(experience == 1 ? "container.enchant.experience.one" : "container.enchant.experience.many", experience).withStyle(ChatFormatting.GREEN));
        if (catalyst > 0) tooltip.add(Component.literal(catalyst + " × ").append(Component.translatable("item.minecraft.lapis_lazuli")).withStyle(ChatFormatting.BLUE));
        graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private void artifice$renderRerollContents(GuiGraphics graphics, int x, int y, boolean missingResources,
        boolean hovered, int experience, int catalyst) {
        int iconV = missingResources ? 0 : hovered ? 30 : 15;
        if (experience == 0 && catalyst == 0) {
            graphics.blit(ARTIFICE_REROLL_TEXTURE, x + 12, y + 6, 64, iconV, 15, 15);
            return;
        }
        graphics.blit(ARTIFICE_REROLL_TEXTURE, x + 3, y + 6, 64, iconV, 15, 15);
        int orbV = missingResources ? 39 : 0;
        if (experience > 0 && catalyst > 0) {
            this.artifice$renderCostOrb(graphics, x + (experience > 9 ? 17 : 20), y + 13, 38, orbV,
                experience, missingResources ? ChatFormatting.RED : ChatFormatting.GREEN);
            this.artifice$renderCostOrb(graphics, x + (catalyst > 9 ? 17 : 20), y + 1, 51, orbV,
                catalyst, missingResources ? ChatFormatting.RED : ChatFormatting.BLUE);
        } else if (experience > 0) {
            this.artifice$renderCostOrb(graphics, x + (experience > 9 ? 17 : 20), y + 7, 38, orbV,
                experience, missingResources ? ChatFormatting.RED : ChatFormatting.GREEN);
        } else if (catalyst > 0) {
            this.artifice$renderCostOrb(graphics, x + (catalyst > 9 ? 17 : 20), y + 7, 51, orbV,
                catalyst, missingResources ? ChatFormatting.RED : ChatFormatting.BLUE);
        }
    }

    private void artifice$renderCostOrb(GuiGraphics graphics, int x, int y, int u, int v, int cost,
        ChatFormatting color) {
        graphics.blit(ARTIFICE_REROLL_TEXTURE, x, y, u, v + Math.min(2, cost / 5) * 13, 13, 13);
        this.artifice$renderReadableText(graphics, x + 8, y + 3, String.valueOf(cost), color.getColor());
    }

    private void artifice$renderReadableText(GuiGraphics graphics, int x, int y, String value, int color) {
        graphics.drawString(this.font, value, x - 1, y, 0, false);
        graphics.drawString(this.font, value, x + 1, y, 0, false);
        graphics.drawString(this.font, value, x, y - 1, 0, false);
        graphics.drawString(this.font, value, x, y + 1, 0, false);
        graphics.drawString(this.font, value, x, y, color, false);
    }

    private int artifice$rerollButtonX() {
        return this.leftPos - 36;
    }
}
