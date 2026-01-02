package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.item.RubyArmorItem;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PowderSnowHandler {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            EffectStateManager stateManager = EffectStateManager.getServerState(server);

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);

                // ✅ CHECK: Emerald Boots OR Ruby Boots
                if (!(boots.getItem() instanceof EmeraldArmorItem) &&
                        !(boots.getItem() instanceof RubyArmorItem)) {
                    continue;
                }

                boolean armorEnabled = stateManager.isArmorEnabled(player.getUuid());

                if (armorEnabled) {
                    handlePowderSnowWalking(player);
                } else {
                    handlePowderSnowSinking(player);
                }
            }
        });

        EmeraldMod.LOGGER.info("✓ Registered Powder Snow Handler (Emerald + Ruby Boots - Toggleable)");
    }

    private static void handlePowderSnowWalking(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        BlockPos belowPos = pos.down();

        BlockState currentBlock = player.getWorld().getBlockState(pos);
        BlockState belowBlock = player.getWorld().getBlockState(belowPos);

        boolean inPowderSnow = currentBlock.getBlock() == Blocks.POWDER_SNOW;
        boolean abovePowderSnow = belowBlock.getBlock() == Blocks.POWDER_SNOW;

        if (inPowderSnow || abovePowderSnow) {
            player.setInPowderSnow(false);
            player.setFrozenTicks(0);

            Vec3d velocity = player.getVelocity();
            if (velocity.y < -0.08 && inPowderSnow) {
                player.setVelocity(velocity.x, 0.0, velocity.z);
                player.velocityModified = true;
            }
        }
    }

    private static void handlePowderSnowSinking(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        BlockPos belowPos = pos.down();

        BlockState currentBlock = player.getWorld().getBlockState(pos);
        BlockState belowBlock = player.getWorld().getBlockState(belowPos);

        boolean inPowderSnow = currentBlock.getBlock() == Blocks.POWDER_SNOW;
        boolean abovePowderSnow = belowBlock.getBlock() == Blocks.POWDER_SNOW;

        if (inPowderSnow || abovePowderSnow) {
            player.setInPowderSnow(true);

            Vec3d velocity = player.getVelocity();
            double targetY = -0.08;

            if (velocity.y > -0.05) {
                targetY = -0.15;
            }

            double newX = velocity.x * 0.25;
            double newY = Math.min(velocity.y - 0.05, targetY);
            double newZ = velocity.z * 0.25;

            player.setVelocity(newX, newY, newZ);
            player.velocityModified = true;

            if (player.isOnGround() && (inPowderSnow || abovePowderSnow)) {
                player.setOnGround(false);
            }

            player.fallDistance = 0.0f;
        }
    }
}