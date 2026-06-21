package com.apotheosis_artifice.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.datafixers.util.Either;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.client.AdventureModuleClient;
import dev.shadowsoffire.apotheosis.adventure.client.SocketTooltipRenderer.SocketComponent;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.client.event.RenderTooltipEvent;

@Mixin(value = AdventureModuleClient.class, remap = false)
public class AffixTooltipMixin {

    @Inject(method = "comps", at = @At("HEAD"), cancellable = true)
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
        list.removeIf(c -> {
            Optional<FormattedText> o = c.left();
            return o.isPresent() && o.get() instanceof Component comp
                && comp.getContents() instanceof LiteralContents tc
                && "APOTH_REMOVE_MARKER".equals(tc.text());
        });
        int insertAt = list.size();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).left().isPresent()) {
                String txt = list.get(i).left().get().getString().trim();
                if (txt.startsWith("+") || txt.startsWith("-")) {
                    insertAt = i + 1;
                    break;
                }
            }
        }
        list.add(Math.min(insertAt, list.size()), Either.right(new SocketComponent(stack, SocketHelper.getGems(stack))));
        ci.cancel();
    }
}
