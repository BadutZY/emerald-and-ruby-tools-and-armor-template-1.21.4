package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Handler untuk powder snow walking dengan toggle
 * Effect ON: Berjalan normal di atas powder snow
 * Effect OFF: Tenggelam seperti vanilla (SINK INTO)
 */
public class PowderSnowHandler {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            EffectStateManager stateManager = EffectStateManager.getServerState(server);

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);

                // Only process if wearing emerald boots
                if (!(boots.getItem() instanceof EmeraldArmorItem)) {
                    continue;
                }

                // Check if armor effect enabled
                boolean armorEnabled = stateManager.isArmorEnabled(player.getUuid());

                if (armorEnabled) {
                    // Help player walk normally on powder snow (effect ON)
                    handlePowderSnowWalking(player);
                } else {
                    // Force sinking in powder snow (effect OFF)
                    handlePowderSnowSinking(player);
                }
            }
        });

        EmeraldMod.LOGGER.info("✅ Registered Powder Snow Handler (Toggleable)");
    }

    /**
     * Membantu player berjalan NORMAL di atas powder snow (effect ON)
     */
    private static void handlePowderSnowWalking(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        BlockPos belowPos = pos.down();

        BlockState currentBlock = player.getWorld().getBlockState(pos);
        BlockState belowBlock = player.getWorld().getBlockState(belowPos);

        boolean inPowderSnow = currentBlock.getBlock() == Blocks.POWDER_SNOW;
        boolean abovePowderSnow = belowBlock.getBlock() == Blocks.POWDER_SNOW;

        if (inPowderSnow || abovePowderSnow) {
            // Remove powder snow state completely
            player.setInPowderSnow(false);

            // Reset freezing completely
            player.setFrozenTicks(0);

            // Only prevent downward velocity if player is sinking
            Vec3d velocity = player.getVelocity();
            if (velocity.y < -0.08 && inPowderSnow) {
                player.setVelocity(velocity.x, 0.0, velocity.z);
                player.velocityModified = true;
            }
        }
    }

    /**
     * FIXED: Memaksa player tenggelam SEPENUHNYA di powder snow (effect OFF)
     */
    private static void handlePowderSnowSinking(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        BlockPos belowPos = pos.down();

        BlockState currentBlock = player.getWorld().getBlockState(pos);
        BlockState belowBlock = player.getWorld().getBlockState(belowPos);

        boolean inPowderSnow = currentBlock.getBlock() == Blocks.POWDER_SNOW;
        boolean abovePowderSnow = belowBlock.getBlock() == Blocks.POWDER_SNOW;

        if (inPowderSnow || abovePowderSnow) {
            // CRITICAL: Enable powder snow state
            player.setInPowderSnow(true);

            Vec3d velocity = player.getVelocity();

            // AGGRESSIVE SINKING: Strong downward force
            double targetY = -0.08; // Base sink speed

            // If player tries to jump, counter it strongly
            if (velocity.y > -0.05) {
                targetY = -0.15;
            }

            // Apply strong horizontal dampening and downward force
            double newX = velocity.x * 0.25; // Very slow horizontal
            double newY = Math.min(velocity.y - 0.05, targetY); // Force down
            double newZ = velocity.z * 0.25; // Very slow horizontal

            player.setVelocity(newX, newY, newZ);
            player.velocityModified = true;

            // Force remove onGround status
            if (player.isOnGround() && (inPowderSnow || abovePowderSnow)) {
                player.setOnGround(false);
            }

            // Additional velocity dampening
            player.fallDistance = 0.0f; // Prevent fall damage

            // Debug log
            if (player.age % 20 == 0) {
                EmeraldMod.LOGGER.debug("Sinking {} - Pos: {}, Vel: {}, InSnow: {}, OnGround: {}",
                        player.getName().getString(),
                        String.format("%.2f", player.getY()),
                        String.format("%.3f", velocity.y),
                        player.inPowderSnow,
                        player.isOnGround());
            }
        }
    }
}