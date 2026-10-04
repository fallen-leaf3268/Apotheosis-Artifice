package com.apotheosis_artifice.mixin.client;

import com.apotheosis_artifice.ClientSetup;
import com.apotheosis_artifice.enchant.ApotheosisArtificeReforgingTableBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableTile;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableTileRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ReforgingTableTileRenderer.class, remap = false)
public class ReforgingHammerMixin {

    @Redirect(
        method = "render(Ldev/shadowsoffire/apotheosis/adventure/affix/reforging/ReforgingTableTile;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelManager;getModel(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/BakedModel;"),
        require = 1)
    private BakedModel artifice$hammerModel(ModelManager models, ResourceLocation original, ReforgingTableTile tile,
        float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        return models.getModel(tile.getBlockState().getBlock() instanceof ApotheosisArtificeReforgingTableBlock
            ? ClientSetup.REFORGING_HAMMER : original);
    }
}
