package com.example.emeraldmod.mixin;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.state.EffectStateManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent slow movement in powder snow when effect is ON
 * When effect is OFF, allow vanilla slowdown
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "slowMovement", at = @At("HEAD"), cancellable = true)
    private void preventPowderSnowSlowdown(BlockState state, Vec3d multiplier, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        // Only handle server-side player entities
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // Check if the slow movement is caused by powder snow
        if (state.getBlock() != Blocks.POWDER_SNOW) {
            return;
        }

        // Check if player is wearing emerald boots
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof EmeraldArmorItem)) {
            return;
        }

        // Check if armor effect is enabled
        EffectStateManager stateManager = EffectStateManager.getServerState(player.getServer());
        boolean armorEnabled = stateManager.isArmorEnabled(player.getUuid());

        // CRITICAL FIX: Only cancel slowdown if effect is ON
        if (armorEnabled) {
            // Effect ON: Cancel slowdown completely
            ci.cancel();

            if (player.age % 40 == 0) { // Log setiap 2 detik
                EmeraldMod.LOGGER.debug("Prevented slowdown for {} (effect ON)",
                        player.getName().getString());
            }
        }
        // Effect OFF: Don't cancel, allow vanilla slowdown to occur
        else {
            if (player.age % 40 == 0) { // Log setiap 2 detik
                EmeraldMod.LOGGER.debug("Allowing slowdown for {} (effect OFF)",
                        player.getName().getString());
            }
        }
    }
}