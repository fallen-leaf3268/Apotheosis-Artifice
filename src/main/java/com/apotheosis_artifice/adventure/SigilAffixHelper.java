package com.apotheosis_artifice.adventure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.apotheosis_artifice.affix.AttributeBaseAffix;
import com.apotheosis_artifice.affix.CurioSlotBonusAffix;
import com.apotheosis_artifice.affix.DamageResistanceAffix;
import com.apotheosis_artifice.affix.EffectImmunityAffix;
import com.apotheosis_artifice.affix.RadianceAffix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.CatalyzingAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.CleavingAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.DamageReductionAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.EnlightenedAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.ExecutingAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.FestiveAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.MagicalArrowAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.OmneticAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.PotionAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.PsychicAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.RadialAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.RetreatingAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.SpectralShotAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.TelepathicAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.ThunderstruckAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

public final class SigilAffixHelper {

    public static final String MALICE_PENDING = "apotheosis_artifice:malice_pending";
    public static final String MALICE_USES = "apotheosis_artifice:malice_uses";
    public static final String MALICE_HISTORY = "apotheosis_artifice:malice_history";
    public static final String AFFIX_LIMITS = "apotheosis_artifice:affix_limits";
    public static final String SUPREMACY_USED = "apotheosis_artifice:supremacy_used";

    public static boolean canApplySupremacy(ItemStack stack) {
        if (!Apotheosis.enableAdventure || stack.isEmpty()) {
            return false;
        }
        CompoundTag data = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (data == null) {
            return false;
        }
        CompoundTag values = data.getCompound(AffixHelper.AFFIXES);
        return values.getAllKeys().stream().anyMatch(id -> values.contains(id, Tag.TAG_ANY_NUMERIC)
            && Float.isFinite(values.getFloat(id)) && values.getFloat(id) < 1.5F);
    }

    public static boolean canApplyMalice(ItemStack stack) {
        return SigilUpgradePolicy.canApplyMalice(snapshot(instances(stack)));
    }

    public static boolean isExtremelyMalicious(ItemStack stack) {
        return SigilUpgradePolicy.isExtremelyMalicious(snapshot(instances(stack)));
    }

    public static boolean hasPendingMalice(ItemStack stack) {
        CompoundTag data = stack.getTagElement(AffixHelper.AFFIX_DATA);
        return !stack.isEmpty() && data != null && data.getBoolean(MALICE_PENDING);
    }

    public static int getMaliceUses(ItemStack stack) {
        CompoundTag data = stack.getTagElement(AffixHelper.AFFIX_DATA);
        return data == null ? 0 : data.getInt(MALICE_USES);
    }

