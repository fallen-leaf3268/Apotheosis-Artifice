package com.apotheosis_artifice.mixin;

import java.util.function.BiConsumer;

import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ApotheosisConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.FestiveAffix;
import dev.shadowsoffire.apotheosis.adventure.AdventureEvents;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketedGems;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;

@Mixin(value = AdventureEvents.class, remap = false)
public class AdventureEventsMixin {

    @Redirect(method = "affixModifiers", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/apotheosis/adventure/socket/SocketedGems;addModifiers(Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"))
    private void cf_skipCurioGems(SocketedGems gems, LootCategory category, EquipmentSlot slot,
        BiConsumer<Attribute, AttributeModifier> modifiers, ItemAttributeModifierEvent event) {
        if (!cf_isCurioItem(event.getItemStack())) gems.addModifiers(category, slot, modifiers);
    }

    @ModifyArg(method = "affixModifiers", at = @At(value = "INVOKE",
        target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"), index = 0)
    private BiConsumer<DynamicHolder<? extends Affix>, AffixInstance> cf_skipCurioAffixes(
        BiConsumer<DynamicHolder<? extends Affix>, AffixInstance> original) {
        return (affix, instance) -> {
            if (!cf_isCurioItem(instance.stack())) original.accept(affix, instance);
        };
    }

    @Unique
    private static boolean cf_isCurioItem(ItemStack stack) {
        var afxData = stack.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData == null) return false;
        String category = afxData.getString("curio_artifice");
        return category.equals("curio") || category.startsWith("curios:");
    }

    @Mixin(value = FestiveAffix.class, remap = false)
    public static class LootPinataEffects {
        @ModifyArg(method = "drops", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            remap = true), index = 6, require = 1, allow = 1)
        private float artifice$mutePinataExplosion(Player player, double x, double y, double z,
            SoundEvent sound, SoundSource source, float volume, float pitch) {
            return ApotheosisConfig.getLootPinataEffect() != ApotheosisConfig.LootPinataEffect.EXPLOSION
                && sound == SoundEvents.GENERIC_EXPLODE ? 0F : volume;
        }

        @ModifyArg(method = "drops", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            remap = true), index = 0, require = 1, allow = 1)
        private ParticleOptions artifice$replacePinataParticles(ParticleOptions particle, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
            var effect = ApotheosisConfig.getLootPinataEffect();
            if (effect != ApotheosisConfig.LootPinataEffect.EXPLOSION && particle == ParticleTypes.EXPLOSION_EMITTER) {
                return effect == ApotheosisConfig.LootPinataEffect.FIREWORKS
                    ? ApotheosisArtificeMod.LOOT_PINATA_FIREWORK.get() : ApotheosisArtificeMod.LOOT_PINATA_SILENT.get();
            }
            return particle;
        }

        @ModifyArg(method = "drops", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            remap = true), index = 4, require = 1, allow = 1)
        private int artifice$pinataParticleCount(ParticleOptions particle, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
            return artifice$isPinataEffect(particle) ? 1 : count;
        }

        @ModifyArg(method = "drops", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            remap = true), index = 5, require = 1, allow = 1)
        private double artifice$pinataParticleOffsetX(ParticleOptions particle, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
            return artifice$isPinataEffect(particle) ? 0D : dx;
        }

        @ModifyArg(method = "drops", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            remap = true), index = 6, require = 1, allow = 1)
        private double artifice$pinataParticleOffsetY(ParticleOptions particle, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
            return artifice$isPinataEffect(particle) ? 0D : dy;
        }

        @ModifyArg(method = "drops", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            remap = true), index = 7, require = 1, allow = 1)
        private double artifice$pinataParticleOffsetZ(ParticleOptions particle, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
            return artifice$isPinataEffect(particle) ? 0D : dz;
        }

        @ModifyArg(method = "drops", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            remap = true), index = 8, require = 1, allow = 1)
        private double artifice$pinataParticleSpeed(ParticleOptions particle, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
            return artifice$isPinataEffect(particle) ? 0D : speed;
        }

        @Unique
        private static boolean artifice$isPinataEffect(ParticleOptions particle) {
            return ApotheosisConfig.getLootPinataEffect() != ApotheosisConfig.LootPinataEffect.EXPLOSION
                && (particle == ParticleTypes.EXPLOSION_EMITTER
                    || particle == ApotheosisArtificeMod.LOOT_PINATA_FIREWORK.get()
                    || particle == ApotheosisArtificeMod.LOOT_PINATA_SILENT.get());
        }
    }
}
