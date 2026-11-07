package com.apotheosis_artifice.data;

import java.io.Reader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.shadowsoffire.placebo.json.JsonUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;

public final class GemAdditions {
    public static final String DIRECTORY = "apotheosis_artifice/gem_additions";

    private GemAdditions() {}

    public static void apply(Map<ResourceLocation, JsonElement> gems, ResourceManager resources, IContext context,
            Logger logger, BiConsumer<ResourceLocation, JsonObject> validator) {
        Map<String, Integer> priorities = new HashMap<>();
        resources.listPacks().forEachOrdered(pack -> priorities.put(pack.packId(), priorities.size()));
        var additions = resources.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).entrySet().stream()
            .sorted(Comparator.<Map.Entry<ResourceLocation, Resource>>comparingInt(entry -> priorities.get(entry.getValue().sourcePackId()))
                .reversed().thenComparing(entry -> entry.getKey().toString())).toList();

        for (var entry : additions) {
            ResourceLocation source = entry.getKey();
            Resource resource = entry.getValue();
            try (Reader reader = resource.openAsReader()) {
                JsonObject addition = GsonHelper.convertToJsonObject(JsonParser.parseReader(reader), "gem addition");
                if (!JsonUtil.checkConditions(addition, source, DIRECTORY, logger, context)) continue;
                ResourceLocation target = new ResourceLocation(GsonHelper.getAsString(addition, "target"));
                JsonElement original = gems.get(target);
                if (original == null) {
                    logger.warn("Skipping gem addition {} from {}: target {} does not exist", source, resource.sourcePackId(), target);
                    continue;
                }
                if (!JsonUtil.checkAndLogEmpty(original, target, "gems", logger)
                    || !JsonUtil.checkConditions(original, target, "gems", logger, context)) continue;
                JsonObject candidate = GemAddition.merge(original.getAsJsonObject(), addition);
                validator.accept(target, candidate);
                gems.put(target, candidate);
            } catch (Exception exception) {
                logger.error("Failed applying gem addition {} from {}. Previous gem definition retained.", source, resource.sourcePackId(), exception);
            }
        }
    }
}
