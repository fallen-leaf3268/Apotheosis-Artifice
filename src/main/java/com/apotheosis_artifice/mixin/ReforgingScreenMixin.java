package com.apotheosis_artifice.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ApotheosisNetwork;
import com.apotheosis_artifice.ISlotSelectMenu;

import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

@Mixin(ReforgingScreen.class)
public abstract class ReforgingScreenMixin extends AbstractContainerScreen<ReforgingMenu> {

    @SuppressWarnings("unused")
    private ReforgingScreenMixin() { super(null, null, null); }

    @Shadow(remap = false)
    private int animationTick;

    @Shadow(remap = false)
    private int maxSlot;

    @Unique
    private static final ResourceLocation ARTIFICE_TEXTURE = new ResourceLocation(
        ApotheosisArtificeMod.MODID, "textures/gui/reforge.png");

    @Unique
    private static final ResourceLocation ARTIFICE_ANIMATED_TEXTURE = new ResourceLocation(
        ApotheosisArtificeMod.MODID, "textures/gui/reforge_animation.png");

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"),
        index = 0, require = 2, remap = true)
    private ResourceLocation curiosforge_mainTexture(ResourceLocation original) {
        return ((ISlotSelectMenu) this.menu).curiosforge_usesArtificeGui() ? ARTIFICE_TEXTURE : original;
    }

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"),
        index = 0, require = 1, remap = true)
    private ResourceLocation curiosforge_animationTexture(ResourceLocation original) {
        return ((ISlotSelectMenu) this.menu).curiosforge_usesArtificeGui() ? ARTIFICE_ANIMATED_TEXTURE : original;
    }

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"),
        index = 6, require = 1, remap = true)
    private float curiosforge_animationV(float original) {
        return ((ISlotSelectMenu) this.menu).curiosforge_usesArtificeGui()
            ? Math.max(0F, Math.min(19 * 112F, original)) : original;
    }

    @Unique
    private List<Button> curiosforge_slotButtons = new ArrayList<>();

    @Unique
    private ItemStack curiosforge_lastSlotItem = ItemStack.EMPTY;

    @Unique
    private List<String> curiosforge_lastAvailableSlots = List.of();

    @Unique
    private int curiosforge_lastLeft;

    @Unique
    private int curiosforge_lastTop;

    @Unique
    private void curiosforge_renderOutputCharge(GuiGraphics gfx, float partials) {
        if (!((ISlotSelectMenu) this.menu).curiosforge_usesArtificeGui() || !this.menu.getSlot(0).hasItem()) return;
        float progress = Mth.clamp((8 - this.animationTick - partials) / 8F, 0F, 1F);
        int frame = Math.min(19, Mth.lerpInt(progress, 0, 20));
        int stage = frame == 0 ? 0 : frame <= 3 ? 1 : frame <= 6 ? 2 : frame <= 11 ? 3 : 4;
        int u = stage == 0 ? 232 : 184;
        int v = stage == 0 ? 88 : 88 + (stage - 1) * 24;
        int left = this.getGuiLeft();
        int top = this.getGuiTop();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        for (int index = 0; index < 3; index++) {
            boolean available = this.animationTick > 0 || index <= this.maxSlot;
            gfx.setColor(available ? 1F : 0.65F, available ? 1F : 0.7F, 1F, 1F);
            gfx.blit(ARTIFICE_TEXTURE, left + 23 + 54 * index, top + 131, u, v, 24, 24, 256, 384);
        }
        gfx.setColor(1F, 1F, 1F, 1F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void curiosforge_checkItem(net.minecraft.client.gui.GuiGraphics gfx, float partials, int x, int y, CallbackInfo ci) {
        this.curiosforge_renderOutputCharge(gfx, partials);
        ItemStack stack = this.menu.getSlot(0).getItem();
        List<String> cats = ((ISlotSelectMenu) this.menu).curiosforge_getAvailableSlots();
        if (ItemStack.matches(stack, this.curiosforge_lastSlotItem)
            && cats.equals(this.curiosforge_lastAvailableSlots)
            && this.getGuiLeft() == this.curiosforge_lastLeft
            && this.getGuiTop() == this.curiosforge_lastTop
            && this.children().containsAll(this.curiosforge_slotButtons)) return;
        this.curiosforge_lastSlotItem = stack.copy();
        this.curiosforge_lastAvailableSlots = List.copyOf(cats);
        this.curiosforge_lastLeft = this.getGuiLeft();
        this.curiosforge_lastTop = this.getGuiTop();
        curiosforge_rebuild(stack);
    }

    @Unique
    private void curiosforge_rebuild(ItemStack stack) {
        for (Button b : this.curiosforge_slotButtons) {
            this.removeWidget(b);
        }
        this.curiosforge_slotButtons.clear();

        if (stack.isEmpty()) return;

        List<String> cats = ((ISlotSelectMenu) this.menu).curiosforge_getAvailableSlots();
        if (cats.size() <= 1) return;

        int left = this.getGuiLeft();
        int top = this.getGuiTop();
        int btnX = left + this.imageWidth + 4;

        for (int idx = 0; idx < cats.size(); idx++) {
            int y = top + 12 + idx * 16;
            String catId = cats.get(idx);
            final int fIdx = idx;

            Component label;
            if (catId.startsWith("curios:")) {
                label = Component.translatable("curios.identifier." + catId.substring(7));
            } else {
                label = Component.translatable("text.apotheosis.category." + catId);
            }

            Button.OnPress onPress = b -> {
                ((ISlotSelectMenu) ReforgingScreenMixin.this.menu).curiosforge_selectSlot(fIdx);
                ApotheosisNetwork.CHANNEL.sendToServer(new ApotheosisNetwork.SlotSelectPacket(fIdx));
            };
            Button btn = ((ISlotSelectMenu) this.menu).curiosforge_usesArtificeGui()
                ? new ArtificeSlotButton(btnX, y, label, onPress)
                : Button.builder(label, onPress).bounds(btnX, y, 60, 14).build();
            this.curiosforge_slotButtons.add(btn);
            this.addRenderableWidget(btn);
        }
    }

    public static class ArtificeSlotButton extends Button {

        private static final ResourceLocation TEXTURE = new ResourceLocation(
            ApotheosisArtificeMod.MODID, "textures/gui/reforge.png");

        public ArtificeSlotButton(int x, int y, Component label, OnPress onPress) {
            super(x, y, 60, 14, label, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int v = !this.active ? 56 : this.isHoveredOrFocused() ? 36 : 16;
            gfx.setColor(1F, 1F, 1F, this.alpha);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            gfx.blit(TEXTURE, this.getX(), this.getY(), 184, v, this.width, this.height, 256, 384);
            gfx.setColor(1F, 1F, 1F, 1F);
            this.renderString(gfx, Minecraft.getInstance().font,
                (this.active ? 0xF1EAD8 : 0x8C8392) | Mth.ceil(this.alpha * 255F) << 24);
        }
    }
}
