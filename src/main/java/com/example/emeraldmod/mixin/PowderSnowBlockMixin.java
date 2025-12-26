package com.example.emeraldmod.mixin;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for powder snow walking
 * TRUE = can walk on top, FALSE = will sink
 * FIXED: Now properly checks armor effect state
 */
@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void allowEmeraldBootsWalking(Entity entity, CallbackInfoReturnable<Boolean> cir) {

        // Handle players with toggle check
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            ItemStack boots = serverPlayer.getEquippedStack(EquipmentSlot.FEET);

            if (boots.getItem() instanceof EmeraldArmorItem) {
                EffectStateManager stateManager = EffectStateManager.getServerState(serverPlayer.getServer());
                boolean armorEnabled = stateManager.isArmorEnabled(serverPlayer.getUuid());

                // CRITICAL: Return value determines walking ability
                if (armorEnabled) {
                    // Effect ON: Allow walking on top
                    EmeraldMod.LOGGER.debug("Player {} CAN WALK on powder snow (effect ON)",
                            serverPlayer.getName().getString());
                    cir.setReturnValue(true);
                } else {
                    // Effect OFF: MUST SINK (return false)
                    EmeraldMod.LOGGER.debug("Player {} WILL SINK in powder snow (effect OFF)",
                            serverPlayer.getName().getString());
                    cir.setReturnValue(false);
                }
                return;
            }
        }

        // Handle horses (always enabled)
        else if (entity instanceof HorseEntity horse) {
            ItemStack armorStack = horse.getBodyArmor();
            if (!armorStack.isEmpty() && armorStack.getItem() == ModItems.EMERALD_HORSE_ARMOR) {
                cir.setReturnValue(true);
                return;
            }
        }

        // Handle other living entities (no toggle - always walk on top when wearing boots)
        else if (entity instanceof LivingEntity living) {
            ItemStack boots = living.getEquippedStack(EquipmentSlot.FEET);
            if (boots.getItem() instanceof EmeraldArmorItem) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}