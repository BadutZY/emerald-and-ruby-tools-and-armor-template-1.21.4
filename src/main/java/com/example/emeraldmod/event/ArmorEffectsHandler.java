package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.effect.ModEffects;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArmorEffectsHandler {

    private static final Map<UUID, Boolean> previousArmorState = new HashMap<>();
    private static final Map<UUID, Boolean> previousNegativeImmunityState = new HashMap<>();

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            EffectStateManager stateManager = EffectStateManager.getServerState(server);

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID playerUuid = player.getUuid();
                boolean currentState = stateManager.isArmorEnabled(playerUuid);
                Boolean previousState = previousArmorState.get(playerUuid);

                if (currentState) {
                    // Check apakah player punya Ruby Armor
                    boolean hasRubyArmor = hasAnyRubyArmor(player);
                    Boolean previousNegativeImmunity = previousNegativeImmunityState.get(playerUuid);

                    // ⭐ IMPROVED: Hapus negative effects SEBELUM apply armor effects
                    // Ini memastikan negative effects langsung hilang saat armor effect dinyalakan
                    if (hasRubyArmor && (previousNegativeImmunity == null || !previousNegativeImmunity)) {
                        removeAllNegativeEffects(player);
                        EmeraldMod.LOGGER.info("Cleared all negative effects from player {} (Negative Immunity activated)",
                                player.getName().getString());
                    }

                    // Apply armor effects (termasuk Negative Immunity)
                    applyArmorEffects(player);

                    // ⭐ EXTRA SAFETY: Selalu hapus negative effects setiap tick jika punya Ruby Armor dan armor effect ON
                    if (hasRubyArmor) {
                        removeAllNegativeEffects(player);
                    }

                    // Update state
                    previousNegativeImmunityState.put(playerUuid, hasRubyArmor);

                    if (previousState != null && !previousState) {
                        EmeraldMod.LOGGER.info("Armor effects ENABLED for player: {}",
                                player.getName().getString());
                    }
                } else {
                    if (previousState == null || previousState) {
                        removeArmorEffects(player);
                    } else {
                        removeArmorEffectsSilent(player);
                    }

                    // Reset negative immunity state ketika armor disabled
                    previousNegativeImmunityState.put(playerUuid, false);
                }

                previousArmorState.put(playerUuid, currentState);
            }
        });

        EmeraldMod.LOGGER.info("✓ Registered Armor Effects Handler (Emerald + Ruby Armor - Toggleable)");
        EmeraldMod.LOGGER.info("  - Ruby Armor: Negative Effect Immunity (Auto-clear every tick)");
    }

    private static void applyArmorEffects(PlayerEntity player) {
        // ===== HELMET: Water Breathing (Emerald OR Ruby) =====
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.getItem() == ModItems.EMERALD_HELMET || helmet.getItem() == ModItems.RUBY_HELMET) {
            if (!hasInfiniteEffect(player, StatusEffects.WATER_BREATHING)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WATER_BREATHING,
                        StatusEffectInstance.INFINITE, 0, false, false, true
                ));
            }
        } else {
            if (player.hasStatusEffect(StatusEffects.WATER_BREATHING)) {
                player.removeStatusEffect(StatusEffects.WATER_BREATHING);
            }
        }

        // ===== CHESTPLATE: Dolphin's Grace (Emerald OR Ruby) =====
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestplate.getItem() == ModItems.EMERALD_CHESTPLATE || chestplate.getItem() == ModItems.RUBY_CHESTPLATE) {
            if (!hasInfiniteEffect(player, StatusEffects.DOLPHINS_GRACE)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.DOLPHINS_GRACE,
                        StatusEffectInstance.INFINITE, 0, false, false, true
                ));
            }
        } else {
            if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
            }
        }

        // ===== LEGGINGS: Silent Step (Emerald OR Ruby) =====
        ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
        if (leggings.getItem() == ModItems.EMERALD_LEGGINGS || leggings.getItem() == ModItems.RUBY_LEGGINGS) {
            if (!hasInfiniteEffect(player, ModEffects.SILENT_STEP_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.SILENT_STEP_ENTRY,
                        StatusEffectInstance.INFINITE, 0, false, false, true
                ));
            }
        } else {
            if (player.hasStatusEffect(ModEffects.SILENT_STEP_ENTRY)) {
                player.removeStatusEffect(ModEffects.SILENT_STEP_ENTRY);
            }
        }

        // ===== BOOTS: Snow Powder Walker (Emerald OR Ruby) =====
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (boots.getItem() == ModItems.EMERALD_BOOTS || boots.getItem() == ModItems.RUBY_BOOTS) {
            if (!hasInfiniteEffect(player, ModEffects.SNOW_POWDER_WALKER_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.SNOW_POWDER_WALKER_ENTRY,
                        StatusEffectInstance.INFINITE, 0, false, false, true
                ));
            }
        } else {
            if (player.hasStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY)) {
                player.removeStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY);
            }
        }

        // ===== FIRE RESISTANCE: Any Emerald OR Ruby Armor =====
        if (hasAnyModArmor(player)) {
            if (!hasInfiniteEffect(player, StatusEffects.FIRE_RESISTANCE)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.FIRE_RESISTANCE,
                        StatusEffectInstance.INFINITE, 0, false, false, true
                ));
            }

            if (player.isOnFire()) {
                player.setFireTicks(0);
            }
        } else {
            if (player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                player.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
            }
        }

        // ===== NEGATIVE IMMUNITY: Any Ruby Armor =====
        if (hasAnyRubyArmor(player)) {
            // Apply Negative Immunity effect
            if (!hasInfiniteEffect(player, ModEffects.NEGATIVE_IMMUNITY_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.NEGATIVE_IMMUNITY_ENTRY,
                        StatusEffectInstance.INFINITE, 0, false, false, true
                ));
            }
        } else {
            // Remove Negative Immunity jika tidak ada Ruby armor
            if (player.hasStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY)) {
                player.removeStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY);
            }
        }
    }

    /**
     * ⭐ IMPROVED METHOD: Remove semua negative effects dari player
     * Hanya menghapus HARMFUL effects, membiarkan BENEFICIAL effects tetap ada
     */
    private static void removeAllNegativeEffects(PlayerEntity player) {
        // Collect list of negative effects untuk dihapus
        java.util.List<net.minecraft.registry.entry.RegistryEntry<StatusEffect>> effectsToRemove =
                new java.util.ArrayList<>();

        // Scan semua active effects
        for (StatusEffectInstance activeEffect : player.getStatusEffects()) {
            StatusEffect statusEffect = activeEffect.getEffectType().value();

            // Hanya collect HARMFUL effects
            if (statusEffect.getCategory() == StatusEffectCategory.HARMFUL) {
                effectsToRemove.add(activeEffect.getEffectType());
            }
        }

        // Remove semua negative effects yang sudah di-collect
        for (net.minecraft.registry.entry.RegistryEntry<StatusEffect> effectToRemove : effectsToRemove) {
            player.removeStatusEffect(effectToRemove);
        }
    }

    /**
     * Check if player is wearing ANY Ruby armor piece
     */
    private static boolean hasAnyRubyArmor(PlayerEntity player) {
        for (ItemStack armorStack : player.getArmorItems()) {
            if (armorStack.getItem() == ModItems.RUBY_HELMET ||
                    armorStack.getItem() == ModItems.RUBY_CHESTPLATE ||
                    armorStack.getItem() == ModItems.RUBY_LEGGINGS ||
                    armorStack.getItem() == ModItems.RUBY_BOOTS) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyModArmor(PlayerEntity player) {
        for (ItemStack armorStack : player.getArmorItems()) {
            // Check Emerald Armor
            if (armorStack.getItem() == ModItems.EMERALD_HELMET ||
                    armorStack.getItem() == ModItems.EMERALD_CHESTPLATE ||
                    armorStack.getItem() == ModItems.EMERALD_LEGGINGS ||
                    armorStack.getItem() == ModItems.EMERALD_BOOTS) {
                return true;
            }
            // Check Ruby Armor
            if (armorStack.getItem() == ModItems.RUBY_HELMET ||
                    armorStack.getItem() == ModItems.RUBY_CHESTPLATE ||
                    armorStack.getItem() == ModItems.RUBY_LEGGINGS ||
                    armorStack.getItem() == ModItems.RUBY_BOOTS) {
                return true;
            }
        }
        return false;
    }

    private static void removeArmorEffects(PlayerEntity player) {
        // Remove ability effects
        if (player.hasStatusEffect(StatusEffects.WATER_BREATHING)) {
            player.removeStatusEffect(StatusEffects.WATER_BREATHING);
        }
        if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        }
        if (player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            player.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
        }
        if (player.hasStatusEffect(ModEffects.SILENT_STEP_ENTRY)) {
            player.removeStatusEffect(ModEffects.SILENT_STEP_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY)) {
            player.removeStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.INFINITE_DURABILITY_ENTRY)) {
            player.removeStatusEffect(ModEffects.INFINITE_DURABILITY_ENTRY);
        }

        // Remove Negative Immunity
        if (player.hasStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY)) {
            player.removeStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY);
        }

        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }

    private static void removeArmorEffectsSilent(PlayerEntity player) {
        // Remove ability effects
        if (player.hasStatusEffect(StatusEffects.WATER_BREATHING)) {
            player.removeStatusEffect(StatusEffects.WATER_BREATHING);
        }
        if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        }
        if (player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            player.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
        }
        if (player.hasStatusEffect(ModEffects.SILENT_STEP_ENTRY)) {
            player.removeStatusEffect(ModEffects.SILENT_STEP_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY)) {
            player.removeStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY);
        }

        // Remove Negative Immunity
        if (player.hasStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY)) {
            player.removeStatusEffect(ModEffects.NEGATIVE_IMMUNITY_ENTRY);
        }

        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }

    private static boolean hasInfiniteEffect(PlayerEntity player, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
        StatusEffectInstance instance = player.getStatusEffect(effect);
        if (instance == null) return false;
        return instance.isDurationBelow(0) || instance.getDuration() == StatusEffectInstance.INFINITE;
    }

    public static void clearPlayerState(UUID playerUuid) {
        previousArmorState.remove(playerUuid);
        previousNegativeImmunityState.remove(playerUuid);
    }
}