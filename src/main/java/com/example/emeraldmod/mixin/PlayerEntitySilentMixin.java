package com.example.emeraldmod.mixin;

import com.example.emeraldmod.effect.ModEffects;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin untuk PlayerEntity - disable footstep sounds untuk Silent Step effect
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntitySilentMixin {

    /**
     * Cancel playStepSound method untuk player dengan Silent Step
     * Ini akan membuat footstep sounds tidak terdengar sama sekali
     */
    @Inject(
            method = "playStepSound",
            at = @At("HEAD"),
            cancellable = true
    )
    private void emeraldmod$cancelStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Jika player memiliki Silent Step effect, cancel step sound
        if (player.hasStatusEffect(ModEffects.SILENT_STEP_ENTRY)) {
            ci.cancel();
        }
    }
}