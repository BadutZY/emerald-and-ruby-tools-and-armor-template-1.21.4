package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.effect.ModEffects;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HorseArmorEffectsHandler {

    private static final Identifier SPEED_MODIFIER_ID = Identifier.of(EmeraldMod.MOD_ID, "emerald_horse_speed");
    private static final double SPEED_BOOST_AMOUNT = 0.3; // 30% speed boost

    // Track pemain yang sedang menunggang kuda dengan emerald armor
    private static final Set<UUID> CURRENTLY_RIDING_EMERALD_HORSE = new HashSet<>();

    // Swimming constants (untuk air)
    private static final double WATER_SURFACE_THRESHOLD = 0.5; // Keep horse above water
    private static final double SWIM_UPWARD_FORCE = 0.15; // Upward force when in water
    private static final double WATER_MOVEMENT_MULTIPLIER = 0.7; // Movement speed in water

    // Lava swimming constants (untuk lava)
    private static final double LAVA_SURFACE_THRESHOLD = 0.5; // Keep horse above lava
    private static final double LAVA_SWIM_UPWARD_FORCE = 0.18; // Upward force when in lava (slightly stronger than water)
    private static final double LAVA_MOVEMENT_MULTIPLIER = 0.6; // Movement speed in lava (slower than water)

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world instanceof ServerWorld serverWorld) {
                applyHorseArmorEffects(serverWorld);
            }
        });

        EmeraldMod.LOGGER.info("✓ Registered Horse Armor Effects Handler");
    }

    private static void applyHorseArmorEffects(ServerWorld world) {
        // Set untuk track pemain yang sedang naik kuda emerald di tick ini
        Set<UUID> ridingThisTick = new HashSet<>();

        // Iterasi semua pemain di world
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getWorld() != world) continue;

            UUID playerUUID = player.getUuid();
            boolean isRidingEmeraldHorse = false;

            // Cek apakah pemain sedang naik kuda
            if (player.hasVehicle() && player.getVehicle() instanceof HorseEntity horse) {
                ItemStack horseArmor = horse.getBodyArmor();

                // Cek apakah kuda pakai emerald armor
                if (!horseArmor.isEmpty() && horseArmor.getItem() == ModItems.EMERALD_HORSE_ARMOR) {
                    isRidingEmeraldHorse = true;
                    ridingThisTick.add(playerUUID);

                    // Apply effects ke kuda
                    applyEffectsToHorse(horse);

                    // Apply effects ke pemain (rider) - INFINITE DURATION
                    applyInfiniteEffectsToRider(player);

                    // Handle swimming mechanics (water & lava)
                    handleHorseSwimming(horse, player);
                    handleHorseLavaSwimming(horse, player);
                }
            }

            // Jika pemain TIDAK naik kuda emerald, hapus effects
            if (!isRidingEmeraldHorse) {
                // Cek apakah pemain sebelumnya naik kuda emerald
                if (CURRENTLY_RIDING_EMERALD_HORSE.contains(playerUUID)) {
                    removeEffectsFromRider(player);
                    EmeraldMod.LOGGER.debug("Removed horse effects from player {} (dismounted)",
                            player.getName().getString());
                } else {
                    // Double check: hapus effects jika masih ada (safety measure)
                    if (hasAnyHorseEffect(player)) {
                        removeEffectsFromRider(player);
                        EmeraldMod.LOGGER.debug("Cleaned up lingering horse effects from player {}",
                                player.getName().getString());
                    }
                }
            }
        }

        // Iterasi semua kuda untuk remove effects jika tidak pakai emerald armor lagi
        world.iterateEntities().forEach(entity -> {
            if (entity instanceof HorseEntity horse) {
                ItemStack armorStack = horse.getBodyArmor();

                // Jika kuda tidak pakai emerald armor, hapus effects
                if (armorStack.isEmpty() || armorStack.getItem() != ModItems.EMERALD_HORSE_ARMOR) {
                    removeEffectsFromHorse(horse);
                }
            }
        });

        // Update tracking set
        CURRENTLY_RIDING_EMERALD_HORSE.clear();
        CURRENTLY_RIDING_EMERALD_HORSE.addAll(ridingThisTick);
    }

    private static void applyEffectsToHorse(HorseEntity horse) {
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
                    false, // ambient - false
                    false, // showParticles - false (tidak ada particles)
                    false  // showIcon - false (TIDAK TAMPIL ICON)
            ));
        }

        // 6. Swimming Horse Effect (visual indicator)
        if (!hasInfiniteEffect(horse, ModEffects.SWIMMING_HORSE_ENTRY)) {
            horse.addStatusEffect(new StatusEffectInstance(
                    ModEffects.SWIMMING_HORSE_ENTRY,
                    StatusEffectInstance.INFINITE,
                    0,
                    false,
                    false,
                    true // Show icon
            ));
        }
    }

    private static void handleHorseSwimming(HorseEntity horse, PlayerEntity rider) {
        // Cek apakah kuda berada di air
        if (!horse.isTouchingWater()) {
            return;
        }

        // Get current velocity
        Vec3d velocity = horse.getVelocity();

        // Get kuda position relative to water surface
        double waterLevel = horse.getFluidHeight(net.minecraft.registry.tag.FluidTags.WATER);

        // Jika kuda terlalu dalam di air, apply upward force untuk mengapung
        if (waterLevel > WATER_SURFACE_THRESHOLD) {
            // Apply upward buoyancy force
            double upwardForce = SWIM_UPWARD_FORCE;

            // Extra force jika kuda sedang tenggelam
            if (velocity.y < 0) {
                upwardForce += Math.abs(velocity.y) * 0.5;
            }

            // Set velocity dengan upward force
            horse.setVelocity(
                    velocity.x * WATER_MOVEMENT_MULTIPLIER,
                    Math.max(velocity.y + upwardForce, 0.0), // Prevent sinking
                    velocity.z * WATER_MOVEMENT_MULTIPLIER
            );

            horse.velocityModified = true;

            // Prevent drowning effect
            horse.setAir(horse.getMaxAir());
        } else {
            // Kuda sudah di permukaan, stabilize position
            if (velocity.y < 0) {
                // Cancel downward movement
                horse.setVelocity(
                        velocity.x * WATER_MOVEMENT_MULTIPLIER,
                        0.05, // Slight upward to stay on surface
                        velocity.z * WATER_MOVEMENT_MULTIPLIER
                );
                horse.velocityModified = true;
            }
        }
    }

    private static void handleHorseLavaSwimming(HorseEntity horse, PlayerEntity rider) {
        // Cek apakah kuda berada di lava
        if (!horse.isInLava()) {
            return;
        }

        // Get current velocity
        Vec3d velocity = horse.getVelocity();

        // Get kuda position relative to lava surface
        double lavaLevel = horse.getFluidHeight(net.minecraft.registry.tag.FluidTags.LAVA);

        // Jika kuda terlalu dalam di lava, apply upward force untuk mengapung
        if (lavaLevel > LAVA_SURFACE_THRESHOLD) {
            // Apply upward buoyancy force
            double upwardForce = LAVA_SWIM_UPWARD_FORCE;

            // Extra force jika kuda sedang tenggelam di lava
            if (velocity.y < 0) {
                upwardForce += Math.abs(velocity.y) * 0.6; // Stronger force in lava
            }

            // Set velocity dengan upward force
            horse.setVelocity(
                    velocity.x * LAVA_MOVEMENT_MULTIPLIER,
                    Math.max(velocity.y + upwardForce, 0.0), // Prevent sinking
                    velocity.z * LAVA_MOVEMENT_MULTIPLIER
            );

            horse.velocityModified = true;

            // Extinguish fire jika kuda terbakar
            if (horse.isOnFire()) {
                horse.setFireTicks(0);
            }
        } else {
            // Kuda sudah di permukaan lava, stabilize position
            if (velocity.y < 0) {
                // Cancel downward movement
                horse.setVelocity(
                        velocity.x * LAVA_MOVEMENT_MULTIPLIER,
                        0.08, // Slight upward to stay on surface (higher than water)
                        velocity.z * LAVA_MOVEMENT_MULTIPLIER
                );
                horse.velocityModified = true;
            }

            // Extinguish fire di permukaan juga
            if (horse.isOnFire()) {
                horse.setFireTicks(0);
            }
        }

        // Fire Resistance sudah di-apply di applyEffectsToHorse dengan hidden icon
    }

    private static void applyInfiniteEffectsToRider(PlayerEntity player) {
        // INFINITE DURATION untuk semua effects - sama seperti armor
        // FIRE RESISTANCE DIHAPUS - HANYA UNTUK KUDA

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

        // 6. Swimming Horse Effect (visual indicator untuk rider juga)
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
    }

    private static void removeEffectsFromRider(PlayerEntity player) {
        // FORCE REMOVE semua effects dari pemain ketika turun dari kuda
        // FIRE RESISTANCE TIDAK PERLU DIHAPUS karena tidak pernah diberikan

        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            player.removeStatusEffect(StatusEffects.SPEED);
            EmeraldMod.LOGGER.debug("Removed SPEED from {}", player.getName().getString());
        }

        if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
            player.removeStatusEffect(StatusEffects.REGENERATION);
            EmeraldMod.LOGGER.debug("Removed REGENERATION from {}", player.getName().getString());
        }

        if (player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            player.removeStatusEffect(StatusEffects.JUMP_BOOST);
            EmeraldMod.LOGGER.debug("Removed JUMP_BOOST from {}", player.getName().getString());
        }

        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            player.removeStatusEffect(StatusEffects.RESISTANCE);
            EmeraldMod.LOGGER.debug("Removed RESISTANCE from {}", player.getName().getString());
        }

        if (player.hasStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY)) {
            player.removeStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY);
            EmeraldMod.LOGGER.debug("Removed SWIMMING_HORSE from {}", player.getName().getString());
        }

        if (player.hasStatusEffect(ModEffects.HORSE_FIRE_ENTRY)) {
            player.removeStatusEffect(ModEffects.HORSE_FIRE_ENTRY);
            EmeraldMod.LOGGER.debug("Removed HORSE_FIRE from {}", player.getName().getString());
        }

        if (player.hasStatusEffect(ModEffects.HORSE_LAVA_ENTRY)) {
            player.removeStatusEffect(ModEffects.HORSE_LAVA_ENTRY);
            EmeraldMod.LOGGER.debug("Removed HORSE_LAVA from {}", player.getName().getString());
        }

        if (player.hasStatusEffect(ModEffects.HORSE_SNOW_ENTRY)) {
            player.removeStatusEffect(ModEffects.HORSE_SNOW_ENTRY);
            EmeraldMod.LOGGER.debug("Removed HORSE_SNOW from {}", player.getName().getString());
        }
    }

    private static boolean hasAnyHorseEffect(PlayerEntity player) {
        // Cek apakah pemain punya salah satu horse effect
        // FIRE RESISTANCE DIHAPUS dari check karena tidak diberikan ke player
        return player.hasStatusEffect(StatusEffects.SPEED) ||
                player.hasStatusEffect(StatusEffects.REGENERATION) ||
                player.hasStatusEffect(StatusEffects.JUMP_BOOST) ||
                player.hasStatusEffect(StatusEffects.RESISTANCE) ||
                player.hasStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY) ||
                player.hasStatusEffect(ModEffects.HORSE_FIRE_ENTRY) ||
                player.hasStatusEffect(ModEffects.HORSE_LAVA_ENTRY) ||
        player.hasStatusEffect(ModEffects.HORSE_SNOW_ENTRY);
    }

    private static void removeEffectsFromHorse(HorseEntity horse) {
        // Hapus speed modifier jika ada
        EntityAttributeInstance speedAttribute = horse.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttribute != null && speedAttribute.hasModifier(SPEED_MODIFIER_ID)) {
            speedAttribute.removeModifier(SPEED_MODIFIER_ID);
        }

        // Hapus status effects dari kuda - TERMASUK FIRE RESISTANCE
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
            EmeraldMod.LOGGER.debug("Removed FIRE_RESISTANCE from horse (armor removed)");
        }
        if (horse.hasStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY)) {
            horse.removeStatusEffect(ModEffects.SWIMMING_HORSE_ENTRY);
        }
        if (horse.hasStatusEffect(ModEffects.HORSE_FIRE_ENTRY)) {
            horse.removeStatusEffect(ModEffects.HORSE_FIRE_ENTRY);
        }
        if (horse.hasStatusEffect(ModEffects.HORSE_LAVA_ENTRY)) {
            horse.removeStatusEffect(ModEffects.HORSE_FIRE_ENTRY);
        }
        if (horse.hasStatusEffect(ModEffects.HORSE_SNOW_ENTRY)) {
            horse.removeStatusEffect(ModEffects.HORSE_FIRE_ENTRY);
        }
    }

    /**
     * Cek apakah entity sudah memiliki effect dengan duration INFINITE
     * Untuk mencegah blinking icon
     */
    private static boolean hasInfiniteEffect(net.minecraft.entity.LivingEntity entity, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
        StatusEffectInstance instance = entity.getStatusEffect(effect);
        if (instance == null) {
            return false;
        }
        // Cek apakah duration adalah INFINITE
        return instance.isDurationBelow(0) || instance.getDuration() == StatusEffectInstance.INFINITE;
    }

    // Method untuk cleanup saat server shutdown atau world unload
    public static void cleanup() {
        CURRENTLY_RIDING_EMERALD_HORSE.clear();
    }
}