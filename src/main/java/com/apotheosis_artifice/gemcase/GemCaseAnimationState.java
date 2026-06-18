package com.apotheosis_artifice.gemcase;

import java.util.Random;

import net.minecraft.util.Mth;

public class GemCaseAnimationState {

    private static final int SWITCH_INTERVAL_MIN = 80;
    private static final int SWITCH_INTERVAL_MAX = 240;
    private static final int ANIMATION_DURATION = 40;

    private final Random random;
    private final int[] slotPositions;
    private int ticksUntilNextSwitch;
    private int animationTicks;
    private int swappingIndex1 = -1;
    private int swappingIndex2 = -1;
    private boolean isAnimating = false;

    public GemCaseAnimationState(Random random) {
        this.random = random;
        this.slotPositions = new int[16];
        for (int i = 0; i < 16; i++) this.slotPositions[i] = i;
        this.shuffleInitial();
        this.ticksUntilNextSwitch = this.getRandomSwitchInterval();
    }

    private void shuffleInitial() {
        for (int i = 0; i < 16; i++) {
            int j = this.random.nextInt(16);
            int temp = this.slotPositions[i];
            this.slotPositions[i] = this.slotPositions[j];
            this.slotPositions[j] = temp;
        }
    }

    public void tick(int activeGemCount, boolean isPlayerNearby) {
        if (activeGemCount < 4) return;

        if (this.isAnimating) {
            this.animationTicks++;
            if (this.animationTicks >= ANIMATION_DURATION) {
                this.completeSwap();
                this.isAnimating = false;
                this.ticksUntilNextSwitch = this.getRandomSwitchInterval();
            }
        }
        else if (isPlayerNearby) {
            this.ticksUntilNextSwitch--;
            if (this.ticksUntilNextSwitch <= 0) {
                this.startRandomSwap(activeGemCount);
            }
        }
    }

    private void startRandomSwap(int activeGemCount) {
        this.swappingIndex1 = this.random.nextInt(activeGemCount);
        this.swappingIndex2 = this.random.nextInt(activeGemCount);
        if (this.swappingIndex1 == this.swappingIndex2) {
            this.swappingIndex2 = (this.swappingIndex2 + 1) % activeGemCount;
        }
        this.isAnimating = true;
        this.animationTicks = 0;
    }

    private void completeSwap() {
        if (this.swappingIndex1 >= 0 && this.swappingIndex2 >= 0) {
            int temp = this.slotPositions[this.swappingIndex1];
            this.slotPositions[this.swappingIndex1] = this.slotPositions[this.swappingIndex2];
            this.slotPositions[this.swappingIndex2] = temp;
        }
    }

    public PositionInfo getPosition(int gemIndex, float partialTicks) {
        int baseSlot = this.slotPositions[gemIndex];
        if (!this.isAnimating || (gemIndex != this.swappingIndex1 && gemIndex != this.swappingIndex2)) {
            return new PositionInfo(baseSlot, 0, 0);
        }
        float progress = Mth.clamp((this.animationTicks + partialTicks) / ANIMATION_DURATION, 0, 1);
        progress = smoothStep(progress);
        int targetSlot = gemIndex == this.swappingIndex1 ? this.slotPositions[this.swappingIndex2] : this.slotPositions[this.swappingIndex1];
        int baseX = baseSlot % 4, baseZ = baseSlot / 4;
        int targetX = targetSlot % 4, targetZ = targetSlot / 4;
        return new PositionInfo(baseSlot, (targetX - baseX) * progress, (targetZ - baseZ) * progress);
    }

    private static float smoothStep(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private int getRandomSwitchInterval() {
        return SWITCH_INTERVAL_MIN + this.random.nextInt(SWITCH_INTERVAL_MAX - SWITCH_INTERVAL_MIN);
    }

    public record PositionInfo(int baseSlot, float offsetX, float offsetZ) {}
}
