package com.apotheosis_artifice.enchant;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.registries.ForgeRegistries;

public final class EnchantingDiscountRegistry extends DynamicRegistry<EnchantingDiscountData> {

    public static final EnchantingDiscountRegistry INSTANCE = new EnchantingDiscountRegistry();
    private static final EnchantingDiscounts.Bonuses NONE = new EnchantingDiscounts.Bonuses(0, 0);

    private volatile Map<ResourceLocation, EnchantingDiscounts.Bonuses> discounts = Map.of();

    private EnchantingDiscountRegistry() {
        super(ApotheosisArtificeMod.LOGGER, "enchanting_discounts", true, false);
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerDefaultCodec(new ResourceLocation(ApotheosisArtificeMod.MODID, "enchanting_discounts"), EnchantingDiscountData.CODEC);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> definitions = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resources, "enchanting_stats", EnchantingStatRegistry.GSON, definitions);
        definitions.entrySet().removeIf(entry -> !(entry.getValue() instanceof JsonObject object)
            || !(object.get("stats") instanceof JsonObject stats)
            || !stats.has("lapis_discount") && !stats.has("experience_discount"));
        return definitions;
    }

    static List<ResourceLocation> blocksForTag(ResourceLocation tag) {
        return INSTANCE.getContext().getTag(TagKey.create(Registries.BLOCK, tag)).stream()
            .map(holder -> ForgeRegistries.BLOCKS.getKey(holder.value())).toList();
    }

    @Override
    protected void beginReload() {
        this.discounts = Map.of();
        super.beginReload();
    }

    @Override
    protected void validateItem(ResourceLocation id, EnchantingDiscountData value) {
        for (ResourceLocation block : value.blocks()) {
            if (!ForgeRegistries.BLOCKS.containsKey(block)) {
                throw new IllegalArgumentException("Unknown enchanting discount block: " + block);
            }
        }
    }

    @Override
    protected void onReload() {
        this.discounts = EnchantingDiscountData.index(this.registry);
        Map<ResourceLocation, ResourceLocation> sources = new HashMap<>();
        this.registry.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString())).forEach(entry -> entry.getValue().blocks().forEach(block -> {
            ResourceLocation previous = sources.put(block, entry.getKey());
            if (previous != null) {
                this.logger.warn("Enchanting discount definitions {} and {} target {}; using {}. Override the same definition ID to avoid conflicts.",
                    previous, entry.getKey(), block, entry.getKey());
            }
        }));
        super.onReload();
    }

    public EnchantingDiscounts.Bonuses getBonuses(ResourceLocation block) {
        return this.discounts.getOrDefault(block, NONE);
    }
}
