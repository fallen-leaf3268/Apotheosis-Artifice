package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.locale.Language;

@Mixin(Language.class)
public class LanguageMixin {

    @Inject(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void apothArtifice$redirectGemClass(String key, CallbackInfoReturnable<String> cir) {
        Language language = (Language) (Object) this;
        if (language.has(key) || !key.equals(cir.getReturnValue())) return;
        String curiosKey = null;

        if (key.startsWith("gem_class.")) {
            curiosKey = mapGemClass(key);
        } else if (key.startsWith("text.apotheosis.category.")) {
            curiosKey = mapCategory(key);
        }

        if (curiosKey != null && language.has(curiosKey)) {
            cir.setReturnValue(language.getOrDefault(curiosKey));
        }
    }

    private static String mapGemClass(String key) {
        String gemKey = key.substring(10);
        String curiosId = switch (gemKey) {
            case "belt_protective" -> "belt";
            case "bracelet_balanced" -> "bracelet";
            case "charm_defensive" -> "charm";
            case "curse_violent" -> "hostility_curse";
            case "hands_offensive" -> "hands";
            case "necklace_arcane" -> "necklace";
            case "ring_defensive" -> "ring";
            case "curios_defensive" -> null;
            case "curios_necklace" -> "necklace";
            default -> null;
        };
        return curiosId != null ? "curios.identifier." + curiosId : null;
    }

    private static String mapCategory(String key) {
        String catKey = key.substring("text.apotheosis.category.".length());
        if (catKey.endsWith(".plural")) {
            catKey = catKey.substring(0, catKey.length() - 7);
        }
        if (!catKey.startsWith("curios:") || catKey.length() == 7) return null;
        return "curios.identifier." + catKey.substring(7);
    }
}
