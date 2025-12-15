package com.example.emeraldmod.mixin;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.event.AntiGravityHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlock.class)
public class FallingBlockMixin {

    /**
     * PRIMARY DEFENSE: Prevent scheduledTick
     * This is the main method that makes falling blocks fall
     */
    @Inject(
            method = "scheduledTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventFallingWhenStabilized(
            BlockState state,
            ServerWorld world,
            BlockPos pos,
            Random random,
            CallbackInfo ci
    ) {
        if (AntiGravityHandler.isStabilized(pos)) {
            ci.cancel();
            EmeraldMod.LOGGER.debug("BLOCKED scheduledTick at {}", pos);
        }
    }

    /**
     * SECONDARY DEFENSE: Prevent onBlockAdded from scheduling tick
     * This prevents falling blocks from scheduling ticks when they update
     */
    @Inject(
            method = "onBlockAdded",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventSchedulingWhenStabilized(
            BlockState state,
            World world,
            BlockPos pos,
            BlockState oldState,
            boolean notify,
            CallbackInfo ci
    ) {
        if (AntiGravityHandler.isStabilized(pos)) {
            ci.cancel();
            EmeraldMod.LOGGER.debug("BLOCKED onBlockAdded at {}", pos);
        }
    }
}