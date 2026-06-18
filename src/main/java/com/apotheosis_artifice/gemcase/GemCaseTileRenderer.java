package com.apotheosis_artifice.gemcase;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class GemCaseTileRenderer implements BlockEntityRenderer<GemCaseTile> {

    private static final float PX = 1F / 16F;
    private static final float SCALE = 1F / 6F;

    public GemCaseTileRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(GemCaseTile tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (tile.getLevel() == null) return;

        Direction facing = tile.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        float angle = switch (facing) {
            case NORTH -> 0F;
            case EAST -> 270F;
            case SOUTH -> 180F;
            case WEST -> 90F;
            default -> 0F;
        };

        GemCaseAnimationState anim = tile.getAnimationState();
        int gemIndex = 0;
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, Integer>> entry : tile.getGemMap().entrySet()) {
            if (gemIndex >= 16) break;
            ResourceLocation gemId = entry.getKey();
            DynamicHolder<Gem> holder = GemRegistry.INSTANCE.holder(gemId);
            if (!holder.isBound()) continue;

            ResourceLocation bestRarity = null;
            int bestOrdinal = -1;
            for (Map.Entry<ResourceLocation, Integer> re : entry.getValue().entrySet()) {
                if (re.getValue() > 0) {
                    DynamicHolder<LootRarity> rh = RarityRegistry.INSTANCE.holder(re.getKey());
                    if (rh.isBound() && rh.get().ordinal() > bestOrdinal) {
                        bestOrdinal = rh.get().ordinal();
                        bestRarity = re.getKey();
                    }
                }
            }
            if (bestRarity == null) continue;

            ItemStack stack = new ItemStack(Adventure.Items.GEM.get());
            GemItem.setGem(stack, holder.get());
            DynamicHolder<LootRarity> rarityHolder = RarityRegistry.INSTANCE.holder(bestRarity);
            if (rarityHolder.isBound()) AffixHelper.setRarity(stack, rarityHolder.get());

            GemCaseAnimationState.PositionInfo info = anim.getPosition(gemIndex, partialTick);
            int slot = info.baseSlot();
            float gridX = slot % 4 + info.offsetX();
            float gridZ = slot / 4 + info.offsetZ();

            pose.pushPose();
            pose.translate(0.5F, 0F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(-0.5F, 0F, -0.5F);
            pose.translate(0F, 1F, 0F);
            pose.scale(SCALE, SCALE, SCALE);

            float tx = (2.5F + gridX * 3.75F) / SCALE * PX;
            float ty = -2F * PX / SCALE + 0.01F * gemIndex;
            float tz = (3.5F + gridZ * 3.25F) / SCALE * PX;
            pose.translate(tx, ty, tz);
            pose.mulPose(Axis.XP.rotationDegrees(45F));

            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, tile.getLevel(), tile.getBlockPos().hashCode() + gemIndex);
            pose.popPose();
            gemIndex++;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(GemCaseTile tile) {
        return false;
    }
}
