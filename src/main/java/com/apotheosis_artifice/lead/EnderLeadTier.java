package com.apotheosis_artifice.lead;

public enum EnderLeadTier {
    NORMAL(64, false),
    OCCULT(1024, true);

    private final int durability;
    private final boolean unbreakable;

    EnderLeadTier(int durability, boolean unbreakable) {
        this.durability = durability;
        this.unbreakable = unbreakable;
    }

    public int durability() { return durability; }
    public boolean unbreakable() { return unbreakable; }

    public boolean canBind(boolean gardenEnabled, boolean spawnerEnabled) {
        return this == OCCULT && gardenEnabled && spawnerEnabled;
    }

    public boolean canCapture(CaptureTarget target) {
        return target.mob && target.alive && !target.boss && !target.blacklisted && !target.removed
            && !target.passenger && !target.vehicle && target.serializable;
    }

    public record CaptureTarget(boolean mob, boolean animal, boolean ambient, boolean water, boolean alive,
        boolean boss, boolean blacklisted, boolean removed, boolean passenger, boolean vehicle, boolean serializable) {}
}
