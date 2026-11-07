package com.apotheosis_artifice.curios;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public final class CuriosModuleCondition implements ICondition {
    public static final ResourceLocation ID = new ResourceLocation(ApotheosisArtificeMod.MODID, "curios_module");
    public static final IConditionSerializer<CuriosModuleCondition> SERIALIZER = new IConditionSerializer<>() {
        @Override
        public void write(JsonObject json, CuriosModuleCondition value) {}

        @Override
        public CuriosModuleCondition read(JsonObject json) {
            return new CuriosModuleCondition();
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    };

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return CuriosApotheosisModule.resourceState(context).enabled();
    }
}
