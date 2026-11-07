package com.apotheosis_artifice.data;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.util.GsonHelper;

public final class GemAddition {
    private GemAddition() {}

    public static JsonObject merge(JsonObject original, JsonObject addition) {
        checkFields(addition, Set.of("target", "extend_bonuses", "add_bonuses", "conditions", "forge:conditions"));
        JsonArray extensions = addition.has("extend_bonuses") ? GsonHelper.getAsJsonArray(addition, "extend_bonuses") : new JsonArray();
        JsonArray additions = addition.has("add_bonuses") ? GsonHelper.getAsJsonArray(addition, "add_bonuses") : new JsonArray();
        if (extensions.isEmpty() && additions.isEmpty()) throw new IllegalArgumentException("No gem addition operations specified");

        JsonObject merged = original.deepCopy();
        JsonArray bonuses = GsonHelper.getAsJsonArray(merged, "bonuses");
        Map<LootCategory, JsonObject> owners = new HashMap<>();
        for (JsonElement element : bonuses) {
            JsonObject bonus = element.getAsJsonObject();
            for (LootCategory type : types(bonus)) {
                if (owners.putIfAbsent(type, bonus) != null) throw conflict(type);
            }
        }

        for (JsonElement element : extensions) {
            JsonObject extension = element.getAsJsonObject();
            checkFields(extension, Set.of("match_category", "add_types"));
            LootCategory match = category(GsonHelper.getAsString(extension, "match_category"));
            JsonObject bonus = owners.get(match);
            if (bonus == null) throw new IllegalArgumentException("No bonus directly declares category " + match.getName());
            JsonArray addedTypes = GsonHelper.getAsJsonArray(extension, "add_types");
            if (addedTypes.isEmpty()) throw new IllegalArgumentException("add_types must not be empty");
            JsonArray existingTypes = bonus.getAsJsonObject("gem_class").getAsJsonArray("types");
            for (JsonElement added : addedTypes) {
                LootCategory type = category(GsonHelper.convertToString(added, "add_types entry"));
                JsonObject owner = owners.putIfAbsent(type, bonus);
                if (owner != null && owner != bonus) throw conflict(type);
                if (owner == null) existingTypes.add(type.getName());
            }
        }

        for (JsonElement element : additions) {
            JsonObject bonus = element.getAsJsonObject();
            Set<LootCategory> types = types(bonus);
            JsonObject existing = owners.get(types.iterator().next());
            if (existing != null && sameBonus(existing, bonus)) continue;
            for (LootCategory type : types) {
                if (owners.containsKey(type)) throw conflict(type);
            }
            JsonObject copy = bonus.deepCopy();
            bonuses.add(copy);
            types.forEach(type -> owners.put(type, copy));
        }
        return merged;
    }

    private static boolean sameBonus(JsonObject first, JsonObject second) {
        if (!types(first).equals(types(second))) return false;
        JsonObject left = first.deepCopy();
        JsonObject right = second.deepCopy();
        left.getAsJsonObject("gem_class").remove("types");
        right.getAsJsonObject("gem_class").remove("types");
        return left.equals(right);
    }

    private static Set<LootCategory> types(JsonObject bonus) {
        JsonArray values = GsonHelper.getAsJsonArray(GsonHelper.getAsJsonObject(bonus, "gem_class"), "types");
        if (values.isEmpty()) throw new IllegalArgumentException("gem_class.types must not be empty");
        Set<LootCategory> types = new LinkedHashSet<>();
        for (JsonElement value : values) types.add(category(GsonHelper.convertToString(value, "gem_class.types entry")));
        return types;
    }

    private static LootCategory category(String name) {
        LootCategory category = LootCategory.byId(name);
        if (category == null) throw new IllegalArgumentException("Unknown equipment category " + name);
        return category;
    }

    private static IllegalArgumentException conflict(LootCategory type) {
        return new IllegalArgumentException("Equipment category already belongs to another bonus: " + type.getName());
    }

    private static void checkFields(JsonObject object, Set<String> allowed) {
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException("Unsupported gem addition field " + key);
        }
    }
}
