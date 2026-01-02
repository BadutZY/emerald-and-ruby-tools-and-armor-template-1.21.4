package com.example.emeraldmod.network;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handler untuk packet dari client
 */
public class ServerPacketHandler {

    public static void register() {
        // Register packet receiver untuk toggle effect
        ServerPlayNetworking.registerGlobalReceiver(
                ToggleEffectPacket.ID,
                (packet, context) -> {
                    ServerPlayerEntity player = context.player();

                    // Process di server thread
                    context.server().execute(() -> {
                        EffectStateManager stateManager = EffectStateManager.getServerState(context.server());

                        switch (packet.effectType()) {
                            case TOOLS -> {
                                stateManager.setToolsEnabled(player.getUuid(), packet.enabled());
                                EmeraldMod.LOGGER.info("Player {} toggled tools effect: {}",
                                        player.getName().getString(),
                                        packet.enabled() ? "ON" : "OFF");

                                // Hapus tool effects jika disabled
                                if (!packet.enabled()) {
                                    removeToolEffects(player);
                                }
                            }
                            case ARMOR -> {
                                stateManager.setArmorEnabled(player.getUuid(), packet.enabled());
                                EmeraldMod.LOGGER.info("Player {} toggled armor effect: {}",
                                        player.getName().getString(),
                                        packet.enabled() ? "ON" : "OFF");

                                // ⭐ IMPROVED: Jika enabled dan player punya Ruby Armor, hapus negative effects SEGERA
                                if (packet.enabled() && hasAnyRubyArmor(player)) {
                                    // Hapus negative effects 2x untuk memastikan benar-benar bersih
                                    removeAllNegativeEffects(player);

                                    // Schedule removal lagi setelah 1 tick untuk handle edge cases
                                    context.server().execute(() -> {
                                        removeAllNegativeEffects(player);
                                    });

                                    EmeraldMod.LOGGER.info("Immediately cleared all negative effects from player {} (Armor effect enabled with Ruby Armor)",
                                            player.getName().getString());
                                }

                                // Hapus armor effects jika disabled
                                if (!packet.enabled()) {
                                    removeArmorEffects(player);
                                }
                            }
                        }
                    });
                }
        );

        EmeraldMod.LOGGER.info("✓ Registered Server Packet Handlers");
    }

    /**
     * ⭐ NEW METHOD: Check if player is wearing ANY Ruby armor piece
     */
    private static boolean hasAnyRubyArmor(ServerPlayerEntity player) {
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);

        return helmet.getItem() == ModItems.RUBY_HELMET ||
                chestplate.getItem() == ModItems.RUBY_CHESTPLATE ||
                leggings.getItem() == ModItems.RUBY_LEGGINGS ||
                boots.getItem() == ModItems.RUBY_BOOTS;
    }

    /**
     * ⭐ IMPROVED METHOD: Remove semua negative effects dari player
     * Hanya menghapus HARMFUL effects, membiarkan BENEFICIAL effects
     */
    private static void removeAllNegativeEffects(ServerPlayerEntity player) {
        // Collect list of negative effects untuk dihapus
        java.util.List<net.minecraft.registry.entry.RegistryEntry<StatusEffect>> effectsToRemove =
                new java.util.ArrayList<>();

        // Scan semua active effects
        for (net.minecraft.entity.effect.StatusEffectInstance activeEffect : player.getStatusEffects()) {
            StatusEffect statusEffect = activeEffect.getEffectType().value();

            // Hanya collect HARMFUL effects (negative)
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
     * Remove semua tool effects dari player
     */
    private static void removeToolEffects(ServerPlayerEntity player) {
        // Import ModEffects
        var ModEffects = com.example.emeraldmod.effect.ModEffects.class;

        try {
            var shockwaveField = ModEffects.getField("SHOCKWAVE_ENTRY");
            var autoSmeltField = ModEffects.getField("AUTO_SMELT_ENTRY");
            var treeChoppingField = ModEffects.getField("TREE_CHOPPING_ENTRY");
            var antiGravityField = ModEffects.getField("ANTI_GRAVITY_ENTRY");
            var autoReplantField = ModEffects.getField("AUTO_REPLANT_ENTRY");

            player.removeStatusEffect((net.minecraft.registry.entry.RegistryEntry) shockwaveField.get(null));
            player.removeStatusEffect((net.minecraft.registry.entry.RegistryEntry) autoSmeltField.get(null));
            player.removeStatusEffect((net.minecraft.registry.entry.RegistryEntry) treeChoppingField.get(null));
            player.removeStatusEffect((net.minecraft.registry.entry.RegistryEntry) antiGravityField.get(null));
            player.removeStatusEffect((net.minecraft.registry.entry.RegistryEntry) autoReplantField.get(null));

        } catch (Exception e) {
            EmeraldMod.LOGGER.error("Failed to remove tool effects", e);
        }
    }

    /**
     * Remove semua armor effects dari player (kecuali yang dari horse)
     */
    private static void removeArmorEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.WATER_BREATHING);
        player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.DOLPHINS_GRACE);
        player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.FIRE_RESISTANCE);

        // Import ModEffects untuk custom effects
        var ModEffects = com.example.emeraldmod.effect.ModEffects.class;

        try {
            var silentStepField = ModEffects.getField("SILENT_STEP_ENTRY");
            var snowWalkerField = ModEffects.getField("SNOW_POWDER_WALKER_ENTRY");
            var negativeImmunityField = ModEffects.getField("NEGATIVE_IMMUNITY_ENTRY");

            player.removeStatusEffect((net.minecraft.registry.entry.RegistryEntry) silentStepField.get(null));
            player.removeStatusEffect((net.minecraft.registry.entry.RegistryEntry) snowWalkerField.get(null));
            player.removeStatusEffect((net.minecraft.registry.entry.RegistryEntry) negativeImmunityField.get(null));

        } catch (Exception e) {
            EmeraldMod.LOGGER.error("Failed to remove armor effects", e);
        }
    }
}