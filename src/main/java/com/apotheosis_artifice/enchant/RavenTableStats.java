package com.apotheosis_artifice.enchant;

import net.minecraft.nbt.CompoundTag;

public class RavenTableStats {

    private float eterna;
    private float quanta;
    private float arcana;

    public RavenTableStats() { this(0, 0, 0); }

    public RavenTableStats(float eterna, float quanta, float arcana) {
        this.eterna = eterna;
        this.quanta = quanta;
        this.arcana = arcana;
    }

    public float eterna() { return eterna; }
    public float quanta() { return quanta; }
    public float arcana() { return arcana; }

    public void set(float eterna, float quanta, float arcana) {
        this.eterna = eterna;
        this.quanta = quanta;
        this.arcana = arcana;
    }

    public void save(CompoundTag tag) {
        tag.putFloat("raven_eterna", this.eterna);
        tag.putFloat("raven_quanta", this.quanta);
        tag.putFloat("raven_arcana", this.arcana);
    }

    public void load(CompoundTag tag) {
        this.eterna = tag.getFloat("raven_eterna");
        this.quanta = tag.getFloat("raven_quanta");
        this.arcana = tag.getFloat("raven_arcana");
    }
}
