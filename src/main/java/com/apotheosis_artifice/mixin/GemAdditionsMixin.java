package com.apotheosis_artifice.mixin;

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.data.GemAdditions;
import com.apotheosis_artifice.curios.CuriosApotheosisModule;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.codec.CodecMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;

@Mixin(value = DynamicRegistry.class, remap = false)
public abstract class GemAdditionsMixin<R extends CodecProvider<? super R>> {
    @Shadow @Final protected CodecMap<R> codecs;
    @Shadow @Final protected Logger logger;
    @Shadow protected abstract IContext getContext();
    @Shadow protected abstract void validateItem(ResourceLocation key, R value);

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/placebo/reload/DynamicRegistry;beginReload()V", shift = At.Shift.AFTER), require = 1)
    private void apotheosis_artifice_applyGemAdditions(Map<ResourceLocation, JsonElement> objects, ResourceManager resources,
            ProfilerFiller profiler, CallbackInfo ci) {
        if (!((Object) this instanceof GemRegistry)) return;
        IContext context = this.getContext();
        GemAdditions.apply(objects, resources, context, CuriosApotheosisModule.resourceState(context).enabled(), this.logger, (id, candidate) -> {
            R gem = this.codecs.parse(JsonOps.INSTANCE, candidate).getOrThrow(false, this.logger::error);
            Preconditions.checkNotNull(gem.getCodec(), "Gem %s is not declaring a codec", id);
            Preconditions.checkNotNull(this.codecs.getKey(gem.getCodec()), "Gem %s is declaring an unregistered codec", id);
            this.validateItem(id, gem);
        });
    }
}
