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

        // ===== SWORD =====
        if (itemStack.getItem() == ModItems.EMERALD_SWORD || itemStack.getItem() == ModItems.RUBY_SWORD) {
            // Ability effect (for both)
            activeEffects.add(ModEffects.SHOCKWAVE_ENTRY);

            if (itemStack.getItem() == ModItems.RUBY_SWORD) {
                activeEffects.add(ModEffects.LIGHTING_SLASH_ENTRY);
            }
        }

        // ===== PICKAXE =====
        else if (itemStack.getItem() == ModItems.EMERALD_PICKAXE || itemStack.getItem() == ModItems.RUBY_PICKAXE) {
            // Ability effect (for both)
            activeEffects.add(ModEffects.AUTO_SMELT_ENTRY);

            if (itemStack.getItem() == ModItems.RUBY_PICKAXE) {
                activeEffects.add(ModEffects.VEIN_MINING_ENTRY);
            }
        }

        // ===== AXE =====
        else if (itemStack.getItem() == ModItems.EMERALD_AXE || itemStack.getItem() == ModItems.RUBY_AXE) {
            // Ability effect (for both)
            activeEffects.add(ModEffects.TREE_CHOPPING_ENTRY);

            if (itemStack.getItem() == ModItems.RUBY_AXE) {
                activeEffects.add(ModEffects.AUTO_PLACE_ENTRY);
            }
        }

        // ===== SHOVEL =====
        else if (itemStack.getItem() == ModItems.EMERALD_SHOVEL || itemStack.getItem() == ModItems.RUBY_SHOVEL) {
            // Ability effect (for both)
            activeEffects.add(ModEffects.ANTI_GRAVITY_ENTRY);

            if (itemStack.getItem() == ModItems.RUBY_SHOVEL) {
                activeEffects.add(ModEffects.FAST_DIGGING_ENTRY);
            }
        }

        // ===== HOE =====
        else if (itemStack.getItem() == ModItems.EMERALD_HOE || itemStack.getItem() == ModItems.RUBY_HOE) {
            // Ability effect (for both)
            activeEffects.add(ModEffects.AUTO_REPLANT_ENTRY);

            if (itemStack.getItem() == ModItems.RUBY_HOE) {
                activeEffects.add(ModEffects.MORE_HARVEST_ENTRY);
            }
        }
    }

    private static void removeInactiveToolEffects(PlayerEntity player, Set<net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>> activeEffects) {
        // Daftar semua tool effects (ability + Ruby icons)
        var allToolEffects = new net.minecraft.registry.entry.RegistryEntry[]{
                // Ability effects
                ModEffects.SHOCKWAVE_ENTRY,
                ModEffects.AUTO_SMELT_ENTRY,
                ModEffects.TREE_CHOPPING_ENTRY,
                ModEffects.ANTI_GRAVITY_ENTRY,
                ModEffects.AUTO_REPLANT_ENTRY,
                ModEffects.VEIN_MINING_ENTRY,
                ModEffects.FAST_DIGGING_ENTRY,
                ModEffects.MORE_HARVEST_ENTRY,
                ModEffects.AUTO_PLACE_ENTRY,
                ModEffects.LIGHTING_SLASH_ENTRY
        };

        // Hapus effect yang tidak aktif
        for (var effect : allToolEffects) {
            if (!activeEffects.contains(effect) && player.hasStatusEffect(effect)) {
                player.removeStatusEffect(effect);
            }
        }
    }

    private static void removeToolEffects(PlayerEntity player) {
        // Hapus semua tool effects (ability + Ruby icons)
        player.removeStatusEffect(ModEffects.SHOCKWAVE_ENTRY);
        player.removeStatusEffect(ModEffects.AUTO_SMELT_ENTRY);
        player.removeStatusEffect(ModEffects.TREE_CHOPPING_ENTRY);
        player.removeStatusEffect(ModEffects.ANTI_GRAVITY_ENTRY);
        player.removeStatusEffect(ModEffects.AUTO_REPLANT_ENTRY);
        player.removeStatusEffect(ModEffects.VEIN_MINING_ENTRY);
        player.removeStatusEffect(ModEffects.FAST_DIGGING_ENTRY);
        player.removeStatusEffect(ModEffects.MORE_HARVEST_ENTRY);
        player.removeStatusEffect(ModEffects.AUTO_PLACE_ENTRY);
        player.removeStatusEffect(ModEffects.LIGHTING_SLASH_ENTRY);
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