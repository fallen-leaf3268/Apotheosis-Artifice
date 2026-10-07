package com.apotheosis_artifice.enchant;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.resources.ResourceLocation;

public final class EnchantingDiscountData implements CodecProvider<EnchantingDiscountData> {

    private static final Codec<EnchantingDiscounts.Bonuses> BONUSES_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PlaceboCodecs.nullableField(discountCodec(3), "lapis_discount", 0).forGetter(EnchantingDiscounts.Bonuses::lapis),
        PlaceboCodecs.nullableField(discountCodec(100), "experience_discount", 0).forGetter(EnchantingDiscounts.Bonuses::experience)
    ).apply(instance, EnchantingDiscounts.Bonuses::new));

    public static final Codec<EnchantingDiscountData> CODEC = RecordCodecBuilder.<EnchantingDiscountData>create(instance -> instance.group(
        PlaceboCodecs.nullableField(ResourceLocation.CODEC.listOf(), "blocks", List.of()).forGetter(EnchantingDiscountData::blocks),
        PlaceboCodecs.nullableField(ResourceLocation.CODEC, "block").forGetter(data -> Optional.empty()),
        PlaceboCodecs.nullableField(ResourceLocation.CODEC, "tag").forGetter(data -> Optional.empty()),
        BONUSES_CODEC.fieldOf("stats").forGetter(EnchantingDiscountData::bonuses)
    ).apply(instance, EnchantingDiscountData::new)).flatXmap(EnchantingDiscountData::validateTargets, EnchantingDiscountData::validateTargets);

    private final List<ResourceLocation> blocks;
    private final EnchantingDiscounts.Bonuses bonuses;

    private static Codec<Integer> discountCodec(int maximum) {
        return new PrimitiveCodec<Integer>() {
            @Override
            public <T> DataResult<Integer> read(DynamicOps<T> ops, T input) {
                if (input instanceof JsonElement json && (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber())) {
                    return DataResult.error(() -> "Discount must be a JSON number: " + input);
                }
                return ops.getNumberValue(input).flatMap(number -> {
                    try {
                        int value = new BigDecimal(number.toString()).intValueExact();
                        if (value >= 0 && value <= maximum) return DataResult.success(value);
                    } catch (ArithmeticException | NumberFormatException ignored) {}
                    return DataResult.error(() -> "Discount must be an integer within 0.." + maximum + ": " + number);
                });
            }

            @Override
            public <T> T write(DynamicOps<T> ops, Integer value) {
                return ops.createInt(value);
            }
        };
    }

    public EnchantingDiscountData(ResourceLocation block, int lapisDiscount, int experienceDiscount) {
        this(List.of(Objects.requireNonNull(block)), Optional.empty(), Optional.empty(), new EnchantingDiscounts.Bonuses(lapisDiscount, experienceDiscount));
        if (lapisDiscount < 0 || lapisDiscount > 3 || experienceDiscount < 0 || experienceDiscount > 100) {
            throw new IllegalArgumentException("Enchanting discounts must be within 0..3 lapis and 0..100 experience");
        }
    }

    private EnchantingDiscountData(List<ResourceLocation> blocks, Optional<ResourceLocation> block, Optional<ResourceLocation> tag,
        EnchantingDiscounts.Bonuses bonuses) {
        var targets = new LinkedHashSet<>(blocks);
        block.ifPresent(targets::add);
        tag.ifPresent(value -> targets.addAll(EnchantingDiscountRegistry.blocksForTag(value)));
        this.blocks = List.copyOf(targets);
        this.bonuses = bonuses;
    }

    private static DataResult<EnchantingDiscountData> validateTargets(EnchantingDiscountData data) {
        return data.blocks.isEmpty() ? DataResult.error(() -> "Enchanting stats discount definition has no target blocks") : DataResult.success(data);
    }

    public List<ResourceLocation> blocks() {
        return this.blocks;
    }

    public int lapisDiscount() {
        return this.bonuses.lapis();
    }

    public int experienceDiscount() {
        return this.bonuses.experience();
    }

    public EnchantingDiscounts.Bonuses bonuses() {
        return this.bonuses;
    }

    public static Map<ResourceLocation, EnchantingDiscounts.Bonuses> index(Map<ResourceLocation, EnchantingDiscountData> definitions) {
        Map<ResourceLocation, EnchantingDiscounts.Bonuses> index = new LinkedHashMap<>();
        definitions.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString()))
            .forEach(entry -> entry.getValue().blocks().forEach(block -> index.put(block, entry.getValue().bonuses())));
        return Map.copyOf(index);
    }

    @Override
    public Codec<? extends EnchantingDiscountData> getCodec() {
        return CODEC;
    }
}
