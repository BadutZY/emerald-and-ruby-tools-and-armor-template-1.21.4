package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.effect.ModEffects;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HorseArmorEffectsHandler {

    private static final Identifier SPEED_MODIFIER_ID = Identifier.of(EmeraldMod.MOD_ID, "horse_armor_speed");
    private static final double SPEED_BOOST_AMOUNT = 0.3; // 30% speed boost

    // Track pemain yang sedang menunggang kuda dengan mod armor
    private static final Set<UUID> CURRENTLY_RIDING_MOD_HORSE = new HashSet<>();

    // Swimming constants (untuk air)
    private static final double WATER_SURFACE_THRESHOLD = 0.5;
    private static final double SWIM_UPWARD_FORCE = 0.15;
    private static final double WATER_MOVEMENT_MULTIPLIER = 0.7;

    // Lava swimming constants (untuk lava)
    private static final double LAVA_SURFACE_THRESHOLD = 0.5;
    private static final double LAVA_SWIM_UPWARD_FORCE = 0.18;
    private static final double LAVA_MOVEMENT_MULTIPLIER = 0.6;

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world instanceof ServerWorld serverWorld) {
                applyHorseArmorEffects(serverWorld);
            }
        });

        EmeraldMod.LOGGER.info("✓ Registered Horse Armor Effects Handler (Emerald + Ruby)");
        EmeraldMod.LOGGER.info("  - Ruby Horse Armor: Negative Effect Immunity (Always Active)");
    }

    private static void applyHorseArmorEffects(ServerWorld world) {
        Set<UUID> ridingThisTick = new HashSet<>();

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getWorld() != world) continue;

            UUID playerUUID = player.getUuid();
            boolean isRidingModHorse = false;

            if (player.hasVehicle() && player.getVehicle() instanceof HorseEntity horse) {
                ItemStack horseArmor = horse.getBodyArmor();

                // ✅ CHECK: Emerald, Ruby, OR Netherite Horse Armor
                if (!horseArmor.isEmpty() &&
                        (horseArmor.getItem() == ModItems.EMERALD_HORSE_ARMOR ||
                                horseArmor.getItem() == ModItems.RUBY_HORSE_ARMOR)) {

                    isRidingModHorse = true;
                    ridingThisTick.add(playerUUID);

                    // Check if Ruby Horse Armor
                    boolean isRubyArmor = horseArmor.getItem() == ModItems.RUBY_HORSE_ARMOR;

                    // Apply effects ke kuda
                    applyEffectsToHorse(horse, isRubyArmor);

                    // ⭐ NEW: Clear negative effects dari kuda jika Ruby Armor
                    if (isRubyArmor) {
                        removeAllNegativeEffectsFromHorse(horse);
                    }

                    // Apply effects ke pemain (rider) - INFINITE DURATION
                    applyInfiniteEffectsToRider(player, horseArmor);

                    // Handle swimming mechanics (water & lava)
                    handleHorseSwimming(horse, player);
                    handleHorseLavaSwimming(horse, player);
                }
            }

            // Jika pemain TIDAK naik kuda mod, hapus effects
            if (!isRidingModHorse) {
                if (CURRENTLY_RIDING_MOD_HORSE.contains(playerUUID)) {
                    removeEffectsFromRider(player);
                    EmeraldMod.LOGGER.debug("Removed horse effects from player {} (dismounted)",
                            player.getName().getString());
                } else {
                    if (hasAnyHorseEffect(player)) {
                        removeEffectsFromRider(player);
                        EmeraldMod.LOGGER.debug("Cleaned up lingering horse effects from player {}",
                                player.getName().getString());
                    }
                }
            }
        }

        // Iterasi semua kuda untuk remove effects jika tidak pakai mod armor lagi
        world.iterateEntities().forEach(entity -> {
            if (entity instanceof HorseEntity horse) {
                ItemStack armorStack = horse.getBodyArmor();

                // Jika kuda tidak pakai mod armor, hapus effects
                if (armorStack.isEmpty() ||
                        (armorStack.getItem() != ModItems.EMERALD_HORSE_ARMOR &&
                                armorStack.getItem() != ModItems.RUBY_HORSE_ARMOR)) {
                    removeEffectsFromHorse(horse);
                }
            }
        });

        // Update tracking set
        CURRENTLY_RIDING_MOD_HORSE.clear();
        CURRENTLY_RIDING_MOD_HORSE.addAll(ridingThisTick);
    }

    private static void applyEffectsToHorse(HorseEntity horse, boolean isRubyArmor) {
        // 1. Speed Boost menggunakan attribute modifier
        EntityAttributeInstance speedAttribute = horse.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            if (!speedAttribute.hasModifier(SPEED_MODIFIER_ID)) {
                EntityAttributeModifier speedModifier = new EntityAttributeModifier(
                        SPEED_MODIFIER_ID,
                        SPEED_BOOST_AMOUNT,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                speedAttribute.addTemporaryModifier(speedModifier);
            }
        }

        // 2. Regeneration Effect untuk kuda (infinite)
        if (!hasInfiniteEffect(horse, StatusEffects.REGENERATION)) {
            horse.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.REGENERATION,
                    StatusEffectInstance.INFINITE,
                    1,
                    false,
                    false,
                    false
            ));
        }

        // 3. Jump Boost untuk kuda (infinite)
        if (!hasInfiniteEffect(horse, StatusEffects.JUMP_BOOST)) {
            horse.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.JUMP_BOOST,
                    StatusEffectInstance.INFINITE,
                    1,
                    false,
                    false,
                    false
            ));
        }

        // 4. Resistance untuk kuda (infinite)
        if (!hasInfiniteEffect(horse, StatusEffects.RESISTANCE)) {
            horse.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE,
                    StatusEffectInstance.INFINITE,
                    0,
                    false,
                    false,
                    false
            ));
        }

        // 5. Fire Resistance untuk kuda SAJA (infinite) - HIDDEN ICON
        if (!hasInfiniteEffect(horse, StatusEffects.FIRE_RESISTANCE)) {
            horse.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.FIRE_RESISTANCE,
                    StatusEffectInstance.INFINITE,
                    0,
                    false,
                    false,
                    false
            ));
        }

        // ⭐ NEW: 6. Negative Immunity untuk Ruby Horse Armor ONLY
        if (isRubyArmor) {
            if (!hasInfiniteEffect(horse, ModEffects.NEGATIVE_IMMUNITY_ENTRY)) {
                horse.addStatusEffect(new StatusEffectInstance(
                        ModEffects.NEGATIVE_IMMUNITY_ENTRY,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        false // Hidden dari horse, visible dari player via icon
                ));
            }
        } else {
            // Remove Negative Immunity jika bukan Ruby Armor
            if (horse.hasStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY)) {
                horse.removeStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY);
            }
        }
    }

    /**
     * ⭐ NEW METHOD: Remove semua negative effects dari kuda
     * Hanya untuk Ruby Horse Armor
     */
    private static void removeAllNegativeEffectsFromHorse(HorseEntity horse) {
        // Collect list of negative effects untuk dihapus
        java.util.List<net.minecraft.registry.entry.RegistryEntry<StatusEffect>> effectsToRemove =
                new java.util.ArrayList<>();

        // Scan semua active effects pada kuda
        for (StatusEffectInstance activeEffect : horse.getStatusEffects()) {
            StatusEffect statusEffect = activeEffect.getEffectType().value();

            // Hanya collect HARMFUL effects (negative)
            if (statusEffect.getCategory() == StatusEffectCategory.HARMFUL) {
                effectsToRemove.add(activeEffect.getEffectType());
            }
        }

        // Remove semua negative effects yang sudah di-collect
        for (net.minecraft.registry.entry.RegistryEntry<StatusEffect> effectToRemove : effectsToRemove) {
            horse.removeStatusEffect(effectToRemove);
        }
    }

    private static void handleHorseSwimming(HorseEntity horse, PlayerEntity rider) {
        if (!horse.isTouchingWater()) {
            return;
        }

        Vec3d velocity = horse.getVelocity();
        double waterLevel = horse.getFluidHeight(net.minecraft.registry.tag.FluidTags.WATER);

        if (waterLevel > WATER_SURFACE_THRESHOLD) {
            double upwardForce = SWIM_UPWARD_FORCE;

            if (velocity.y < 0) {
                upwardForce += Math.abs(velocity.y) * 0.5;
            }

            horse.setVelocity(
                    velocity.x * WATER_MOVEMENT_MULTIPLIER,
                    Math.max(velocity.y + upwardForce, 0.0),
                    velocity.z * WATER_MOVEMENT_MULTIPLIER
            );

            horse.velocityModified = true;
            horse.setAir(horse.getMaxAir());
        } else {
            if (velocity.y < 0) {
                horse.setVelocity(
                        velocity.x * WATER_MOVEMENT_MULTIPLIER,
                        0.05,
                        velocity.z * WATER_MOVEMENT_MULTIPLIER
                );
                horse.velocityModified = true;
            }
        }
    }

    private static void handleHorseLavaSwimming(HorseEntity horse, PlayerEntity rider) {
        if (!horse.isInLava()) {
            return;
        }

        Vec3d velocity = horse.getVelocity();
        double lavaLevel = horse.getFluidHeight(net.minecraft.registry.tag.FluidTags.LAVA);

        if (lavaLevel > LAVA_SURFACE_THRESHOLD) {
            double upwardForce = LAVA_SWIM_UPWARD_FORCE;

            if (velocity.y < 0) {
                upwardForce += Math.abs(velocity.y) * 0.6;
            }

            horse.setVelocity(
                    velocity.x * LAVA_MOVEMENT_MULTIPLIER,
                    Math.max(velocity.y + upwardForce, 0.0),
                    velocity.z * LAVA_MOVEMENT_MULTIPLIER
            );

            horse.velocityModified = true;

            if (horse.isOnFire()) {
                horse.setFireTicks(0);
            }
        } else {
            if (velocity.y < 0) {
                horse.setVelocity(
                        velocity.x * LAVA_MOVEMENT_MULTIPLIER,
                        0.08,
                        velocity.z * LAVA_MOVEMENT_MULTIPLIER
                );
                horse.velocityModified = true;
            }

            if (horse.isOnFire()) {
                horse.setFireTicks(0);
            }
        }
    }

    private static void applyInfiniteEffectsToRider(PlayerEntity player, ItemStack horseArmor) {
        // INFINITE DURATION untuk semua effects
        // FIRE RESISTANCE DIHAPUS - HANYA UNTUK KUDA

        // Check if Ruby Horse Armor
        boolean isRubyArmor = horseArmor.getItem() == ModItems.RUBY_HORSE_ARMOR;

        // 1. Speed (Swiftness)
        if (!hasInfiniteEffect(player, StatusEffects.SPEED)) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED,
                    StatusEffectInstance.INFINITE,
                    1,
                    false,
                    false,
                    true
            ));
        }

        // 2. Regeneration
        if (!hasInfiniteEffect(player, StatusEffects.REGENERATION)) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.REGENERATION,
                    StatusEffectInstance.INFINITE,
                    1,
                    false,
                    false,
                    true
            ));
        }

        // 3. Jump Boost
        if (!hasInfiniteEffect(player, StatusEffects.JUMP_BOOST)) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.JUMP_BOOST,
                    StatusEffectInstance.INFINITE,
                    1,
                    false,
                    false,
                    true
            ));
        }

        // 4. Resistance
        if (!hasInfiniteEffect(player, StatusEffects.RESISTANCE)) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE,
                    StatusEffectInstance.INFINITE,
                    0,
                    false,
                    false,
                    true
            ));
        }

        // 5. Visual Indicators (for all horse armors)
        if (!hasInfiniteEffect(player, ModEffects.SWIMMING_HORSE_ENTRY)) {
            player.addStatusEffect(new StatusEffectInstance(
                    ModEffects.SWIMMING_HORSE_ENTRY,
                    StatusEffectInstance.INFINITE,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (!hasInfiniteEffect(player, ModEffects.HORSE_FIRE_ENTRY)) {
            player.addStatusEffect(new StatusEffectInstance(
                    ModEffects.HORSE_FIRE_ENTRY,
                    StatusEffectInstance.INFINITE,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (!hasInfiniteEffect(player, ModEffects.HORSE_LAVA_ENTRY)) {
            player.addStatusEffect(new StatusEffectInstance(
                    ModEffects.HORSE_LAVA_ENTRY,
                    StatusEffectInstance.INFINITE,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (!hasInfiniteEffect(player, ModEffects.HORSE_SNOW_ENTRY)) {
            player.addStatusEffect(new StatusEffectInstance(
                    ModEffects.HORSE_SNOW_ENTRY,
                    StatusEffectInstance.INFINITE,
                    0,
                    false,
                    false,
                    true
            ));
        }

        if (isRubyArmor) {
            if (!hasInfiniteEffect(player, ModEffects.NEGATIVE_IMMUN_HORSE_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.NEGATIVE_IMMUN_HORSE_ENTRY,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
        } else {
            // Remove indicator if not Ruby Armor
            if (player.hasStatusEffect(ModEffects.NEGATIVE_IMMUN_HORSE_ENTRY)) {
                player.removeStatusEffect(ModEffects.NEGATIVE_IMMUN_HORSE_ENTRY);
            }
        }
    }

    private static void removeEffectsFromRider(PlayerEntity player) {
        // FORCE REMOVE semua effects dari pemain ketika turun dari kuda

        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            player.removeStatusEffect(StatusEffects.SPEED);
        }

        if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
            player.removeStatusEffect(StatusEffects.REGENERATION);
        }

        if (player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            player.removeStatusEffect(StatusEffects.JUMP_BOOST);
        }

        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            player.removeStatusEffect(StatusEffects.RESISTANCE);
        }

        if (player.hasStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY)) {
            player.removeStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY);
        }

        if (player.hasStatusEffect(ModEffects.HORSE_FIRE_ENTRY)) {
            player.removeStatusEffect(ModEffects.HORSE_FIRE_ENTRY);
        }

        if (player.hasStatusEffect(ModEffects.HORSE_LAVA_ENTRY)) {
            player.removeStatusEffect(ModEffects.HORSE_LAVA_ENTRY);
        }

        if (player.hasStatusEffect(ModEffects.HORSE_SNOW_ENTRY)) {
            player.removeStatusEffect(ModEffects.HORSE_SNOW_ENTRY);
        }

        if (player.hasStatusEffect(ModEffects.NEGATIVE_IMMUN_HORSE_ENTRY)) {
            player.removeStatusEffect(ModEffects.NEGATIVE_IMMUN_HORSE_ENTRY);
        }
    }

    private static boolean hasAnyHorseEffect(PlayerEntity player) {
        return player.hasStatusEffect(StatusEffects.SPEED) ||
                player.hasStatusEffect(StatusEffects.REGENERATION) ||
                player.hasStatusEffect(StatusEffects.JUMP_BOOST) ||
                player.hasStatusEffect(StatusEffects.RESISTANCE) ||
                player.hasStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY) ||
                player.hasStatusEffect(ModEffects.HORSE_FIRE_ENTRY) ||
                player.hasStatusEffect(ModEffects.HORSE_LAVA_ENTRY) ||
                player.hasStatusEffect(ModEffects.HORSE_SNOW_ENTRY) ||
                player.hasStatusEffect(ModEffects.NEGATIVE_IMMUN_HORSE_ENTRY);
    }

    private static void removeEffectsFromHorse(HorseEntity horse) {
        // Hapus speed modifier jika ada
        EntityAttributeInstance speedAttribute = horse.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttribute != null && speedAttribute.hasModifier(SPEED_MODIFIER_ID)) {
            speedAttribute.removeModifier(SPEED_MODIFIER_ID);
        }

        // Hapus status effects dari kuda
        if (horse.hasStatusEffect(StatusEffects.REGENERATION)) {
            horse.removeStatusEffect(StatusEffects.REGENERATION);
        }
        if (horse.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            horse.removeStatusEffect(StatusEffects.JUMP_BOOST);
        }
        if (horse.hasStatusEffect(StatusEffects.RESISTANCE)) {
            horse.removeStatusEffect(StatusEffects.RESISTANCE);
        }
        if (horse.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            horse.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
        }
        if (horse.hasStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY)) {
            horse.removeStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY);
        }
        if (horse.hasStatusEffect(ModEffects.HORSE_FIRE_ENTRY)) {
            horse.removeStatusEffect(ModEffects.HORSE_FIRE_ENTRY);
        }
        if (horse.hasStatusEffect(ModEffects.HORSE_LAVA_ENTRY)) {
            horse.removeStatusEffect(ModEffects.HORSE_LAVA_ENTRY);
        }
        if (horse.hasStatusEffect(ModEffects.HORSE_SNOW_ENTRY)) {
            horse.removeStatusEffect(ModEffects.HORSE_SNOW_ENTRY);
        }
        if (horse.hasStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY)) {
            horse.removeStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY);
        }
    }

    private static boolean hasInfiniteEffect(net.minecraft.entity.LivingEntity entity, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
        StatusEffectInstance instance = entity.getStatusEffect(effect);
        if (instance == null) {
            return false;
        }
        return instance.isDurationBelow(0) || instance.getDuration() == StatusEffectInstance.INFINITE;
    }

    public static void cleanup() {
        CURRENTLY_RIDING_MOD_HORSE.clear();
    }
}