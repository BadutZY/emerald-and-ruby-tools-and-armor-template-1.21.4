package com.example.emeraldmod.mixin;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.state.EffectStateManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to prevent freezing damage when armor effect is ON
 * When effect is OFF, allow vanilla freezing
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * Prevent freezing ticks from increasing when effect is ON
     * Allow freezing when effect is OFF
     */
    @Inject(method = "canFreeze", at = @At("HEAD"), cancellable = true)
    private void preventFreezing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Only handle server-side players
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // Check if wearing emerald boots
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof EmeraldArmorItem)) {
            return;
        }

        // Check if armor effect is enabled
        EffectStateManager stateManager = EffectStateManager.getServerState(player.getServer());
        boolean armorEnabled = stateManager.isArmorEnabled(player.getUuid());

        if (armorEnabled) {
            // Effect ON: Prevent freezing completely
            cir.setReturnValue(false);

            if (player.age % 40 == 0) {
                EmeraldMod.LOGGER.debug("Prevented freezing for {} (effect ON)",
                        player.getName().getString());
            }
        } else {
            // Effect OFF: Allow vanilla freezing
            // Don't set return value, let vanilla logic handle it
            if (player.age % 40 == 0) {
                EmeraldMod.LOGGER.debug("Allowing freezing for {} (effect OFF)",
                        player.getName().getString());
            }
        }
    }
}