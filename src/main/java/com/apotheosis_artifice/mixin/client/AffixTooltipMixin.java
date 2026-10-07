package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.datafixers.util.Either;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.client.AdventureModuleClient;
import dev.shadowsoffire.apotheosis.adventure.client.SocketTooltipRenderer.SocketComponent;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.client.event.RenderTooltipEvent;

@Mixin(value = AdventureModuleClient.class, remap = false)
public class AffixTooltipMixin {

    @Inject(method = "comps", at = @At("RETURN"))
    private static void cf_repositionSocket(RenderTooltipEvent.GatherComponents e, CallbackInfo ci) {
        var stack = e.getItemStack();
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData == null || !afxData.contains("curio_artifice")) return;
        String val = afxData.getString("curio_artifice");
        if (!val.equals("curio") && !val.startsWith("curios:")) {
            return;
        }
        int sockets = SocketHelper.getSockets(stack);
        if (sockets == 0) return;

        var list = e.getTooltipElements();
        SocketComponent socket = null;
        int originalIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            var element = list.get(i);
            if (element.right().orElse(null) instanceof SocketComponent existing) {
                if (socket == null) {
                    socket = existing;
                    originalIndex = i;
                }
                list.remove(i--);
            } else if (element.left().orElse(null) instanceof Component component
                && component.getContents() instanceof LiteralContents literal
                && "APOTH_REMOVE_MARKER".equals(literal.text())) {
                list.remove(i--);
            }
        }
        int insertAt = originalIndex == -1 ? list.size() : Math.min(originalIndex, list.size());
        boolean curioAttributes = false;
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i).left().orElse(null) instanceof Component component)) {
                curioAttributes = false;
                continue;
            }
            if (artifice$matchesCurioTranslation(component, true)) {
                curioAttributes = true;
            } else if (curioAttributes && artifice$matchesCurioTranslation(component, false)) {
                insertAt = i + 1;
            } else if (!(component.getContents() instanceof LiteralContents literal
                && literal.text().isBlank() && component.getSiblings().isEmpty())) {
                curioAttributes = false;
            }
        }
        if (socket == null) socket = new SocketComponent(stack, SocketHelper.getGems(stack));
        list.add(insertAt, Either.right(socket));
    }

    @Unique
    private static boolean artifice$matchesCurioTranslation(Component component, boolean header) {
        if (component.getContents() instanceof TranslatableContents contents) {
            String key = contents.getKey();
            if (header ? key.startsWith("curios.modifiers.") && !key.startsWith("curios.modifiers.slots.")
                : key.startsWith("attribute.modifier.") || key.startsWith("curios.modifiers.slots.")) return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (artifice$matchesCurioTranslation(sibling, header)) return true;
        }
        return false;
    }
}
