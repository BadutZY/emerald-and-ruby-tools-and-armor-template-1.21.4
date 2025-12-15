package com.example.emeraldmod.mixin;

import com.example.emeraldmod.item.EmeraldArmorItem;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void allowEmeraldBootsWalking(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living) {
            ItemStack boots = living.getEquippedStack(EquipmentSlot.FEET);
            if (boots.getItem() instanceof EmeraldArmorItem) {
                cir.setReturnValue(true);
            }
        }
    }
}