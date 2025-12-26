package com.example.emeraldmod.mixin;

import com.example.emeraldmod.item.ModItems;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public class HorseEntityPowderSnowMixin {

    /**
     * Inject ke static method canWalkOnPowderSnow untuk membolehkan kuda dengan emerald armor
     * berjalan di atas powder snow
     */
    @Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void allowEmeraldHorseArmorWalkingOnPowderSnow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // Cek apakah entity adalah kuda
        if (entity instanceof HorseEntity horse) {
            ItemStack armorStack = horse.getBodyArmor();

            // Cek apakah kuda menggunakan emerald horse armor
            if (!armorStack.isEmpty() && armorStack.getItem() == ModItems.EMERALD_HORSE_ARMOR) {
                cir.setReturnValue(true);
            }
        }
    }
}