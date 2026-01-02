package com.example.emeraldmod.mixin;

import com.example.emeraldmod.effect.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin untuk block dan remove semua negative effects saat player memiliki Negative Immunity effect
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityNegativeImmunityMixin {

    /**
     * Inject ke method canHaveStatusEffect untuk block negative effects
     * Method ini dipanggil SEBELUM effect ditambahkan
     */
    @Inject(method = "canHaveStatusEffect", at = @At("HEAD"), cancellable = true)
    private void emeraldmod$blockNegativeEffects(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Cek apakah entity memiliki Negative Immunity effect
        if (entity.hasStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY)) {
            RegistryEntry<StatusEffect> effectEntry = effect.getEffectType();
            StatusEffect statusEffect = effectEntry.value();

            // Block semua negative effects (HARMFUL category)
            if (statusEffect.getCategory() == StatusEffectCategory.HARMFUL) {
                cir.setReturnValue(false); // BLOCK effect ini dari ditambahkan
            }
        }
    }

    /**
     * ⭐ IMPROVED: Inject ke method addStatusEffect - RETURN position
     * Remove negative effects yang ada SAAT Negative Immunity ditambahkan
     */
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("RETURN"))
    private void emeraldmod$removeExistingNegativeEffectsAfter(StatusEffectInstance effect, net.minecraft.entity.Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Ketika Negative Immunity effect berhasil ditambahkan
        if (effect.getEffectType().equals(ModEffects.NEGATIVE_IMMUNITY_ENTRY) && cir.getReturnValue()) {
            // Collect negative effects untuk dihapus
            java.util.List<RegistryEntry<StatusEffect>> effectsToRemove = new java.util.ArrayList<>();

            for (StatusEffectInstance activeEffect : entity.getStatusEffects()) {
                StatusEffect statusEffect = activeEffect.getEffectType().value();
                if (statusEffect.getCategory() == StatusEffectCategory.HARMFUL) {
                    effectsToRemove.add(activeEffect.getEffectType());
                }
            }

            // Remove semua negative effects
            for (RegistryEntry<StatusEffect> effectToRemove : effectsToRemove) {
                entity.removeStatusEffect(effectToRemove);
            }
        }
    }

    /**
     * ⭐ ADDITIONAL: Inject ke method addStatusEffect - HEAD position untuk immediate block
     * Mencegah negative effects ditambahkan jika Negative Immunity sudah aktif
     */
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void emeraldmod$blockNegativeEffectsImmediately(StatusEffectInstance effect, net.minecraft.entity.Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Jika entity SUDAH memiliki Negative Immunity
        if (entity.hasStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY)) {
            StatusEffect statusEffect = effect.getEffectType().value();

            // Block negative effects dari ditambahkan sama sekali
            if (statusEffect.getCategory() == StatusEffectCategory.HARMFUL) {
                cir.setReturnValue(false);
            }
        }
    }
}