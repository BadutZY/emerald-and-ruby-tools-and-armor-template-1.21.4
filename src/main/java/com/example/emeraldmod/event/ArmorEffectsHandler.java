package com.example.emeraldmod.event;

import com.example.emeraldmod.effect.ModEffects;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class ArmorEffectsHandler {

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                applyArmorEffects(player);
            }
        });
    }

    private static void applyArmorEffects(PlayerEntity player) {
        // Cek helmet
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.getItem() == ModItems.EMERALD_HELMET) {
            // Water Breathing dengan durasi infinite
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
            // Hapus Water Breathing jika helmet dilepas
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
            // Hapus Dolphin's Grace jika chestplate dilepas
            if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
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
            // Hapus Snow Powder Walker jika boots dilepas
            if (player.hasStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY)) {
                player.removeStatusEffect(ModEffects.SNOW_POWDER_WALKER_ENTRY);
            }
        }

        // Fire Resistance untuk semua armor emerald - INFINITE DURATION
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

            // Matikan api jika sedang terbakar
            if (player.isOnFire()) {
                player.setFireTicks(0);
            }
        } else {
            // Hapus Fire Resistance jika tidak ada armor emerald sama sekali
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
     * Cek apakah player sudah memiliki effect dengan duration INFINITE
     * Untuk mencegah blinking icon
     */
    private static boolean hasInfiniteEffect(PlayerEntity player, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
        StatusEffectInstance instance = player.getStatusEffect(effect);
        if (instance == null) {
            return false;
        }
        // Cek apakah duration adalah INFINITE
        return instance.isDurationBelow(0) || instance.getDuration() == StatusEffectInstance.INFINITE;
    }
}