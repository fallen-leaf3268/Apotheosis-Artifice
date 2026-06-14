package com.apotheosis_artifice.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ApotheosisNetwork;
import com.apotheosis_artifice.ISlotSelectMenu;

import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Mixin(ReforgingScreen.class)
public abstract class ReforgingScreenMixin extends AbstractContainerScreen<ReforgingMenu> {

    @SuppressWarnings("unused")
    private ReforgingScreenMixin() { super(null, null, null); }

    @Unique
    private List<Button> curiosforge_slotButtons = new ArrayList<>();

    @Unique
    private ItemStack curiosforge_lastSlotItem = ItemStack.EMPTY;

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void curiosforge_checkItem(net.minecraft.client.gui.GuiGraphics gfx, float partials, int x, int y, CallbackInfo ci) {
        ItemStack stack = this.menu.getSlot(0).getItem();
        if (ItemStack.matches(stack, this.curiosforge_lastSlotItem)) return;
        this.curiosforge_lastSlotItem = stack.copy();
        curiosforge_rebuild(stack);
    }

    @Unique
    private void curiosforge_rebuild(ItemStack stack) {
        for (Button b : this.curiosforge_slotButtons) {
            this.removeWidget(b);
        }
        this.curiosforge_slotButtons.clear();

        if (stack.isEmpty()) return;

        List<String> cats = ((ISlotSelectMenu) this.menu).curiosforge_getAvailableSlots();
        if (cats.size() <= 1) return;

        int left = this.getGuiLeft();
        int top = this.getGuiTop();
        int btnX = left + this.imageWidth + 4;

        for (int idx = 0; idx < cats.size(); idx++) {
            int y = top + 12 + idx * 16;
            String catId = cats.get(idx);
            final int fIdx = idx;

            Component label;
            if (catId.startsWith("curio:")) {
                label = Component.translatable("curios.identifier." + catId.substring(6));
            } else {
                label = Component.translatable("text.apotheosis.category." + catId);
            }

            Button btn = Button.builder(label, b -> {
                ApotheosisArtificeMod.LOGGER.info("[Screen] Button clicked: idx={} cat={}", fIdx, catId);
                ((ISlotSelectMenu) ReforgingScreenMixin.this.menu).curiosforge_selectSlot(fIdx);
                ApotheosisNetwork.CHANNEL.sendToServer(new ApotheosisNetwork.SlotSelectPacket(fIdx));
            })
                .bounds(btnX, y, 60, 14)
                .build();
            this.curiosforge_slotButtons.add(btn);
            this.addRenderableWidget(btn);
        }
    }
}
