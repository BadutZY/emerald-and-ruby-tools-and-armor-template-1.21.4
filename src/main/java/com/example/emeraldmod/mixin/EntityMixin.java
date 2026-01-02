package com.example.emeraldmod.mixin;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.item.RubyArmorItem;
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
 * Prevent powder snow slowdown for Emerald and Ruby boots
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "slowMovement", at = @At("HEAD"), cancellable = true)
    private void preventPowderSnowSlowdown(BlockState state, Vec3d multiplier, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        if (state.getBlock() != Blocks.POWDER_SNOW) {
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
            // Effect ON: Cancel slowdown
            ci.cancel();
        }
        // Effect OFF: Allow vanilla slowdown
    }
}