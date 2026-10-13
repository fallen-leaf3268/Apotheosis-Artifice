package com.apotheosis_artifice.compat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;

public final class AffixCompatibilityDiagnostics {

    private static final Logger LOGGER = LogManager.getLogger("ApotheosisArtifice");
    private static final Set<Failure> REPORTED = ConcurrentHashMap.newKeySet();

    private AffixCompatibilityDiagnostics() {}

    public static void warnOnce(ResourceLocation affixId, String category, String stage, Exception exception) {
        if (REPORTED.add(new Failure(affixId, category, stage))) {
            LOGGER.warn("Affix {} failed for category {} during {}; skipping this result", affixId, category, stage, exception);
        }
    }

    private record Failure(ResourceLocation affixId, String category, String stage) {}
}
