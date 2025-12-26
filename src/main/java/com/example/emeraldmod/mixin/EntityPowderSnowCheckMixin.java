package com.example.emeraldmod.mixin;

import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.state.EffectStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Additional mixin untuk memaksa inPowderSnow state yang benar
 */
@Mixin(Entity.class)
public abstract class EntityPowderSnowCheckMixin {

    @Shadow
    public boolean inPowderSnow;

    /**
     * Force check powder snow state setiap tick
     * Inject ke method tick() untuk ensure state yang benar
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void emeraldmod$enforcePowderSnowState(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        // Only handle server-side players
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // Only process if wearing emerald boots
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof EmeraldArmorItem)) {
            return;
        }

        // Check armor effect state
        EffectStateManager stateManager = EffectStateManager.getServerState(player.getServer());
        boolean armorEnabled = stateManager.isArmorEnabled(player.getUuid());

        // If in powder snow and effect is OFF, ensure inPowderSnow is true
        if (!armorEnabled && this.inPowderSnow) {
            // Effect OFF + in powder snow = ensure state stays true
            // This prevents any code from resetting it
            player.setInPowderSnow(true);
        }
    }
}