    public static Map<DynamicHolder<? extends Affix>, AffixInstance> getAllAffixes(ItemStack stack) {
        if (!Apotheosis.enableAdventure || stack.isEmpty() || AffixRegistry.INSTANCE.getValues().isEmpty()) {
            return Map.of();
        }
        CompoundTag data = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (data == null || !data.contains(AffixHelper.AFFIXES, Tag.TAG_COMPOUND)) {
            return Map.of();
        }
        DynamicHolder<LootRarity> rarity = AffixHelper.getRarity(data);
        if (!rarity.isBound()) {
            rarity = RarityRegistry.getMinRarity();
        }
        if (!rarity.isBound()) {
            return Map.of();
        }
        CompoundTag values = data.getCompound(AffixHelper.AFFIXES);
        LootCategory category = LootCategory.forItem(stack);
        Map<DynamicHolder<? extends Affix>, AffixInstance> result = new LinkedHashMap<>();
        Map<DynamicHolder<? extends Affix>, AffixInstance> compatible = null;
        for (String key : values.getAllKeys().stream().sorted().toList()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null || !values.contains(key, Tag.TAG_ANY_NUMERIC)) {
                continue;
            }
            DynamicHolder<Affix> affix = AffixRegistry.INSTANCE.holder(id);
            float level = values.getFloat(key);
            if (!Float.isFinite(level) || !affix.isBound()) {
                continue;
            }
            DynamicHolder<LootRarity> effectiveRarity = rarity;
            if (!affix.get().canApplyTo(stack, category, rarity.get())) {
                if (compatible == null) {
                    ItemStack copy = stack.copy();
                    CompoundTag safeValues = new CompoundTag();
                    for (String candidate : values.getAllKeys()) {
                        ResourceLocation candidateId = ResourceLocation.tryParse(candidate);
                        if (candidateId != null && values.contains(candidate, Tag.TAG_ANY_NUMERIC)
                            && Float.isFinite(values.getFloat(candidate)) && AffixRegistry.INSTANCE.holder(candidateId).isBound()) {
                            safeValues.putFloat(candidate, values.getFloat(candidate));
                        }
                    }
                    copy.getOrCreateTagElement(AffixHelper.AFFIX_DATA).put(AffixHelper.AFFIXES, safeValues);
                    compatible = AffixHelper.getAffixesImpl(copy);
                }
                AffixInstance fallback = compatible.get(affix);
                if (fallback == null || !fallback.isValid() || !fallback.affix().equals(affix)
                    || !affix.get().canApplyTo(stack, category, fallback.rarity().get())) {
                    continue;
                }
                effectiveRarity = fallback.rarity();
            }
            result.put(affix, new AffixInstance(affix, stack, effectiveRarity, level));
        }
        return Collections.unmodifiableMap(result);
    }

    public static float getMaximum(AffixInstance instance) {
        return getMaximum(instance.stack().getTagElement(AffixHelper.AFFIX_DATA), instance.affix().getId().toString(), instance.level());
    }

    public static float getMaximum(CompoundTag data, String id, float level) {
        return Float.isFinite(level) ? Math.max(1F, Math.min(SigilUpgradePolicy.MALICE_LEVEL, level)) : 1F;
    }

    public static boolean hasSigilState(ItemStack stack) {
        CompoundTag data = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (data == null) {
            return false;
        }
        if (data.contains(MALICE_PENDING) || data.contains(MALICE_USES) || data.contains(MALICE_HISTORY)
            || data.contains(AFFIX_LIMITS) || data.contains(SUPREMACY_USED)) {
            return true;
        }
        CompoundTag values = data.getCompound(AffixHelper.AFFIXES);
        return values.getAllKeys().stream().anyMatch(id -> values.getFloat(id) > 1F);
    }

    public static void clearMaliceState(ItemStack stack) {
        CompoundTag data = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (data != null) {
            data.remove(MALICE_PENDING);
            data.remove(MALICE_USES);
            data.remove(MALICE_HISTORY);
            data.remove(AFFIX_LIMITS);
            data.remove(SUPREMACY_USED);
        }
    }

    public static void applySupremacy(ItemStack stack) {
        if (!canApplySupremacy(stack)) {
            return;
        }
        CompoundTag data = stack.getTagElement(AffixHelper.AFFIX_DATA);
        CompoundTag values = data.getCompound(AffixHelper.AFFIXES);
        for (String id : values.getAllKeys()) {
            if (values.contains(id, Tag.TAG_ANY_NUMERIC) && Float.isFinite(values.getFloat(id))) {
                values.putFloat(id, Math.max(values.getFloat(id), 1.5F));
            }
        }
        clearUpgradeLimits(data);
    }

    public static void applyMalicePreview(ItemStack stack, int boosted, int reset) {
        List<SigilUpgradePolicy.Affix> affixes = snapshot(instances(stack));
        if (!SigilUpgradePolicy.canApplyMalice(affixes) || boosted < 0 || boosted >= affixes.size()
            || reset < -1 || reset >= affixes.size() || reset == boosted) {
            return;
        }
        SigilUpgradePolicy.Affix buff = affixes.get(boosted);
        if (!buff.valid() || buff.levelIndependent()) {
            return;
        }
        if (reset >= 0) {
            SigilUpgradePolicy.Affix removed = affixes.get(reset);
            if (!removed.valid() || removed.levelIndependent()) {
                return;
            }
        }
        var output = new ArrayList<>(affixes);
        float level = Math.max(buff.level(), SigilUpgradePolicy.MALICE_LEVEL);
        output.set(boosted, new SigilUpgradePolicy.Affix(buff.id(), level, buff.valid(), buff.levelIndependent(), buff.durability(),
            level, false));
        if (reset >= 0) {
            SigilUpgradePolicy.Affix removed = affixes.get(reset);
            output.set(reset, new SigilUpgradePolicy.Affix(removed.id(), 0F, removed.valid(), removed.levelIndependent(), removed.durability(),
                1F, false));
        }
        setLevels(stack, output);
        stack.getOrCreateTagElement(AffixHelper.AFFIX_DATA).putInt(MALICE_USES, getMaliceUses(stack) + 1);
    }

    public static ItemStack prepareMalice(ItemStack input) {
        ItemStack output = input.copy();
        output.getOrCreateTagElement(AffixHelper.AFFIX_DATA).putBoolean(MALICE_PENDING, true);
        return output;
    }

    public static boolean completePending(Player player, ItemStack stack) {
        return SigilUpgradePolicy.consumePending(!player.level().isClientSide, Apotheosis.enableAdventure,
            () -> hasPendingMalice(stack),
            () -> stack.getTagElement(AffixHelper.AFFIX_DATA).remove(MALICE_PENDING), () -> applyMalice(player, stack));
    }

    public static boolean isLevelIndependent(AffixInstance inst) {
        if (!inst.isValid()) {
            return true;
        }
        Affix affix = inst.affix().get();
        SigilUpgradePolicy.ValueKind kind = valueKind(affix);
        if (kind == SigilUpgradePolicy.ValueKind.FIXED || kind == SigilUpgradePolicy.ValueKind.UNKNOWN) {
            return true;
        }
        JsonElement definition = encode(affix);
        JsonElement rarityValues = null;
        if (definition != null && definition.isJsonObject()) {
            JsonObject object = definition.getAsJsonObject();
            if (object.has("values") && object.get("values").isJsonObject()) {
                JsonObject values = object.getAsJsonObject("values");
                rarityValues = values.get(inst.rarity().getId().toString());
                if (rarityValues == null) {
                    rarityValues = values.get(inst.rarity().getId().getPath());
                }
            }
        }
        return rarityValues == null || SigilUpgradePolicy.isLevelIndependent(kind, rarityValues);
    }

    private static List<AffixInstance> instances(ItemStack stack) {
        return getAllAffixes(stack).values().stream().filter(AffixInstance::isValid).toList();
    }

    private static List<SigilUpgradePolicy.Affix> snapshot(List<AffixInstance> instances) {
        return instances.stream().map(inst -> new SigilUpgradePolicy.Affix(inst.affix().getId().toString(), inst.level(),
            inst.isValid(), isLevelIndependent(inst), inst.affix().get().getType() == AffixType.DURABILITY, getMaximum(inst), false)).toList();
    }

    private static void setLevels(ItemStack stack, List<SigilUpgradePolicy.Affix> affixes) {
        CompoundTag data = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (data == null) {
            return;
        }
        CompoundTag values = data.getCompound(AffixHelper.AFFIXES);
        for (SigilUpgradePolicy.Affix affix : affixes) {
            if (affix.valid()) {
                values.putFloat(affix.id(), affix.level());
            }
        }
        data.put(AffixHelper.AFFIXES, values);
        clearUpgradeLimits(data);
    }

    private static void clearUpgradeLimits(CompoundTag data) {
        data.remove(AFFIX_LIMITS);
        data.remove(MALICE_HISTORY);
        data.remove(SUPREMACY_USED);
    }

    private static void applyMalice(Player player, ItemStack stack) {
        List<AffixInstance> instances = instances(stack);
        RandomSource random = new SingleThreadedRandomSource(player.getPersistentData().getInt(ReforgingMenu.REFORGE_SEED));
        SigilUpgradePolicy.MaliceResult result = SigilUpgradePolicy.malice(snapshot(instances), getMaliceUses(stack),
            new SigilUpgradePolicy.Rng() {
                @Override
                public int nextInt(int bound) {
                    return random.nextInt(bound);
                }

                @Override
                public float nextFloat() {
                    return random.nextFloat();
                }
            });
        if (!result.applied()) {
            return;
        }
        setLevels(stack, result.affixes());
        stack.getOrCreateTagElement(AffixHelper.AFFIX_DATA).putInt(MALICE_USES, result.uses());
        player.getPersistentData().putInt(ReforgingMenu.REFORGE_SEED, player.getRandom().nextInt());
        AffixInstance boosted = instances.get(result.boosted());
        Component boostedName = noticeName(new AffixInstance(boosted.affix(), stack, boosted.rarity(), result.affixes().get(result.boosted()).level()), ChatFormatting.YELLOW);
        if (result.reset() < 0) {
            player.sendSystemMessage(Component.translatable("message.apotheosis_artifice.malice_notice_final", boostedName));
        } else {
            player.sendSystemMessage(Component.translatable("message.apotheosis_artifice.malice_notice", boostedName,
                noticeName(instances.get(result.reset()), ChatFormatting.RED)));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS,
            1.0F, player.getRandom().nextFloat() * 0.4F + 0.8F);
    }

    private static Component noticeName(AffixInstance instance, ChatFormatting color) {
        return Component.literal("[").append(instance.getName(true)).append("]").withStyle(style -> style.withColor(color)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, instance.getAugmentingText())));
    }

    @SuppressWarnings("unchecked")
    private static JsonElement encode(Affix affix) {
        return ((Codec<Affix>) affix.getCodec()).encodeStart(JsonOps.INSTANCE, affix).result().orElse(null);
    }

    private static SigilUpgradePolicy.ValueKind valueKind(Affix affix) {
        if (affix instanceof OmneticAffix || affix instanceof MagicalArrowAffix || affix instanceof RetreatingAffix
            || affix instanceof TelepathicAffix || affix instanceof EffectImmunityAffix) {
            return SigilUpgradePolicy.ValueKind.FIXED;
        }
        if (affix instanceof CleavingAffix) {
            return SigilUpgradePolicy.ValueKind.CLEAVING;
        }
        if (affix instanceof PotionAffix) {
            return SigilUpgradePolicy.ValueKind.MOB_EFFECT;
        }
        if (affix instanceof RadialAffix) {
            return SigilUpgradePolicy.ValueKind.RADIAL;
        }
        if (affix instanceof AttributeAffix || affix instanceof CatalyzingAffix || affix instanceof DamageReductionAffix
            || affix instanceof EnlightenedAffix || affix instanceof ExecutingAffix || affix instanceof FestiveAffix
            || affix instanceof PsychicAffix || affix instanceof SpectralShotAffix || affix instanceof ThunderstruckAffix
            || affix instanceof AttributeBaseAffix || affix instanceof CurioSlotBonusAffix || affix instanceof DamageResistanceAffix
            || affix instanceof RadianceAffix) {
            return SigilUpgradePolicy.ValueKind.SCALAR;
        }
        return SigilUpgradePolicy.ValueKind.UNKNOWN;
    }
}
