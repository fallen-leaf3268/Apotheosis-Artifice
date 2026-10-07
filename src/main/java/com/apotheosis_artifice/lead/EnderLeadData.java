package com.apotheosis_artifice.lead;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class EnderLeadData {
    public static final String ENTITY_DATA = "entity_data";
    public static final String NAME = "name";
    private static final Pattern ENTITY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final int MAX_ENTITIES = 64;
    private static final int MAX_DEPTH = 16;

    private EnderLeadData() {}

    public static boolean containsEntity(CompoundTag itemTag) {
        if (itemTag == null || !itemTag.contains(ENTITY_DATA)) return false;
        return !(itemTag.get(ENTITY_DATA) instanceof CompoundTag data) || !data.isEmpty();
    }

    public static Optional<CompoundTag> entityTag(CompoundTag itemTag) {
        if (itemTag == null || !itemTag.contains(ENTITY_DATA, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag entity = itemTag.getCompound(ENTITY_DATA);
        return entity.isEmpty() ? Optional.empty() : Optional.of(entity.copy());
    }

    public static CompoundTag shareTag(CompoundTag itemTag) {
        if (itemTag == null) return null;
        CompoundTag shared = itemTag.copy();
        if (itemTag.contains(ENTITY_DATA, Tag.TAG_COMPOUND)) {
            CompoundTag entity = new CompoundTag();
            CompoundTag stored = itemTag.getCompound(ENTITY_DATA);
            if (stored.contains("id", Tag.TAG_STRING)) entity.putString("id", stored.getString("id"));
            shared.put(ENTITY_DATA, entity);
        }
        return shared;
    }

    public static void clear(CompoundTag itemTag) {
        itemTag.remove(ENTITY_DATA);
        itemTag.remove(NAME);
    }

    public static Optional<List<StoredEntity>> tree(CompoundTag entityTag, Predicate<String> knownType) {
        List<StoredEntity> entities = new ArrayList<>();
        return collect(entityTag, knownType, new HashSet<>(), entities, 0)
            ? Optional.of(List.copyOf(entities)) : Optional.empty();
    }

    private static boolean collect(CompoundTag tag, Predicate<String> knownType, Set<UUID> uuids,
        List<StoredEntity> entities, int depth) {
        if (depth > MAX_DEPTH || entities.size() >= MAX_ENTITIES || !tag.contains("id", Tag.TAG_STRING) || !tag.hasUUID("UUID")) return false;
        String id = tag.getString("id");
        if (!ENTITY_ID.matcher(id).matches() || !knownType.test(id)) return false;
        UUID uuid = tag.getUUID("UUID");
        if (!uuids.add(uuid)) return false;
        entities.add(new StoredEntity(id, uuid));
        Tag rawPassengers = tag.get("Passengers");
        if (rawPassengers == null) return true;
        if (!(rawPassengers instanceof ListTag passengers) || !passengers.isEmpty() && passengers.getElementType() != Tag.TAG_COMPOUND) return false;
        for (int i = 0; i < passengers.size(); i++) if (!collect(passengers.getCompound(i), knownType, uuids, entities, depth + 1)) return false;
        return true;
    }

    public static boolean capture(CompoundTag itemTag, Supplier<CompoundTag> serialize, BooleanSupplier remove, String name) {
        if (containsEntity(itemTag)) return false;
        CompoundTag entity = serialize.get();
        if (entity == null || tree(entity, id -> true).isEmpty()) return false;
        Tag previousData = itemTag.get(ENTITY_DATA);
        Tag previousName = itemTag.get(NAME);
        itemTag.put(ENTITY_DATA, entity.copy());
        itemTag.putString(NAME, name);
        if (remove.getAsBoolean()) return true;
        restore(itemTag, ENTITY_DATA, previousData);
        restore(itemTag, NAME, previousName);
        return false;
    }

    private static void restore(CompoundTag itemTag, String key, Tag previous) {
        if (previous == null) itemTag.remove(key);
        else itemTag.put(key, previous);
    }

    public static <E> boolean release(CompoundTag itemTag, List<E> entities, Predicate<E> insert,
        Consumer<E> undo, Runnable consumeUse) {
        if (!containsEntity(itemTag) || entities.isEmpty()) return false;
        List<E> attempted = new ArrayList<>();
        boolean complete = true;
        try {
            for (E entity : entities) {
                attempted.add(entity);
                if (!insert.test(entity)) {
                    complete = false;
                    break;
                }
            }
        } catch (RuntimeException error) {
            try { rollback(attempted, undo); }
            catch (RuntimeException rollbackError) { error.addSuppressed(rollbackError); }
            throw error;
        }
        if (!complete) {
            rollback(attempted, undo);
            return false;
        }
        clear(itemTag);
        consumeUse.run();
        return true;
    }

    private static <E> void rollback(List<E> entities, Consumer<E> undo) {
        RuntimeException failure = null;
        for (int i = entities.size() - 1; i >= 0; i--) {
            try { undo.accept(entities.get(i)); }
            catch (RuntimeException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }

    public record StoredEntity(String typeId, UUID uuid) {}
}
