package com.example.emeraldmod.mixin;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.item.RubyArmorItem;
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
 * Prevent freezing when wearing Emerald or Ruby boots with effect ON
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "canFreeze", at = @At("HEAD"), cancellable = true)
    private void preventFreezing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // ✅ CHECK: Emerald Boots OR Ruby Boots
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof EmeraldArmorItem) &&
                !(boots.getItem() instanceof RubyArmorItem)) {
            return;
        }

        // ✅ CHECK: Armor effect enabled
        EffectStateManager stateManager = EffectStateManager.getServerState(player.getServer());
        boolean armorEnabled = stateManager.isArmorEnabled(player.getUuid());

        if (armorEnabled) {
            // Effect ON: Prevent freezing
            cir.setReturnValue(false);
        }
        // Effect OFF: Allow vanilla freezing
    }
}