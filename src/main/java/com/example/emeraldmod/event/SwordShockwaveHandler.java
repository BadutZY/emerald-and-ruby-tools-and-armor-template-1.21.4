package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SwordShockwaveHandler {

    // Counter untuk setiap player (UUID -> hit count)
    private static final Map<UUID, Integer> HIT_COUNTER = new HashMap<>();

    // Konfigurasi shockwave (hit ke-3)
    private static final int HITS_FOR_SHOCKWAVE = 3;
    private static final double SHOCKWAVE_RADIUS = 5.0;
    private static final float SHOCKWAVE_BASE_DAMAGE = 8.0f;
    private static final double KNOCKBACK_STRENGTH = 1.0;
    private static final double SHOCKWAVE_KNOCKBACK = 3.5;
    private static final float DIRECT_HIT_MULTIPLIER = 3.0f;

    // Konfigurasi lightning (hit ke-5, Ruby Sword only)
    private static final int HITS_FOR_LIGHTNING = 5;
    private static final float LIGHTNING_DIRECT_DAMAGE = 15.0f; // 7.5 hearts
    private static final double LIGHTNING_RADIUS = 3.0;
    private static final float LIGHTNING_AOE_DAMAGE = 6.0f; // 3 hearts

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack weapon = player.getStackInHand(hand);
            boolean isEmeraldSword = weapon.getItem() == ModItems.EMERALD_SWORD;
            boolean isRubySword = weapon.getItem() == ModItems.RUBY_SWORD;

            // Check if using Emerald or Ruby Sword
            if (!isEmeraldSword && !isRubySword) {
                return ActionResult.PASS;
            }

            if (!(entity instanceof LivingEntity target)) {
                return ActionResult.PASS;
            }

            // Check tools effect enabled
            if (!world.isClient) {
                ServerWorld serverWorld = (ServerWorld) world;
                EffectStateManager stateManager = EffectStateManager.getServerState(serverWorld.getServer());

                if (!stateManager.isToolsEnabled(player.getUuid())) {
                    HIT_COUNTER.remove(player.getUuid());
                    return ActionResult.PASS;
                }
            }

            if (!world.isClient) {
                handleSwordHit(player, world, target, weapon, isRubySword);
            }

            return ActionResult.PASS;
        });

        EmeraldMod.LOGGER.info("✓ Registered Shockwave Handler (Toggleable)");
        EmeraldMod.LOGGER.info("  - Emerald Sword: Shockwave at 3rd hit");
        EmeraldMod.LOGGER.info("  - Ruby Sword: Shockwave at 3rd hit + Lightning at 5th hit");
    }

    private static void handleSwordHit(PlayerEntity player, World world, LivingEntity target,
                                       ItemStack sword, boolean isRubySword) {
        UUID playerUUID = player.getUuid();
        int currentHits = HIT_COUNTER.getOrDefault(playerUUID, 0) + 1;

        // Apply normal knockback
        applyKnockback(target, player, KNOCKBACK_STRENGTH);

        float baseDamage = getBaseSwordDamage(sword);
        ServerWorld serverWorld = world instanceof ServerWorld ? (ServerWorld) world : null;

        EmeraldMod.LOGGER.info("Player {} - Current hits: {} (Ruby: {})",
                player.getName().getString(), currentHits, isRubySword);

        // ============================================
        // HIT 5: LIGHTNING STRIKE (RUBY SWORD ONLY)
        // ============================================
        if (isRubySword && currentHits == HITS_FOR_LIGHTNING) {
            HIT_COUNTER.put(playerUUID, 0); // Reset counter

            if (serverWorld != null) {
                // Apply massive direct damage
                DamageSource damageSource = world.getDamageSources().playerAttack(player);
                target.damage(serverWorld, damageSource, LIGHTNING_DIRECT_DAMAGE);

                // Trigger lightning strike
                triggerLightningStrike(player, serverWorld, target);

                EmeraldMod.LOGGER.info("⚡ Player {} triggered Lightning Strike (5th hit) ⚡",
                        player.getName().getString());
            }
        }
        // ============================================
        // HIT 3: SHOCKWAVE (BOTH SWORDS)
        // ============================================
        else if (currentHits == HITS_FOR_SHOCKWAVE) {
            // Untuk Emerald Sword, reset setelah shockwave
            // Untuk Ruby Sword, lanjut ke hit 4 dan 5
            if (!isRubySword) {
                HIT_COUNTER.put(playerUUID, 0); // Reset counter untuk Emerald Sword
            } else {
                HIT_COUNTER.put(playerUUID, currentHits); // Lanjut untuk Ruby Sword
            }

            // Apply bonus damage to direct target
            float bonusDamage = baseDamage * (DIRECT_HIT_MULTIPLIER - 1.0f);

            if (serverWorld != null) {
                DamageSource damageSource = world.getDamageSources().playerAttack(player);
                target.damage(serverWorld, damageSource, bonusDamage);

                spawnDirectHitParticles(serverWorld, target.getPos());

                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.2f);
            }

            // Trigger shockwave
            triggerShockwave(player, world, target);

            EmeraldMod.LOGGER.info("💥 Player {} triggered Shockwave (3rd hit) 💥",
                    player.getName().getString());
        } else {
            // Normal hit - increment counter
            HIT_COUNTER.put(playerUUID, currentHits);
            EmeraldMod.LOGGER.debug("Player {} normal hit - counter: {}",
                    player.getName().getString(), currentHits);
        }
    }

    /**
     * Lightning Strike (Ruby Sword only - Hit 5)
     */
    private static void triggerLightningStrike(PlayerEntity player, ServerWorld world, LivingEntity target) {
        Vec3d targetPos = target.getPos();

        // Spawn visual lightning bolt
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        lightning.refreshPositionAfterTeleport(targetPos);
        lightning.setCosmetic(false); // Real lightning with effects
        world.spawnEntity(lightning);

        // Spawn pre-strike warning particles
        spawnLightningWarningParticles(world, targetPos);

        // Play thunder sound
        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 2.0f, 1.0f);
        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.5f, 1.0f);

        // AOE lightning damage to nearby entities
        Box searchBox = new Box(targetPos, targetPos).expand(LIGHTNING_RADIUS);
        List<Entity> nearbyEntities = world.getOtherEntities(target, searchBox);

        int affectedCount = 0;
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity livingEntity)) continue;

            double distance = entity.getPos().distanceTo(targetPos);
            if (distance > LIGHTNING_RADIUS) continue;

            // Apply AOE lightning damage
            float distanceFactor = (float) (1.0 - (distance / LIGHTNING_RADIUS));
            float aoeDamage = Math.max(2.0f, LIGHTNING_AOE_DAMAGE * distanceFactor);

            DamageSource damageSource = world.getDamageSources().lightningBolt();
            livingEntity.damage(world, damageSource, aoeDamage);

            // Lightning chain particles
            spawnLightningChainParticles(world, targetPos, livingEntity.getPos());

            affectedCount++;
        }

        // Spawn impact particles
        spawnLightningImpactParticles(world, targetPos);

        EmeraldMod.LOGGER.info("Lightning Strike: {} nearby entities affected", affectedCount);
    }

    /**
     * Shockwave (Both swords - Hit 3)
     */
    private static void triggerShockwave(PlayerEntity player, World world, LivingEntity hitTarget) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        Vec3d playerPos = player.getPos();
        spawnShockwaveParticles(serverWorld, playerPos);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.5f);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.8f, 0.8f);

        Box searchBox = new Box(playerPos, playerPos).expand(SHOCKWAVE_RADIUS);
        List<Entity> nearbyEntities = world.getOtherEntities(player, searchBox);

        int affectedCount = 0;
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity livingEntity)) continue;
            if (livingEntity == hitTarget) continue;

            double distance = entity.getPos().distanceTo(playerPos);
            if (distance > SHOCKWAVE_RADIUS) continue;

            float distanceFactor = (float) (1.0 - (distance / SHOCKWAVE_RADIUS));
            float aoeDamage = Math.max(1.0f, SHOCKWAVE_BASE_DAMAGE * distanceFactor);

            DamageSource damageSource = world.getDamageSources().playerAttack(player);
            livingEntity.damage(serverWorld, damageSource, aoeDamage);

            applyRadialKnockback(livingEntity, playerPos, SHOCKWAVE_KNOCKBACK * distanceFactor);
            spawnHitParticles(serverWorld, livingEntity.getPos());

            affectedCount++;
        }

        spawnGroundImpactParticles(serverWorld, playerPos);
    }

    private static float getBaseSwordDamage(ItemStack sword) {
        // Ruby Sword: 9 damage, Emerald Sword: 8 damage
        return sword.getItem() == ModItems.RUBY_SWORD ? 9.0f : 8.0f;
    }

    private static void applyKnockback(LivingEntity target, PlayerEntity attacker, double strength) {
        Vec3d direction = target.getPos().subtract(attacker.getPos()).normalize();
        Vec3d knockbackVelocity = direction.multiply(strength, 0.5, strength);
        target.setVelocity(target.getVelocity().add(knockbackVelocity));
        target.velocityModified = true;
    }

    private static void applyRadialKnockback(LivingEntity target, Vec3d center, double strength) {
        Vec3d direction = target.getPos().subtract(center).normalize();
        Vec3d knockbackVelocity = direction.multiply(strength, 0.8, strength).add(0, 0.5, 0);
        target.setVelocity(target.getVelocity().add(knockbackVelocity));
        target.velocityModified = true;
    }

    // ============================================
    // LIGHTNING PARTICLES
    // ============================================

    private static void spawnLightningWarningParticles(ServerWorld world, Vec3d pos) {
        // Warning particles sebelum lightning strike
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y + 5, pos.z,
                30, 0.5, 2.0, 0.5, 0.1);

        world.spawnParticles(ParticleTypes.END_ROD,
                pos.x, pos.y + 10, pos.z,
                50, 0.3, 5.0, 0.3, 0.2);
    }

    private static void spawnLightningImpactParticles(ServerWorld world, Vec3d pos) {
        // Impact particles saat lightning strike
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0);

        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y + 1, pos.z,
                100, 1.0, 1.0, 1.0, 0.3);

        world.spawnParticles(ParticleTypes.FLASH,
                pos.x, pos.y + 1, pos.z,
                5, 0.5, 0.5, 0.5, 0);

        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                pos.x, pos.y, pos.z,
                30, 1.5, 0.1, 1.5, 0.1);
    }

    private static void spawnLightningChainParticles(ServerWorld world, Vec3d from, Vec3d to) {
        // Chain lightning particles antara target
        Vec3d direction = to.subtract(from);
        int steps = 10;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3d pos = from.add(direction.multiply(t));

            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y + 1, pos.z,
                    2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    // ============================================
    // SHOCKWAVE PARTICLES
    // ============================================

    private static void spawnShockwaveParticles(ServerWorld world, Vec3d center) {
        int particleCount = 50;
        double radius = SHOCKWAVE_RADIUS;

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y;

            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0.1, 0, 0.0);
            world.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0.0);
        }

        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0.0);
    }

    private static void spawnGroundImpactParticles(ServerWorld world, Vec3d center) {
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 30,
                SHOCKWAVE_RADIUS * 0.3, 0.1, SHOCKWAVE_RADIUS * 0.3, 0.1);
        world.spawnParticles(ParticleTypes.POOF, center.x, center.y, center.z, 20,
                SHOCKWAVE_RADIUS * 0.2, 0.1, SHOCKWAVE_RADIUS * 0.2, 0.05);
    }

    private static void spawnHitParticles(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y + 1, pos.z, 5, 0.3, 0.5, 0.3, 0.1);
        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y + 1, pos.z, 3, 0.2, 0.3, 0.2, 0.1);
    }

    private static void spawnDirectHitParticles(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y + 1, pos.z, 15, 0.3, 0.5, 0.3, 0.3);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y + 1, pos.z, 10, 0.3, 0.5, 0.3, 0.2);
        world.spawnParticles(ParticleTypes.FIREWORK, pos.x, pos.y + 1, pos.z, 8, 0.2, 0.3, 0.2, 0.15);
    }

    public static void resetCounter(UUID playerUUID) {
        HIT_COUNTER.remove(playerUUID);
    }

    public static int getHitCount(UUID playerUUID) {
        return HIT_COUNTER.getOrDefault(playerUUID, 0);
    }
}