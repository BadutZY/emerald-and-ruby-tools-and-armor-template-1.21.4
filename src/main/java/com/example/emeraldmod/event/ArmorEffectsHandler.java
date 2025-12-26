package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.effect.ModEffects;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArmorEffectsHandler {

    // Track previous state untuk prevent spam logging
    private static final Map<UUID, Boolean> previousArmorState = new HashMap<>();

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            EffectStateManager stateManager = EffectStateManager.getServerState(server);

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID playerUuid = player.getUuid();
                boolean currentState = stateManager.isArmorEnabled(playerUuid);
                Boolean previousState = previousArmorState.get(playerUuid);

                // Check if armor effect is enabled untuk player ini
                if (currentState) {
                    applyArmorEffects(player);

                    // Log hanya saat state berubah dari OFF ke ON
                    if (previousState != null && !previousState) {
                        EmeraldMod.LOGGER.info("Armor effects ENABLED for player: {}",
                                player.getName().getString());
                    }
                } else {
                    // Log hanya saat state berubah dari ON ke OFF
                    if (previousState == null || previousState) {
                        EmeraldMod.LOGGER.info("=== REMOVING ARMOR EFFECTS ===");
                        EmeraldMod.LOGGER.info("Player: {}", player.getName().getString());
                        removeArmorEffects(player);
                        EmeraldMod.LOGGER.info("Successfully removed all armor effects");
                    } else {
                        // State sudah OFF, remove tanpa log
                        removeArmorEffectsSilent(player);
                    }
                }

                // Update previous state
                previousArmorState.put(playerUuid, currentState);
            }
        });

        EmeraldMod.LOGGER.info("✓ Registered Emerald Armor Effects Handler (Toggleable)");
    }

    private static void applyArmorEffects(PlayerEntity player) {
        // Cek helmet
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.getItem() == ModItems.EMERALD_HELMET) {
            if (!hasInfiniteEffect(player, StatusEffects.WATER_BREATHING)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WATER_BREATHING,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
        } else {
            if (player.hasStatusEffect(StatusEffects.WATER_BREATHING)) {
                player.removeStatusEffect(StatusEffects.WATER_BREATHING);
            }
        }

        // Cek chestplate
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestplate.getItem() == ModItems.EMERALD_CHESTPLATE) {
            if (!hasInfiniteEffect(player, StatusEffects.DOLPHINS_GRACE)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.DOLPHINS_GRACE,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
        } else {
            if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
            }
        }

        // Cek leggings - Silent Step Effect
        ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
        if (leggings.getItem() == ModItems.EMERALD_LEGGINGS) {
            if (!hasInfiniteEffect(player, ModEffects.SILENT_STEP_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.SILENT_STEP_ENTRY,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
        } else {
            if (player.hasStatusEffect(ModEffects.SILENT_STEP_ENTRY)) {
                player.removeStatusEffect(ModEffects.SILENT_STEP_ENTRY);
            }
        }

        // Cek boots - Snow Powder Walker Effect
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (boots.getItem() == ModItems.EMERALD_BOOTS) {
            if (!hasInfiniteEffect(player, ModEffects.SNOW_POWDER_WALKER_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.SNOW_POWDER_WALKER_ENTRY,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
        } else {
            if (player.hasStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY)) {
                player.removeStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY);
            }
        }

        // Fire Resistance untuk semua armor emerald
        if (hasAnyEmeraldArmor(player)) {
            if (!hasInfiniteEffect(player, StatusEffects.FIRE_RESISTANCE)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.FIRE_RESISTANCE,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
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
    }

    private static boolean hasAnyEmeraldArmor(PlayerEntity player) {
        for (ItemStack armorStack : player.getArmorItems()) {
            if (armorStack.getItem() == ModItems.EMERALD_HELMET ||
                    armorStack.getItem() == ModItems.EMERALD_CHESTPLATE ||
                    armorStack.getItem() == ModItems.EMERALD_LEGGINGS ||
                    armorStack.getItem() == ModItems.EMERALD_BOOTS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove semua armor effects DENGAN logging (dipanggil pertama kali saja)
     */
    private static void removeArmorEffects(PlayerEntity player) {
        // Remove all status effects
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

        // Reset fire ticks
        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }

    /**
     * Remove semua armor effects TANPA logging (dipanggil setiap tick setelahnya)
     */
    private static void removeArmorEffectsSilent(PlayerEntity player) {
        // Remove effects tanpa log spam
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

        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }

    /**
     * Cek apakah player sudah memiliki effect dengan duration INFINITE
     */
    private static boolean hasInfiniteEffect(PlayerEntity player, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
        StatusEffectInstance instance = player.getStatusEffect(effect);
        if (instance == null) {
            return false;
        }
        return instance.isDurationBelow(0) || instance.getDuration() == StatusEffectInstance.INFINITE;
    }

    /**
     * Clear tracking saat player disconnect
     */
    public static void clearPlayerState(UUID playerUuid) {
        previousArmorState.remove(playerUuid);
    }
}