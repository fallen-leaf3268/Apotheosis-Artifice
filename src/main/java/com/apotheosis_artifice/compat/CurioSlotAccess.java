package com.apotheosis_artifice.compat;

import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;

public final class CurioSlotAccess {
    private static final boolean SIDED_SLOTS = ModList.get().getModContainerById("curios").orElseThrow()
        .getModInfo().getVersion().compareTo(new DefaultArtifactVersion("5.8.0-beta")) >= 0;

    private CurioSlotAccess() {}

    public static Map<String, ISlotType> getItemStackSlots(ItemStack stack) {
        return SIDED_SLOTS ? CuriosApi.getItemStackSlots(stack, EffectiveSide.get().isClient())
            : CuriosApi.getItemStackSlots(stack);
    }
}
