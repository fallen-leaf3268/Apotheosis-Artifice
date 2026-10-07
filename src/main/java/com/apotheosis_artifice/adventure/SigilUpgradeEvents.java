package com.apotheosis_artifice.adventure;

import java.util.ArrayList;
import java.util.List;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.mixin.AttributeAffixAccessor;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AttributeAffix;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApotheosisArtificeMod.MODID)
public final class SigilUpgradeEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!Apotheosis.enableAdventure) {
            return;
        }
        ItemStack stack = event.getItemStack();
        var components = new ArrayList<Component>();
        if (SigilAffixHelper.hasPendingMalice(stack)) {
            components.add(Component.translatable("text.apotheosis_artifice.malice_pending")
                .withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
        }
        int uses = SigilAffixHelper.getMaliceUses(stack);
        if (uses > 0) {
            components.add(Component.literal(SigilAffixHelper.isExtremelyMalicious(stack) ? "\uD83C\uDF1F " : "\u2022 ")
                .append(Component.translatable("text.apotheosis_artifice.touched_by_malice", uses)).withStyle(ChatFormatting.RED));
        }
        if (SigilAffixHelper.hasSigilState(stack)) {
            List<AffixInstance> affixes = SigilAffixHelper.getAllAffixes(stack).values().stream().filter(AffixInstance::isValid).toList();
            for (int index = 0; index < event.getToolTip().size(); index++) {
                event.getToolTip().set(index, decorateTooltip(event.getToolTip().get(index), affixes));
            }
        }
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), components);
    }

    public static Component decorateAffix(AffixInstance instance, Component text) {
        if (instance.level() <= 1F) return text;
        return decorateAffixes(List.of(instance), text);
    }

    private static MutableComponent decorateAffixes(List<AffixInstance> instances, Component text) {
        MutableComponent result = Component.empty().setStyle(text.getStyle());
        for (AffixInstance instance : instances) {
            ChatFormatting color = instance.level() >= 2F ? ChatFormatting.RED : ChatFormatting.BLUE;
            Component hover = instance.getName(true).copy().append(": ").append(Component.translatable("text.apotheosis_artifice.affix_power",
                Affix.fmt(100F * instance.level())));
            result.append(Component.literal("\uD83C\uDF1F ").withStyle(style -> style.withColor(color)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))));
        }
        return result.append(text.copy());
    }

    private static Component decorateTooltip(Component text, List<AffixInstance> affixes) {
        MutableComponent core;
        if (text.getContents() instanceof TranslatableContents contents) {
            Object[] arguments = contents.getArgs().clone();
            if (contents.getKey().equals("text.apotheosis.dot_prefix") && arguments.length == 1
                && arguments[0] instanceof Component child && affixes.stream().anyMatch(instance -> instance.level() > 1F
                    && matchesAffix(child, instance))) {
                MutableComponent result = Component.empty().setStyle(text.getStyle()).append(decorateTooltip(child, affixes));
                for (Component sibling : text.getSiblings()) result.append(decorateTooltip(sibling, affixes));
                return result;
            }
            for (int index = 0; index < arguments.length; index++) {
                if (arguments[index] instanceof Component child) arguments[index] = decorateTooltip(child, affixes);
            }
            core = MutableComponent.create(new TranslatableContents(contents.getKey(), contents.getFallback(), arguments));
        } else {
            core = MutableComponent.create(text.getContents());
        }
        core.setStyle(text.getStyle());
        var marked = new ArrayList<AffixInstance>();
        for (AffixInstance instance : affixes) {
            if (instance.level() <= 1F) continue;
            if (matchesAffix(text, instance)) marked.add(instance);
        }
        MutableComponent result = marked.isEmpty() ? core : decorateAffixes(marked, core);
        for (Component sibling : text.getSiblings()) result.append(decorateTooltip(sibling, affixes));
        return result;
    }

    private static boolean matchesAffix(Component text, AffixInstance instance) {
        if (instance.affix().get() instanceof AttributeAffix attribute) {
            if (!(text.getContents() instanceof TranslatableContents contents)) return false;
            String key = contents.getKey();
            if (!key.startsWith("attributeslib.modifier.") && !key.startsWith("attribute.modifier.")) return false;
            AttributeAffixAccessor accessor = (AttributeAffixAccessor) (Object) attribute;
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component child && containsTranslation(child, accessor.getAttribute().getDescriptionId())) return true;
            }
            return false;
        }
        Component description = instance.getDescription();
        return !description.getString().isEmpty() && text.getContents().equals(description.getContents());
    }

    private static boolean containsTranslation(Component text, String key) {
        if (text.getContents() instanceof TranslatableContents contents) {
            if (key.equals(contents.getKey())) return true;
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component child && containsTranslation(child, key)) return true;
            }
        }
        return text.getSiblings().stream().anyMatch(child -> containsTranslation(child, key));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !Apotheosis.enableAdventure) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            SigilAffixHelper.completePending(player, stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            SigilAffixHelper.completePending(player, stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            SigilAffixHelper.completePending(player, stack);
        }
    }
}
