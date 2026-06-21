package com.apotheosis_artifice.enchant;

import net.minecraft.resources.ResourceLocation;

public interface BookTexturedTable {

    ResourceLocation getBookTextureId();

    default ResourceLocation getBookGuiTexture() {
        ResourceLocation id = this.getBookTextureId();
        return id.withPath("textures/entity/" + id.getPath() + ".png");
    }
}
