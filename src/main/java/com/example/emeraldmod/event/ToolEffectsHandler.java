package com.example.emeraldmod.event;

import com.example.emeraldmod.effect.ModEffects;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class ToolEffectsHandler {

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                applyToolEffects(player);
            }
        });
    }

    private static void applyToolEffects(PlayerEntity player) {
        // Cek item yang sedang dipegang di main hand
        ItemStack mainHandItem = player.getMainHandStack();

        if (mainHandItem.isEmpty()) {
            // Tidak memegang apa-apa, hapus semua tool effects
            removeAllToolEffects(player);
            return;
        }

        // Emerald Sword - Shockwave Effect
        if (mainHandItem.getItem() == ModItems.EMERALD_SWORD) {
            if (!player.hasStatusEffect(ModEffects.SHOCKWAVE_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.SHOCKWAVE_ENTRY,
                        StatusEffectInstance.INFINITE, // Infinite duration
                        0,
                        false,
                        false,
                        true
                ));
            }
            // Hapus effect tool lain
            removeOtherToolEffects(player, ModEffects.SHOCKWAVE_ENTRY);
        }
        // Emerald Pickaxe - Auto Smelt Effect
        else if (mainHandItem.getItem() == ModItems.EMERALD_PICKAXE) {
            if (!player.hasStatusEffect(ModEffects.AUTO_SMELT_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.AUTO_SMELT_ENTRY,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
            removeOtherToolEffects(player, ModEffects.AUTO_SMELT_ENTRY);
        }
        // Emerald Axe - Tree Chopping Effect
        else if (mainHandItem.getItem() == ModItems.EMERALD_AXE) {
            if (!player.hasStatusEffect(ModEffects.TREE_CHOPPING_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.TREE_CHOPPING_ENTRY,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
            removeOtherToolEffects(player, ModEffects.TREE_CHOPPING_ENTRY);
        }
        // Emerald Shovel - Anti-Gravity Effect
        else if (mainHandItem.getItem() == ModItems.EMERALD_SHOVEL) {
            if (!player.hasStatusEffect(ModEffects.ANTI_GRAVITY_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.ANTI_GRAVITY_ENTRY,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
            removeOtherToolEffects(player, ModEffects.ANTI_GRAVITY_ENTRY);
        }
        // Emerald Hoe - Auto Replant Effect
        else if (mainHandItem.getItem() == ModItems.EMERALD_HOE) {
            if (!player.hasStatusEffect(ModEffects.AUTO_REPLANT_ENTRY)) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModEffects.AUTO_REPLANT_ENTRY,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
            removeOtherToolEffects(player, ModEffects.AUTO_REPLANT_ENTRY);
        }
        // Bukan emerald tool, hapus semua tool effects
        else {
            removeAllToolEffects(player);
        }
    }

    private static void removeOtherToolEffects(PlayerEntity player, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> keepEffect) {
        // Hapus semua tool effects kecuali yang di-keep
        if (player.hasStatusEffect(ModEffects.SHOCKWAVE_ENTRY) && !ModEffects.SHOCKWAVE_ENTRY.equals(keepEffect)) {
            player.removeStatusEffect(ModEffects.SHOCKWAVE_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.AUTO_SMELT_ENTRY) && !ModEffects.AUTO_SMELT_ENTRY.equals(keepEffect)) {
            player.removeStatusEffect(ModEffects.AUTO_SMELT_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.TREE_CHOPPING_ENTRY) && !ModEffects.TREE_CHOPPING_ENTRY.equals(keepEffect)) {
            player.removeStatusEffect(ModEffects.TREE_CHOPPING_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.ANTI_GRAVITY_ENTRY) && !ModEffects.ANTI_GRAVITY_ENTRY.equals(keepEffect)) {
            player.removeStatusEffect(ModEffects.ANTI_GRAVITY_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.AUTO_REPLANT_ENTRY) && !ModEffects.AUTO_REPLANT_ENTRY.equals(keepEffect)) {
            player.removeStatusEffect(ModEffects.AUTO_REPLANT_ENTRY);
        }
    }

    private static void removeAllToolEffects(PlayerEntity player) {
        // Hapus semua tool effects
        if (player.hasStatusEffect(ModEffects.SHOCKWAVE_ENTRY)) {
            player.removeStatusEffect(ModEffects.SHOCKWAVE_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.AUTO_SMELT_ENTRY)) {
            player.removeStatusEffect(ModEffects.AUTO_SMELT_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.TREE_CHOPPING_ENTRY)) {
            player.removeStatusEffect(ModEffects.TREE_CHOPPING_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.ANTI_GRAVITY_ENTRY)) {
            player.removeStatusEffect(ModEffects.ANTI_GRAVITY_ENTRY);
        }
        if (player.hasStatusEffect(ModEffects.AUTO_REPLANT_ENTRY)) {
            player.removeStatusEffect(ModEffects.AUTO_REPLANT_ENTRY);
        }
    }
}