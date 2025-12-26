package com.example.emeraldmod.mixin;

import com.example.emeraldmod.effect.ModEffects;
import net.minecraft.block.TripwireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin untuk TripwireBlock - prevent activation dari silent players
 */
@Mixin(TripwireBlock.class)
public abstract class TripwireHookBlockMixin {

    /**
     * Cancel onEntityCollision untuk silent players
     */
    @Inject(
            method = "onEntityCollision",
            at = @At("HEAD"),
            cancellable = true
    )
    private void emeraldmod$cancelForSilentPlayers(
            net.minecraft.block.BlockState state,
            World world,
            net.minecraft.util.math.BlockPos pos,
            Entity entity,
            CallbackInfo ci) {

        // Jika entity adalah player dengan Silent Step, cancel collision
        if (entity instanceof PlayerEntity player) {
            if (player.hasStatusEffect(ModEffects.SILENT_STEP_ENTRY)) {
                ci.cancel();
            }
        }
    }
}