package com.apotheosis_artifice.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.locale.Language;

@Mixin(Language.class)
public class LanguageMixin {

    @Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
    private void apothArtifice$redirectGemClass(String key, CallbackInfoReturnable<String> cir) {
        String curiosKey = null;

        if (key.startsWith("gem_class.")) {
            curiosKey = mapGemClass(key);
        } else if (key.startsWith("text.apotheosis.category.")) {
            curiosKey = mapCategory(key);
        }

        if (curiosKey != null) {
            String translated = Language.getInstance().getOrDefault(curiosKey);
            if (!translated.equals(curiosKey)) {
                cir.setReturnValue(translated);
            }
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
            // 通用 curio 走我们自己翻译（"任意饰品栏位"）
            case "curios_defensive" -> null;
            case "curios_necklace" -> "necklace";
            default -> null;
        };
        return curiosId != null ? "curios.identifier." + curiosId : null;
    }

    private static String mapCategory(String key) {
        String catKey = key.substring("text.apotheosis.category.".length());
        // strip .plural suffix
        if (catKey.endsWith(".plural")) {
            catKey = catKey.substring(0, catKey.length() - 7);
        }
        // extract slot id from curios:<slot> or just use the key directly
        String slotId;
        if (catKey.startsWith("curios:")) {
            slotId = catKey.substring(7);
        } else {
            // 通用 curio 走我们自己的翻译（"任意饰品栏位"），不借用 Curios
            if ("curio".equals(catKey)) return null;
            slotId = catKey;
        }
        return "curios.identifier." + slotId;
    }
}
