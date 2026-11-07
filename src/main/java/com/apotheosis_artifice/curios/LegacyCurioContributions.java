package com.apotheosis_artifice.curios;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosCapability;

public final class LegacyCurioContributions {
    private static final String APPLIED = "apotheosis_artifice:attribute_base_bonuses";
    private static final String UNIQUE_SLOTS = "apotheosis_artifice:unique_slot_bonuses";
    private static final ResourceLocation LEGACY_BASE = new ResourceLocation("apotheosis_artifice", "attribute/attribute_base");
    private static final Set<ResourceLocation> LEGACY_SLOTS = Set.of(
        new ResourceLocation("apotheosis_artifice", "stat/curio_slot_bonus"),
        new ResourceLocation("apotheosis_artifice", "stat/head_necklace_bonus"));

    private LegacyCurioContributions() {}

    public static boolean prepare(LivingEntity entity, boolean recoverSlots) {
        CompoundTag persistent = entity.getPersistentData();
        boolean needsBase = !persistent.contains(APPLIED, Tag.TAG_COMPOUND);
        boolean needsSlots = recoverSlots && !persistent.contains(UNIQUE_SLOTS, Tag.TAG_COMPOUND);
        if (!needsBase && !needsSlots) return true;
        var handler = entity.getCapability(CuriosCapability.INVENTORY).orElse(null);
        Map<ResourceLocation, Float> bonuses = new HashMap<>();
        CompoundTag slots = new CompoundTag();
        boolean baseComplete = true;
        boolean slotsComplete = true;
        if (handler != null) {
            ResourceManager resources = entity.getServer().getResourceManager();
            for (var slot : handler.getCurios().entrySet()) {
                var stacks = slot.getValue().getStacks();
                for (int index = 0; index < stacks.getSlots(); index++) {
                    var stack = stacks.getStackInSlot(index);
                    CompoundTag data = stack.getTagElement("affix_data");
                    if (data == null || !CurioApotheosisEvents.matchesStoredSlot(stack, slot.getKey())) continue;
                    Contributions contribution = read(data, slot.getKey(), index, id -> load(resources, id));
                    baseComplete &= contribution.baseComplete();
                    slotsComplete &= contribution.slotsComplete();
                    contribution.base().forEach((id, value) -> bonuses.merge(id, value, Float::sum));
                    contribution.slots().forEach((name, ids) -> {
                        CompoundTag values = slots.getCompound(name);
                        ids.forEach(id -> values.putFloat(id.toString(), 0F));
                        slots.put(name, values);
                    });
                }
            }
        }
        CompoundTag base = new CompoundTag();
        if (needsBase && baseComplete) {
            for (var bonus : bonuses.entrySet()) {
                var attribute = ForgeRegistries.ATTRIBUTES.getValue(bonus.getKey());
                var instance = attribute == null ? null : entity.getAttribute(attribute);
                if (instance == null) {
                    baseComplete = false;
                    break;
                }
                double actual = instance.getBaseValue();
                if (actual == attribute.getDefaultValue() + bonus.getValue()) {
                    base.putFloat(bonus.getKey().toString(), bonus.getValue());
                } else if (actual != attribute.getDefaultValue()) {
                    baseComplete = false;
                    break;
                }
            }
            if (baseComplete) persistent.put(APPLIED, base);
        }
        if (needsSlots && slotsComplete) persistent.put(UNIQUE_SLOTS, slots);
        return (!needsBase || baseComplete) && (!needsSlots || slotsComplete);
    }

    static Contributions read(CompoundTag data, String wornSlot, int index,
            Function<ResourceLocation, Optional<JsonObject>> definitions) {
        Map<ResourceLocation, Float> base = new HashMap<>();
        Map<String, Set<UUID>> slots = new HashMap<>();
        CompoundTag affixes = data.getCompound("affixes");
        boolean baseComplete = true;
        boolean slotsComplete = true;
        for (String key : affixes.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null || !affixes.contains(key, Tag.TAG_ANY_NUMERIC)) continue;
            Optional<JsonObject> definition = definitions.apply(id);
            if (definition.isEmpty()) {
                if (LEGACY_BASE.equals(id)) baseComplete = false;
                if (LEGACY_SLOTS.contains(id)) slotsComplete = false;
                continue;
            }
            JsonObject json = definition.get();
            String type = "";
            try {
                type = GsonHelper.getAsString(json, "type", "");
                if (LEGACY_BASE.equals(id) && !type.equals("apotheosis_artifice:attribute_base")) baseComplete = false;
                if (LEGACY_SLOTS.contains(id) && !type.equals("apotheosis_artifice:curio_slot_bonus")) slotsComplete = false;
                if (type.equals("apotheosis_artifice:attribute_base")) {
                    ResourceLocation attribute = ResourceLocation.tryParse(GsonHelper.getAsString(json, "attribute"));
                    Optional<Float> value = value(json, data.getString("rarity"), affixes.getFloat(key));
                    if (attribute == null || value.isEmpty()) baseComplete = false;
                    else if (value.get() != 0F) base.merge(attribute, value.get(), Float::sum);
                } else if (type.equals("apotheosis_artifice:curio_slot_bonus")) {
                    String slot = GsonHelper.getAsString(json, "slot");
                    if (slot.startsWith("curios:")) slot = slot.substring(7);
                    String fixed = GsonHelper.getAsString(json, "uuid", "");
                    UUID uuid = fixed.isEmpty() ? UUID.nameUUIDFromBytes(
                        ("apoth_art:csb:" + wornSlot + ":" + index + ":" + slot).getBytes(StandardCharsets.UTF_8)) : UUID.fromString(fixed);
                    slots.computeIfAbsent(slot, ignored -> new HashSet<>()).add(uuid);
                }
            } catch (JsonParseException | IllegalArgumentException | IllegalStateException exception) {
                if (type.equals("apotheosis_artifice:attribute_base") || LEGACY_BASE.equals(id)) baseComplete = false;
                if (type.equals("apotheosis_artifice:curio_slot_bonus") || LEGACY_SLOTS.contains(id)) slotsComplete = false;
            }
        }
        return new Contributions(Map.copyOf(base), Map.copyOf(slots), baseComplete, slotsComplete);
    }

    private static Optional<Float> value(JsonObject definition, String rarity, float level) {
        if (rarity.isEmpty() || !Float.isFinite(level)) return Optional.empty();
        JsonObject values = GsonHelper.getAsJsonObject(definition, "values");
        var value = values.get(rarity);
        ResourceLocation id = ResourceLocation.tryParse(rarity);
        if (value == null && id != null) value = values.get(id.getPath());
        if (value == null) return Optional.of(0F);
        return StepFunction.CODEC.parse(JsonOps.INSTANCE, value).result().map(function -> function.get(level)).filter(Float::isFinite);
    }

    private static Optional<JsonObject> load(ResourceManager manager, ResourceLocation id) {
        var resource = manager.getResource(new ResourceLocation(id.getNamespace(), "affixes/" + id.getPath() + ".json"));
        if (resource.isEmpty()) return Optional.empty();
        try (var reader = resource.get().openAsReader()) {
            return Optional.of(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            com.apotheosis_artifice.ApotheosisArtificeMod.LOGGER.warn("Unable to read legacy Curio affix {}", id, exception);
            return Optional.empty();
        }
    }

    record Contributions(Map<ResourceLocation, Float> base, Map<String, Set<UUID>> slots, boolean baseComplete, boolean slotsComplete) {}
}
