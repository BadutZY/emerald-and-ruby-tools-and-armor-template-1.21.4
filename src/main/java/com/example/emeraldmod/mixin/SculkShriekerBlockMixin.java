package com.example.emeraldmod.mixin;

import com.example.emeraldmod.effect.ModEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.CalibratedSculkSensorBlock;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin untuk SculkShriekerBlock - prevent activation dari silent players
 * yang menginjak langsung di atas Shrieker sensor
 */
@Mixin(SculkShriekerBlock.class)
public abstract class SculkShriekerBlockMixin {

    /**
     * Cancel onSteppedOn untuk silent players
     * Ini mencegah aktivasi saat player berdiri/berjalan di atas Sculk Shrieker
     */
    @Inject(
            method = "onSteppedOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void emeraldmod$cancelForSilentPlayers(
            World world,
            BlockPos pos,
            BlockState state,
            Entity entity,
            CallbackInfo ci) {

        // Jika entity adalah player dengan Silent Step, cancel stepped on
        if (entity instanceof PlayerEntity player) {
            if (player.hasStatusEffect(ModEffects.SILENT_STEP_ENTRY)) {
                ci.cancel();
            }
        }
    }
}