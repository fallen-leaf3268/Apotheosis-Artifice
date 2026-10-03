package com.apotheosis_artifice.mixin.client;

import com.apotheosis_artifice.enchant.BookTexturedTable;
import com.apotheosis_artifice.enchant.MechanicalRavenEnchantScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentScreen.class)
public class EnchantmentBookMixin {

    @Inject(method = "renderBook", at = @At("HEAD"), cancellable = true)
    private void apotheosis_artifice_cancelBook(GuiGraphics gfx, int x, int y, float pt, CallbackInfo ci) {
        if (((Object) this) instanceof MechanicalRavenEnchantScreen) {
            ci.cancel();
        }
    }

    @Mixin(EnchantTableRenderer.class)
    public static class WorldBook {

        @ModifyVariable(
            method = "render(Lnet/minecraft/world/level/block/entity/EnchantmentTableBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("STORE"), ordinal = 0, require = 1)
        private VertexConsumer artifice$bookTexture(VertexConsumer original, EnchantmentTableBlockEntity tile,
            float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
            if (tile.getBlockState().getBlock() instanceof BookTexturedTable table) {
                return buffers.getBuffer(RenderType.entitySolid(table.getBookGuiTexture()));
            }
            return original;
        }
    }
}
