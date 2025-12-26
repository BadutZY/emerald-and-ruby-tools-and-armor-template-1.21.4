package com.example.emeraldmod.event;

import com.example.emeraldmod.effect.ModEffects;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;

public class ToolEffectsHandler {

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            EffectStateManager stateManager = EffectStateManager.getServerState(server);

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Check if tools effect is enabled untuk player ini
                if (stateManager.isToolsEnabled(player.getUuid())) {
                    applyToolEffects(player);
                } else {
                    // Hapus tool effects jika disabled
                    removeToolEffects(player);
                }
            }
        });
    }

    private static void applyToolEffects(PlayerEntity player) {
        // Cek item yang dipegang di main hand dan offhand
        ItemStack mainHandItem = player.getMainHandStack();
        ItemStack offHandItem = player.getOffHandStack();

        // Set untuk track effect apa saja yang harus aktif
        Set<net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>> activeEffects = new HashSet<>();

        // Check main hand
        checkAndAddToolEffect(mainHandItem, activeEffects);

        // Check offhand
        checkAndAddToolEffect(offHandItem, activeEffects);

        // Apply effects yang seharusnya aktif
        for (var effect : activeEffects) {
            if (!hasInfiniteEffect(player, effect)) {
                player.addStatusEffect(new StatusEffectInstance(
                        effect,
                        StatusEffectInstance.INFINITE,
                        0,
                        false,
                        false,
                        true
                ));
            }
        }

        // Remove effects yang tidak seharusnya aktif
        removeInactiveToolEffects(player, activeEffects);
    }

    private static void checkAndAddToolEffect(ItemStack itemStack, Set<net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>> activeEffects) {
        if (itemStack.isEmpty()) {
            return;
        }

        // Emerald Sword - Shockwave Effect
        if (itemStack.getItem() == ModItems.EMERALD_SWORD) {
            activeEffects.add(ModEffects.SHOCKWAVE_ENTRY);
        }
        // Emerald Pickaxe - Auto Smelt Effect
        else if (itemStack.getItem() == ModItems.EMERALD_PICKAXE) {
            activeEffects.add(ModEffects.AUTO_SMELT_ENTRY);
        }
        // Emerald Axe - Tree Chopping Effect
        else if (itemStack.getItem() == ModItems.EMERALD_AXE) {
            activeEffects.add(ModEffects.TREE_CHOPPING_ENTRY);
        }
        // Emerald Shovel - Anti-Gravity Effect
        else if (itemStack.getItem() == ModItems.EMERALD_SHOVEL) {
            activeEffects.add(ModEffects.ANTI_GRAVITY_ENTRY);
        }
        // Emerald Hoe - Auto Replant Effect
        else if (itemStack.getItem() == ModItems.EMERALD_HOE) {
            activeEffects.add(ModEffects.AUTO_REPLANT_ENTRY);
        }
    }

    private static void removeInactiveToolEffects(PlayerEntity player, Set<net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>> activeEffects) {
        // Daftar semua tool effects
        var allToolEffects = new net.minecraft.registry.entry.RegistryEntry[]{
                ModEffects.SHOCKWAVE_ENTRY,
                ModEffects.AUTO_SMELT_ENTRY,
                ModEffects.TREE_CHOPPING_ENTRY,
                ModEffects.ANTI_GRAVITY_ENTRY,
                ModEffects.AUTO_REPLANT_ENTRY
        };

        // Hapus effect yang tidak aktif
        for (var effect : allToolEffects) {
            if (!activeEffects.contains(effect) && player.hasStatusEffect(effect)) {
                player.removeStatusEffect(effect);
            }
        }
    }

    private static void removeToolEffects(PlayerEntity player) {
        // Hapus semua tool effects
        player.removeStatusEffect(ModEffects.SHOCKWAVE_ENTRY);
        player.removeStatusEffect(ModEffects.AUTO_SMELT_ENTRY);
        player.removeStatusEffect(ModEffects.TREE_CHOPPING_ENTRY);
        player.removeStatusEffect(ModEffects.ANTI_GRAVITY_ENTRY);
        player.removeStatusEffect(ModEffects.AUTO_REPLANT_ENTRY);
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