package com.apotheosis_artifice.client;

import java.util.List;

import dev.shadowsoffire.attributeslib.client.AttributesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

public class AttributesGuiHooks {

    public static EditBox nameBox;

    private static boolean registered = false;

    public static void ensureRegistered() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(new Object() {
            @net.minecraftforge.eventbus.api.SubscribeEvent
            public void onCharTyped(ScreenEvent.CharacterTyped event) {
                EditBox box = AttributesGuiHooks.nameBox;
                if (box == null || !box.isFocused()) return;
                if (box.charTyped(event.getCodePoint(), event.getModifiers())) {
                    event.setCanceled(true);
                }
            }
        });
    }

    public static void init(AttributesGui gui, int leftPos, int topPos) {
        ensureRegistered();
        Font font = Minecraft.getInstance().font;
        nameBox = new EditBox(font, leftPos + 40, topPos + 4, 68, 10, Component.empty());
        nameBox.setBordered(true);
        nameBox.setTextColor(0xFFFFFF);
        nameBox.setCanLoseFocus(true);
    }

    public static void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks, boolean open) {
        if (!open || nameBox == null) return;
        // 先渲染 EditBox（背景/边框/光标/文字）
        nameBox.render(gfx, mouseX, mouseY, partialTicks);
        // 空且未聚焦时显示占位文字
        if (!nameBox.isFocused() && nameBox.getValue().isEmpty()) {
            gfx.drawString(Minecraft.getInstance().font,
                net.minecraft.network.chat.Component.translatable("gui.apotheosis_artifice.attribute_search"),
                nameBox.getX() + 2, nameBox.getY() + 1, 0x666666, false);
        }
    }

    public static void mouseClicked(double mouseX, double mouseY, int button) {
        if (nameBox == null) return;
        nameBox.setFocused(false);
        if (mouseX >= nameBox.getX() && mouseX <= nameBox.getX() + nameBox.getWidth()
            && mouseY >= nameBox.getY() && mouseY <= nameBox.getY() + nameBox.getHeight()) {
            nameBox.setFocused(true);
            nameBox.mouseClicked(mouseX, mouseY, button);
        }
    }

    public static void refreshData(List<AttributeInstance> data) {
        if (nameBox == null) return;
        String search = nameBox.getValue().toLowerCase();
        if (search.isEmpty()) return;
        data.removeIf(inst -> !I18n.get(inst.getAttribute().getDescriptionId()).toLowerCase().contains(search));
    }
}
