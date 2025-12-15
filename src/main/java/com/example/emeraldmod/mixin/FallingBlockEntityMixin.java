package com.example.emeraldmod.mixin;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.event.AntiGravityHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {

    /**
     * ULTIMATE DEFENSE: Prevent FallingBlockEntity creation entirely
     * This is called when a falling block tries to become an entity
     */
    @Inject(
            method = "spawnFromBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void preventSpawningWhenStabilized(
            World world,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<FallingBlockEntity> cir
    ) {
        if (AntiGravityHandler.isStabilized(pos)) {
            // Return null to prevent entity creation
            cir.setReturnValue(null);
            EmeraldMod.LOGGER.info("BLOCKED FallingBlockEntity spawn at {} (CRITICAL DEFENSE)", pos);
        }
    }
}