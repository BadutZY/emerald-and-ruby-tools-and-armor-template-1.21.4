package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SwordShockwaveHandler {

    // Counter untuk setiap player (UUID -> hit count)
    private static final Map<UUID, Integer> HIT_COUNTER = new HashMap<>();

    // Konfigurasi shockwave
    private static final int HITS_FOR_SHOCKWAVE = 3; // Setiap 3 hit
    private static final double SHOCKWAVE_RADIUS = 5.0; // Radius efek shockwave
    private static final float SHOCKWAVE_BASE_DAMAGE = 8.0f; // Base damage untuk shockwave AoE
    private static final double KNOCKBACK_STRENGTH = 1.0; // Kekuatan knockback per hit
    private static final double SHOCKWAVE_KNOCKBACK = 3.5; // Knockback dari shockwave

    // DAMAGE MULTIPLIER untuk direct hit (target yang di-hit ke-3)
    private static final float DIRECT_HIT_MULTIPLIER = 3.0f; // 3x damage untuk target langsung

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Cek apakah player menggunakan Emerald Sword
            ItemStack weapon = player.getStackInHand(hand);
            if (weapon.getItem() != ModItems.EMERALD_SWORD) {
                return ActionResult.PASS;
            }

            // Cek apakah target adalah living entity
            if (!(entity instanceof LivingEntity target)) {
                return ActionResult.PASS;
            }

            // ✅ CHECK: Apakah tools effect enabled? (Server-side only)
            if (!world.isClient) {
                ServerWorld serverWorld = (ServerWorld) world;
                EffectStateManager stateManager = EffectStateManager.getServerState(serverWorld.getServer());

                if (!stateManager.isToolsEnabled(player.getUuid())) {
                    // Tools effect DISABLED, reset counter dan biarkan normal attack
                    HIT_COUNTER.remove(player.getUuid());
                    EmeraldMod.LOGGER.debug("Shockwave disabled for player {}", player.getName().getString());
                    return ActionResult.PASS;
                }
            }

            // Process di server side
            if (!world.isClient) {
                handleSwordHit(player, world, target, weapon);
            }

            return ActionResult.PASS; // Allow normal attack
        });

        EmeraldMod.LOGGER.info("✓ Registered Shockwave Handler for Emerald Sword (with 3x Direct Hit + AoE Damage - Toggleable)");
    }

    private static void handleSwordHit(PlayerEntity player, World world, LivingEntity target, ItemStack sword) {
        UUID playerUUID = player.getUuid();

        // Increment hit counter
        int currentHits = HIT_COUNTER.getOrDefault(playerUUID, 0) + 1;

        // Apply extra knockback pada setiap hit
        applyKnockback(target, player, KNOCKBACK_STRENGTH);

        // Cek apakah sudah 3 hits
        if (currentHits >= HITS_FOR_SHOCKWAVE) {
            // Reset counter
            HIT_COUNTER.put(playerUUID, 0);

            // Calculate bonus damage untuk direct hit (target ke-3)
            float baseDamage = getBaseSwordDamage(sword);
            float bonusDamage = baseDamage * (DIRECT_HIT_MULTIPLIER - 1.0f); // Bonus = 2x base (total 3x)

            // Apply bonus damage ke target yang di-hit langsung (hit ke-3)
            if (world instanceof ServerWorld serverWorld) {
                DamageSource damageSource = world.getDamageSources().playerAttack(player);
                target.damage(serverWorld, damageSource, bonusDamage);

                // Spawn extra hit particles pada target langsung
                spawnDirectHitParticles(serverWorld, target.getPos());

                // Play critical hit sound
                world.playSound(
                        null,
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.2f
                );

                EmeraldMod.LOGGER.debug("Applied {}x damage multiplier to direct target! Base: {}, Bonus: {}, Total: {}",
                        DIRECT_HIT_MULTIPLIER, baseDamage, bonusDamage, baseDamage + bonusDamage);
            }

            // Trigger shockwave (ini akan damage entities di sekitar)
            triggerShockwave(player, world, target);

            EmeraldMod.LOGGER.debug("Player {} triggered shockwave with {}x damage bonus!",
                    player.getName().getString(), DIRECT_HIT_MULTIPLIER);
        } else {
            // Update counter
            HIT_COUNTER.put(playerUUID, currentHits);

            EmeraldMod.LOGGER.debug("Player {} hit count: {}/{}",
                    player.getName().getString(), currentHits, HITS_FOR_SHOCKWAVE);
        }
    }

    private static float getBaseSwordDamage(ItemStack sword) {
        // Emerald Sword base attack damage adalah 8 (3 base + 8 dari tool material)
        return 8.0f; // Base damage Emerald Sword
    }

    private static void triggerShockwave(PlayerEntity player, World world, LivingEntity hitTarget) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        Vec3d playerPos = player.getPos();

        // Create shockwave particles (expanding circle)
        spawnShockwaveParticles(serverWorld, playerPos);

        // Play shockwave sound (gunakan sound yang mirip mace)
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS,
                1.0f,
                1.5f // Pitch lebih tinggi untuk efek yang lebih "sharp"
        );

        // Play additional wind sound untuk efek shockwave
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_TRIDENT_RIPTIDE_1,
                SoundCategory.PLAYERS,
                0.8f,
                0.8f
        );

        // Find all entities dalam radius
        Box searchBox = new Box(playerPos, playerPos).expand(SHOCKWAVE_RADIUS);
        List<Entity> nearbyEntities = world.getOtherEntities(player, searchBox);

        int affectedCount = 0;
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            // Skip target yang sudah di-hit langsung (sudah dapat bonus damage)
            if (livingEntity == hitTarget) {
                EmeraldMod.LOGGER.debug("Skipping direct hit target from shockwave AoE");
                continue;
            }

            // Cek jarak
            double distance = entity.getPos().distanceTo(playerPos);
            if (distance > SHOCKWAVE_RADIUS) {
                continue;
            }

            // Hitung damage berdasarkan jarak (lebih dekat = lebih kuat)
            float distanceFactor = (float) (1.0 - (distance / SHOCKWAVE_RADIUS));
            float aoeDamage = SHOCKWAVE_BASE_DAMAGE * distanceFactor;

            // Minimum damage 1.0 jika masih dalam radius
            if (aoeDamage < 1.0f) {
                aoeDamage = 1.0f;
            }

            // Apply damage
            DamageSource damageSource = world.getDamageSources().playerAttack(player);
            livingEntity.damage(serverWorld, damageSource, aoeDamage);

            // Apply strong knockback (radial dari player)
            applyRadialKnockback(livingEntity, playerPos, SHOCKWAVE_KNOCKBACK * distanceFactor);

            // Spawn hit particles pada entity
            spawnHitParticles(serverWorld, livingEntity.getPos());

            affectedCount++;

            EmeraldMod.LOGGER.debug("Shockwave hit entity at distance {}: {} damage",
                    String.format("%.2f", distance), String.format("%.2f", aoeDamage));
        }

        // Spawn ground impact particles
        spawnGroundImpactParticles(serverWorld, playerPos);

        EmeraldMod.LOGGER.debug("Shockwave affected {} entities (excluding direct hit target)", affectedCount);
    }

    private static void applyKnockback(LivingEntity target, PlayerEntity attacker, double strength) {
        // Hitung arah knockback
        Vec3d attackerPos = attacker.getPos();
        Vec3d targetPos = target.getPos();
        Vec3d direction = targetPos.subtract(attackerPos).normalize();

        // Apply velocity
        Vec3d knockbackVelocity = direction.multiply(strength, 0.5, strength); // Y lebih kecil
        target.setVelocity(target.getVelocity().add(knockbackVelocity));
        target.velocityModified = true;
    }

    private static void applyRadialKnockback(LivingEntity target, Vec3d center, double strength) {
        // Hitung arah dari center ke target (radial)
        Vec3d targetPos = target.getPos();
        Vec3d direction = targetPos.subtract(center).normalize();

        // Apply velocity dengan komponen Y yang lebih besar untuk "launched" effect
        Vec3d knockbackVelocity = direction.multiply(strength, 0.8, strength); // Y lebih besar
        knockbackVelocity = knockbackVelocity.add(0, 0.5, 0); // Extra upward force

        target.setVelocity(target.getVelocity().add(knockbackVelocity));
        target.velocityModified = true;
    }

    private static void spawnShockwaveParticles(ServerWorld world, Vec3d center) {
        // Spawn expanding ring particles
        int particleCount = 50;
        double radius = SHOCKWAVE_RADIUS;

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y;

            // Sweep effect particles (expanding from center)
            world.spawnParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    x, y, z,
                    1, // count
                    0, 0.1, 0, // delta
                    0.0 // speed
            );

            // Explosion particles untuk impact effect
            world.spawnParticles(
                    ParticleTypes.EXPLOSION,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0.0
            );
        }

        // Central explosion particles
        world.spawnParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z,
                1,
                0, 0, 0,
                0.0
        );
    }

    private static void spawnGroundImpactParticles(ServerWorld world, Vec3d center) {
        // Spawn ground crack particles
        world.spawnParticles(
                ParticleTypes.CLOUD,
                center.x, center.y, center.z,
                30, // count
                SHOCKWAVE_RADIUS * 0.3, 0.1, SHOCKWAVE_RADIUS * 0.3, // spread
                0.1 // speed
        );

        // Spawn dust particles
        world.spawnParticles(
                ParticleTypes.POOF,
                center.x, center.y, center.z,
                20,
                SHOCKWAVE_RADIUS * 0.2, 0.1, SHOCKWAVE_RADIUS * 0.2,
                0.05
        );
    }

    private static void spawnHitParticles(ServerWorld world, Vec3d pos) {
        // Spawn crit particles pada entity yang terkena
        world.spawnParticles(
                ParticleTypes.CRIT,
                pos.x, pos.y + 1, pos.z,
                5,
                0.3, 0.5, 0.3,
                0.1
        );

        // Damage indicator particles
        world.spawnParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                pos.x, pos.y + 1, pos.z,
                3,
                0.2, 0.3, 0.2,
                0.1
        );
    }

    private static void spawnDirectHitParticles(ServerWorld world, Vec3d pos) {
        // Spawn enhanced crit particles untuk direct hit
        world.spawnParticles(
                ParticleTypes.CRIT,
                pos.x, pos.y + 1, pos.z,
                15, // Lebih banyak particles
                0.3, 0.5, 0.3,
                0.3 // Speed lebih cepat
        );

        // Enchant glint particles untuk efek "power hit"
        world.spawnParticles(
                ParticleTypes.ENCHANTED_HIT,
                pos.x, pos.y + 1, pos.z,
                10,
                0.3, 0.5, 0.3,
                0.2
        );

        // Firework spark untuk dramatic effect
        world.spawnParticles(
                ParticleTypes.FIREWORK,
                pos.x, pos.y + 1, pos.z,
                8,
                0.2, 0.3, 0.2,
                0.15
        );
    }

    // Method untuk reset counter (opsional, bisa dipanggil saat player logout/death)
    public static void resetCounter(UUID playerUUID) {
        HIT_COUNTER.remove(playerUUID);
    }

    // Method untuk cek current hit count (untuk debugging atau HUD)
    public static int getHitCount(UUID playerUUID) {
        return HIT_COUNTER.getOrDefault(playerUUID, 0);
    }
}