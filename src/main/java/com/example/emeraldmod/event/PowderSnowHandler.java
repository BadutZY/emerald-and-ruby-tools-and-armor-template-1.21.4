package com.example.emeraldmod.event;

import com.example.emeraldmod.item.EmeraldArmorItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PowderSnowHandler {

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                handlePowderSnowWalking(player);
            }
        });
    }

    private static void handlePowderSnowWalking(ServerPlayerEntity player) {
        // Cek apakah player pakai emerald boots
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (boots.getItem() instanceof EmeraldArmorItem) {
            // Cek apakah player berada di powder snow
            BlockPos pos = player.getBlockPos();
            if (player.getWorld().getBlockState(pos).getBlock() == Blocks.POWDER_SNOW) {
                // Implementasi: kurangi velocity downward di powder snow
                if (player.getVelocity().y < 0) {
                    player.setVelocity(player.getVelocity().x, -0.05, player.getVelocity().z);
                }
            }
        }
    }
}